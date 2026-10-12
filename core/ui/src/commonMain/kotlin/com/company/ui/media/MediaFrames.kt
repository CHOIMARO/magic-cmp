package com.company.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import com.company.core.domain.model.editor.MediaType
import kotlinx.coroutines.flow.conflate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reads frames of videos and images of photos at a given size.
 *
 * It keeps the recently used files open, so a series of frames from one video loads fast.
 * The timeline filmstrip and the trim preview use it.
 */
fun interface MediaFrameReader {
    /**
     * Reads one frame of a video, or the image of a photo.
     *
     * @param uri Location of the file.
     * @param type Photo or video.
     * @param time Position of the video frame in seconds. A photo ignores it.
     * @param maxSize Longest side of the image in pixels.
     * @param exact True to read the frame at [time]. False lets the platform use a near frame that loads faster.
     * @return The image, or null when the app cannot read the file.
     */
    suspend fun read(uri: String, type: MediaType, time: Double, maxSize: Int, exact: Boolean): ImageBitmap?
}

/** Returns the frame reader of the platform. */
@Composable
expect fun rememberMediaFrameReader(): MediaFrameReader

/** Longest side of a filmstrip frame in pixels. */
private const val FILMSTRIP_FRAME_SIZE = 192

/** Time step of filmstrip frames in seconds. Near times share one frame. It is shorter than one frame of a 60 fps video. */
private const val FILMSTRIP_TIME_STEP = 0.01

/** Longest side of an exact frame for the stage in pixels. */
private const val EXACT_FRAME_SIZE = 720

/** Keeps recent filmstrip frames. 200 frames of 192 × 108 pixels use about 17 MB. */
private val FilmstripCache = FrameCache(maxSize = 200)

/**
 * Loads one small frame for the clip filmstrip. Recent frames come from a memory cache.
 *
 * While the frame loads, the function shows the kept frame of the same file that is nearest in time,
 * if it is within [nearestWithin]. Thus a zoomed timeline does not show empty frames for a moment.
 *
 * @param uri Location of the file.
 * @param type Photo or video. All frames of a photo are the same image.
 * @param time Position of the video frame in seconds.
 * @param exact True to read the frame at [time]. Use it when the frames are close together, for example on a zoomed
 *   timeline. False lets the platform use a near frame that loads faster.
 * @param nearestWithin Largest time distance, in seconds, of a kept frame that shows while the frame loads.
 *   0 shows no frame.
 * @return The image, or null while it loads or when the app cannot read the file.
 */
@Composable
fun rememberFilmstripFrame(
    uri: String,
    type: MediaType,
    time: Double,
    exact: Boolean = false,
    nearestWithin: Double = 0.0,
): ImageBitmap? {
    val reader = rememberMediaFrameReader()
    val frameTime = if (type == MediaType.PHOTO) 0.0 else (time / FILMSTRIP_TIME_STEP).roundToInt() * FILMSTRIP_TIME_STEP
    var image by remember(uri, frameTime) {
        mutableStateOf(FilmstripCache[uri, frameTime] ?: FilmstripCache.nearest(uri, frameTime, nearestWithin))
    }
    LaunchedEffect(uri, frameTime) {
        if (FilmstripCache.contains(uri, frameTime)) return@LaunchedEffect
        reader.read(uri, type, frameTime, FILMSTRIP_FRAME_SIZE, exact)?.let {
            FilmstripCache[uri, frameTime] = it
            image = it
        }
    }
    return image
}

/**
 * Keeps the most recently used filmstrip frames, and finds the kept frame of a file that is nearest in time.
 * Use it only on the main thread.
 *
 * @param maxSize Number of frames to keep. The least recently used frame goes out first.
 */
private class FrameCache(private val maxSize: Int) {
    private data class Key(val uri: String, val time: Double)

    private val frames = LinkedHashMap<Key, ImageBitmap>()

    /** Returns the frame at [time], and marks it as recently used. */
    operator fun get(uri: String, time: Double): ImageBitmap? = Key(uri, time).let { key -> frames.remove(key)?.also { frames[key] = it } }

    operator fun set(uri: String, time: Double, image: ImageBitmap) {
        val key = Key(uri, time)
        frames.remove(key)
        frames[key] = image
        if (frames.size > maxSize) frames.remove(frames.keys.first())
    }

    /** Tells if the frame at [time] is kept. It does not change the use order. */
    fun contains(uri: String, time: Double): Boolean = Key(uri, time) in frames

    /**
     * Finds the kept frame of the file that is nearest to [time]. It does not change the use order.
     *
     * @param within Largest time distance in seconds.
     * @return The frame, or null when no kept frame is within [within].
     */
    fun nearest(uri: String, time: Double, within: Double): ImageBitmap? {
        if (within <= 0.0) return null
        return frames.entries
            .filter { it.key.uri == uri && abs(it.key.time - time) <= within }
            .minByOrNull { abs(it.key.time - time) }
            ?.value
    }
}

/**
 * Loads the exact video frame at [time] for a large preview, for example while the user drags a trim handle.
 *
 * When [time] changes fast, it reads one frame at a time and then goes to the newest time.
 * It keeps the last image until the next frame loads.
 *
 * @param uri Location of the video file.
 * @param time Position of the frame in seconds.
 * @return The image, or null until the first frame loads.
 */
@Composable
fun rememberExactVideoFrame(uri: String, time: Double): ImageBitmap? {
    val reader = rememberMediaFrameReader()
    val latestTime by rememberUpdatedState(time)
    var image by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri, reader) {
        snapshotFlow { latestTime }.conflate().collect { frameTime ->
            reader.read(uri, MediaType.VIDEO, frameTime, EXACT_FRAME_SIZE, exact = true)?.let { image = it }
        }
    }
    return image
}
