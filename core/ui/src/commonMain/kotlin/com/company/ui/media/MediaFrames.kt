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

/** Time step of filmstrip frames in seconds. Near times share one frame. */
private const val FILMSTRIP_TIME_STEP = 0.05

/** Longest side of an exact frame for the stage in pixels. */
private const val EXACT_FRAME_SIZE = 720

/** Keeps recent filmstrip frames. 160 frames of 192 × 108 pixels use about 13 MB. */
private val FilmstripCache = ImageCache(maxSize = 160)

/**
 * Loads one small frame for the clip filmstrip. Recent frames come from a memory cache.
 *
 * @param uri Location of the file.
 * @param type Photo or video. All frames of a photo are the same image.
 * @param time Position of the video frame in seconds.
 * @return The image, or null while it loads or when the app cannot read the file.
 */
@Composable
fun rememberFilmstripFrame(uri: String, type: MediaType, time: Double): ImageBitmap? {
    val reader = rememberMediaFrameReader()
    val frameTime = if (type == MediaType.PHOTO) 0.0 else (time / FILMSTRIP_TIME_STEP).roundToInt() * FILMSTRIP_TIME_STEP
    val key = thumbnailKey(uri, frameTime)
    var image by remember(key) { mutableStateOf(FilmstripCache[key]) }
    LaunchedEffect(key) {
        if (image != null) return@LaunchedEffect
        reader.read(uri, type, frameTime, FILMSTRIP_FRAME_SIZE, exact = false)?.let {
            FilmstripCache[key] = it
            image = it
        }
    }
    return image
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
