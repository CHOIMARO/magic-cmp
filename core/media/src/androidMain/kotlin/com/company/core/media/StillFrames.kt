package com.company.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BitmapUtil
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.FilterType
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.colorMatrix
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "StillFrames"

/** Time before the clip end of the last frame of a video clip, in seconds. It is one frame at 30 fps. */
private const val LAST_FRAME_OFFSET = 1.0 / 30

/** Longest time that a frame waits for its still frame, in milliseconds. After that, the frame shows no still. */
private const val STILL_WAIT_MILLIS = 3_000L

/** Largest memory of the still frame cache, in bytes. It holds about six still frames of 1080 × 1920 pixels. */
private const val CACHE_BYTES = 48 * 1024 * 1024

/** Longest side of a decoded photo. Media3 uses a similar limit for its textures. */
private const val PHOTO_MAX_SIDE = 4096

/**
 * Identity of a still bitmap. Two compositions that need the same still share one bitmap.
 *
 * @property uri Location of the source file.
 * @property isVideo True for a video, false for a photo.
 * @property timeUs Time of the frame in the source, in microseconds. It is 0 for a photo.
 * @property frameWidth Width of the output frame in pixels.
 * @property frameHeight Height of the output frame in pixels.
 * @property areaWidth Width of the video area in pixels.
 * @property areaHeight Height of the video area in pixels.
 * @property blurRadius Blur radius in pixels of the video area. 0 means no blur.
 * @property filter Color filter of the video.
 */
internal data class StillKey(
    val uri: String,
    val isVideo: Boolean,
    val timeUs: Long,
    val frameWidth: Int,
    val frameHeight: Int,
    val areaWidth: Int,
    val areaHeight: Int,
    val blurRadius: Float,
    val filter: FilterType,
)

/**
 * Still frames of the clip edges for one composition.
 *
 * A background thread loads the stills, and [StillFrameCache] keeps them. Media3 gives the [Context] only when it
 * creates the effect, so call [attach] before the first frame.
 *
 * @param clips Clips of the composition.
 * @param timeline Transitions of the composition.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @param areaWidth Width of the video area in pixels.
 * @param areaHeight Height of the video area in pixels.
 * @param filter Color filter of the video. The still gets the same filter as the live frame.
 */
internal class StillFrames(
    clips: List<Clip>,
    private val timeline: TransitionTimeline,
    frameWidth: Int,
    frameHeight: Int,
    areaWidth: Int,
    areaHeight: Int,
    filter: FilterType,
) {
    private val keys: Map<StillRef, StillKey> = timeline.stillRefs.mapNotNull { ref ->
        val clip = clips.getOrNull(ref.clipIndex) ?: return@mapNotNull null
        val uri = clip.sourceUri ?: return@mapNotNull null
        val time = when {
            clip.type == MediaType.PHOTO -> 0.0
            ref.edge == StillEdge.FIRST -> clip.inPoint
            else -> max(clip.inPoint, clip.outPoint - LAST_FRAME_OFFSET)
        }
        ref to StillKey(
            uri = uri,
            isVideo = clip.type == MediaType.VIDEO,
            timeUs = (time * 1_000_000).roundToLong(),
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            areaWidth = areaWidth,
            areaHeight = areaHeight,
            blurRadius = timeline.stillBlur(ref) * areaWidth,
            filter = filter,
        )
    }.toMap()

    /** Still frames that failed to load. The composition does not try them again. */
    private val failed: MutableSet<StillRef> = ConcurrentHashMap.newKeySet()

    /** Still frames that took too long. Later frames show them only when they are ready. */
    private val late: MutableSet<StillRef> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var context: Context? = null

    /**
     * Gives the context for the file access.
     *
     * @param context Any context. The class keeps only the application context.
     */
    fun attach(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * Starts to load the stills of the transition at [timeUs] and of the next transition.
     *
     * @param timeUs Time on the timeline in microseconds.
     */
    fun prefetch(timeUs: Long) {
        val context = context ?: return
        val current = timeline.stillsOf(timeline.windowIndexAt(timeUs))
        val next = timeline.stillsOf(timeline.nextWindowIndexAfter(timeUs))
        (current + next).forEach { ref -> keys[ref]?.let { StillFrameCache.request(context, it) } }
    }

    /**
     * Returns the still bitmap of [ref]. It waits for the load up to [STILL_WAIT_MILLIS] the first time.
     *
     * @param ref Still frame to get.
     * @return The bitmap, or null when it is not available.
     */
    fun await(ref: StillRef): Bitmap? {
        val context = context ?: return null
        val key = keys[ref] ?: return null
        if (ref in failed) return null
        val future = StillFrameCache.request(context, key)
        if (ref in late && !future.isDone) return null
        val bitmap = try {
            future.get(STILL_WAIT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            Log.w(TAG, "Still frame of clip ${ref.clipIndex} is late", e)
            late += ref
            return null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return null
        } catch (e: ExecutionException) {
            Log.w(TAG, "Cannot load the still frame of clip ${ref.clipIndex}", e)
            null
        }
        if (bitmap == null) failed += ref
        return bitmap
    }
}

/**
 * Process-wide cache and loader of still frames.
 *
 * One background thread loads the stills one at a time. A request for a still in the cache or in work does not start a
 * new load.
 */
internal object StillFrameCache {
    private val lock = Any()
    private val bitmaps = object : LruCache<StillKey, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: StillKey, value: Bitmap): Int = value.allocationByteCount
    }
    private val loading = HashMap<StillKey, Future<Bitmap?>>()
    private val executor by lazy {
        Executors.newSingleThreadExecutor { task -> Thread(task, "StillFrameLoader").apply { isDaemon = true } }
    }

    /**
     * Returns the still of [key]. It starts a load when the cache does not have the still.
     *
     * @param context Application context for the file access.
     * @param key Still to get.
     * @return A future with the bitmap, or with null when the load failed.
     */
    fun request(context: Context, key: StillKey): Future<Bitmap?> = synchronized(lock) {
        bitmaps.get(key)?.let { return CompletableFuture.completedFuture(it) }
        loading.getOrPut(key) { executor.submit(Callable { load(context, key) }) }
    }

    private fun load(context: Context, key: StillKey): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            bitmap = loadStillFrame(context, key)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot load the still frame of ${key.uri}", e)
        } finally {
            // 실패해도 작업 목록에서 지운다. 다음 컴포지션은 다시 불러올 수 있다.
            synchronized(lock) {
                loading.remove(key)
                bitmap?.let { bitmaps.put(key, it) }
            }
        }
        return bitmap
    }
}

// =====================================================================
// Decode, crop, filter, and blur of one still frame
// =====================================================================

/**
 * Makes the still bitmap of [key]. It looks like the live frame after the item effects of the composition.
 *
 * The bitmap fills the video area: Media3 cuts the source to the frame, and then to the area. The still gets the same
 * cuts, the same color filter, and the blur of [StillKey.blurRadius].
 */
@OptIn(UnstableApi::class)
private fun loadStillFrame(context: Context, key: StillKey): Bitmap {
    val source = if (key.isVideo) decodeVideoFrame(context, key) else decodePhoto(context, key)
    try {
        val (cropWidth, cropHeight) = stillCropSize(
            sourceWidth = source.width,
            sourceHeight = source.height,
            frameAspect = key.frameWidth.toFloat() / key.frameHeight,
            areaAspect = key.areaWidth.toFloat() / key.areaHeight,
        )
        val left = (source.width - cropWidth) / 2f
        val top = (source.height - cropHeight) / 2f
        val size = stillBitmapSize(key.areaWidth, key.areaHeight, key.blurRadius)
        val still = drawScaled(source, RectF(left, top, left + cropWidth, top + cropHeight), size, key.filter)
        if (key.blurRadius > 0f) {
            val pixels = IntArray(still.width * still.height)
            still.getPixels(pixels, 0, still.width, 0, 0, still.width, still.height)
            boxBlur(pixels, still.width, still.height, gaussianBoxRadii(key.blurRadius * still.width / key.areaWidth))
            still.setPixels(pixels, 0, still.width, 0, 0, still.width, still.height)
        }
        return still
    } finally {
        source.recycle()
    }
}

/** Decodes the video frame at [StillKey.timeUs]. The retriever turns the frame to the display rotation. */
private fun decodeVideoFrame(context: Context, key: StillKey): Bitmap =
    MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(context, Uri.parse(key.uri))
        checkNotNull(retriever.getFrameAtTime(key.timeUs, MediaMetadataRetriever.OPTION_CLOSEST)) {
            "No video frame at ${key.timeUs} us"
        }
    }

/** Decodes the photo. Media3 decodes it the same way, so the EXIF rotation is the same. */
@OptIn(UnstableApi::class)
private fun decodePhoto(context: Context, key: StillKey): Bitmap {
    val data = checkNotNull(context.contentResolver.openInputStream(Uri.parse(key.uri))) { "Cannot open ${key.uri}" }
        .use { it.readBytes() }
    return BitmapUtil.decode(data, data.size, null, PHOTO_MAX_SIDE)
}

/**
 * Draws the [region] of [source] into a new bitmap of [size] with the color filter.
 *
 * A large scale-down halves the image in steps. One step of more than half skips pixels and makes jagged edges.
 */
private fun drawScaled(source: Bitmap, region: RectF, size: PixelSize, filter: FilterType): Bitmap {
    val smoothPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    var image = source
    var part = region
    while (part.width() >= size.width * 2 && part.height() >= size.height * 2) {
        val halfWidth = (part.width() / 2).roundToInt()
        val halfHeight = (part.height() / 2).roundToInt()
        val half = Bitmap.createBitmap(halfWidth, halfHeight, Bitmap.Config.ARGB_8888)
        Canvas(half).drawBitmap(image, part.toRect(), Rect(0, 0, half.width, half.height), smoothPaint)
        if (image !== source) image.recycle()
        image = half
        part = RectF(0f, 0f, half.width.toFloat(), half.height.toFloat())
    }
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        filter.colorMatrix()?.let { colorFilter = ColorMatrixColorFilter(ColorMatrix(it)) }
    }
    val output = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    Canvas(output).drawBitmap(image, part.toRect(), Rect(0, 0, size.width, size.height), paint)
    if (image !== source) image.recycle()
    return output
}

/** Rounds the rectangle to whole pixels. */
private fun RectF.toRect(): Rect = Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
