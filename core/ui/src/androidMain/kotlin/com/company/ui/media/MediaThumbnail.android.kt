package com.company.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.inspector.frame.FrameExtractor
import com.company.domain.model.editor.MediaType
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val THUMBNAIL_SIZE = 720

@Composable
actual fun rememberMediaThumbnailLoader(): MediaThumbnailLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        cachedThumbnailLoader { uri, type, frameTime ->
            when (type) {
                // 저장할 때 레이아웃 칸도 이 그림을 쓰므로, 썸네일은 그 시간의 프레임을 정확히 읽는다.
                MediaType.VIDEO -> VideoFrames.read(context, uri, frameTime, THUMBNAIL_SIZE, exact = true)
                MediaType.PHOTO -> withContext(Dispatchers.IO) { loadPhoto(context, uri) }
            }
        }
    }
}

// ImageDecoder는 사진의 회전 정보(EXIF)를 반영해서 읽는다. 긴 변이 maxSize를 넘으면 줄여서 읽는다.
private fun loadPhoto(context: Context, uri: String, maxSize: Int = THUMBNAIL_SIZE): ImageBitmap? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val longSide = maxOf(info.size.width, info.size.height)
        if (longSide > maxSize) {
            val scale = maxSize.toFloat() / longSide
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    }.asImageBitmap()
}.getOrNull()

@Composable
actual fun rememberMediaFrameReader(): MediaFrameReader {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        MediaFrameReader { uri, type, time, maxSize, exact ->
            when (type) {
                MediaType.VIDEO -> VideoFrames.read(context, uri, time, maxSize, exact)
                MediaType.PHOTO -> withContext(Dispatchers.IO) { loadPhoto(context, uri, maxSize) }
            }
        }
    }
}

/**
 * Reads video frames with the Media3 [FrameExtractor], and keeps the extractors of the last used videos.
 *
 * All extractors share one Media3 player. The player decodes with the hardware decoder and makes the frame small
 * on the GPU, so the CPU stays free for the screen. The player keeps its decoder between frames of one video.
 * All calls run on the main thread, because the extractors must stay on one thread.
 */
@OptIn(UnstableApi::class)
private object VideoFrames {
    /** Number of extractors to keep. */
    private const val MAX_OPEN = 4

    /** Time without a read, in milliseconds, before the extractors close. Then the player gives back its decoder. */
    private const val IDLE_CLOSE_MILLIS = 10_000L

    /** Largest time distance, in microseconds, of a key frame that a read can use when it does not need the exact frame. */
    private const val FRAME_TIME_TOLERANCE_US = 250_000L

    private val extractors = LinkedHashMap<String, FrameExtractor>()
    private val handler = Handler(Looper.getMainLooper())
    private val closeIdle = Runnable { closeAll() }
    private var reading = 0

    /** True after the hardware decoder failed on a frame that the software decoder could read. */
    private var useSoftwareDecoder = false

    /**
     * Reads one frame of a video.
     *
     * @param context Context of the app.
     * @param uri Location of the video file.
     * @param time Position of the frame in seconds.
     * @param maxSize Longest side of the image in pixels.
     * @param exact True to read the frame at [time]. False lets the player use a key frame near [time].
     * @return The image, or null when the app cannot read the frame.
     */
    suspend fun read(context: Context, uri: String, time: Double, maxSize: Int, exact: Boolean): ImageBitmap? =
        withContext(Dispatchers.Main.immediate) {
            handler.removeCallbacks(closeIdle)
            reading++
            try {
                readFrame(context, uri, (time * 1000).roundToLong(), maxSize, exact)?.asImageBitmap()
            } finally {
                reading--
                if (reading == 0) handler.postDelayed(closeIdle, IDLE_CLOSE_MILLIS)
            }
        }

    private suspend fun readFrame(context: Context, uri: String, positionMs: Long, maxSize: Int, exact: Boolean): Bitmap? {
        extract(extractor(context, uri, maxSize, exact, useSoftwareDecoder), positionMs)?.let { return it }
        if (useSoftwareDecoder) return null
        // Media3는 일부 하드웨어 디코더가 효과를 쓰면서 위치를 옮기면 실패한다고 알린다. 그때는 소프트웨어 디코더로 다시 읽는다.
        val frame = extract(extractor(context, uri, maxSize, exact, software = true), positionMs) ?: return null
        // 소프트웨어 디코더로는 읽혔으므로 파일이 아니라 하드웨어 디코더의 문제다. 다음 프레임부터 소프트웨어 디코더만 쓴다.
        useSoftwareDecoder = true
        return frame
    }

    /** Returns the frame image, or null when the extraction fails. A cancelled coroutine stops with its exception. */
    private suspend fun extract(extractor: FrameExtractor, positionMs: Long): Bitmap? = try {
        extractor.getFrame(positionMs).await().bitmap
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        null
    }

    private fun extractor(context: Context, uri: String, maxSize: Int, exact: Boolean, software: Boolean): FrameExtractor {
        val key = "$uri#$maxSize#$exact#$software"
        extractors.remove(key)?.let { open ->
            extractors[key] = open
            return open
        }
        val extractor = FrameExtractor.Builder(context, MediaItem.fromUri(uri))
            .setEffects(listOf(FitInside(maxSize)))
            .setSeekParameters(if (exact) SeekParameters.EXACT else SeekParameters(FRAME_TIME_TOLERANCE_US, FRAME_TIME_TOLERANCE_US))
            .setMediaCodecSelector(if (software) MediaCodecSelector.PREFER_SOFTWARE else MediaCodecSelector.DEFAULT)
            .build()
        extractors[key] = extractor
        if (extractors.size > MAX_OPEN) extractors.remove(extractors.keys.first())?.close()
        return extractor
    }

    private fun closeAll() {
        extractors.values.forEach { it.close() }
        extractors.clear()
    }
}

/**
 * Makes the frame smaller, so that its longest side is not more than [maxSize] pixels. It keeps the aspect ratio.
 *
 * It samples from smaller copies of a large frame (mipmaps), so a small frame does not get jagged edges.
 */
@OptIn(UnstableApi::class)
private data class FitInside(val maxSize: Int) : MatrixTransformation {
    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        val scale = minOf(1f, maxSize.toFloat() / maxOf(inputWidth, inputHeight))
        return Size((inputWidth * scale).roundToInt().coerceAtLeast(1), (inputHeight * scale).roundToInt().coerceAtLeast(1))
    }

    // 출력 크기만 바꾸고 행렬은 그대로 두면, 프레임 전체가 줄어든 출력에 맞춰 그려진다.
    override fun getMatrix(presentationTimeUs: Long): Matrix = Matrix()

    override fun getGlTextureMinFilter(): Int = C.TEXTURE_MIN_FILTER_LINEAR_MIPMAP_LINEAR
}

/** Waits for the result of the future, but does not stop the thread. When the coroutine stops, it cancels the future. */
private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addListener({ continuation.resumeWith(runCatching { Futures.getDone(this) }) }, MoreExecutors.directExecutor())
    continuation.invokeOnCancellation { cancel(false) }
}
