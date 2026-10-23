package com.company.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.company.core.designsystem.component.StripeBox
import com.company.domain.model.editor.MediaType
import kotlin.math.roundToInt

/** Loads thumbnails. Use it on the main thread. */
fun interface MediaThumbnailLoader {
    /**
     * Loads a small image of a photo, or one frame of a video. Recent images come from a memory cache.
     *
     * @param uri Location of the file.
     * @param type Photo or video.
     * @param time Position of the video frame in seconds. A photo ignores it.
     * @return The image, or null when the app cannot read the file.
     */
    suspend fun load(uri: String, type: MediaType, time: Double): ImageBitmap?
}

/** Returns the thumbnail loader of the platform. Code outside a composable, for example the export, can use it. */
@Composable
expect fun rememberMediaThumbnailLoader(): MediaThumbnailLoader

/**
 * Loads a small image of a photo, or one frame of a video, for a thumbnail.
 *
 * When only [time] changes, the function keeps the last image until the new frame loads.
 *
 * @param uri Location of the file.
 * @param type Photo or video.
 * @param time Position of the video frame in seconds. A photo ignores it.
 * @return The image, or null while it loads or when the app cannot read the file.
 */
@Composable
fun rememberMediaThumbnail(uri: String, type: MediaType, time: Double = 0.0): ImageBitmap? {
    val loader = rememberMediaThumbnailLoader()
    val key = thumbnailKey(uri, thumbnailFrameTime(type, time))
    var image by remember(uri, type) { mutableStateOf(MediaThumbnailCache[key]) }
    LaunchedEffect(key) {
        loader.load(uri, type, time)?.let { image = it }
    }
    return image
}

/**
 * Makes a [MediaThumbnailLoader] that uses [MediaThumbnailCache].
 *
 * @param read Reads the image of a file at a frame time. It runs on the main thread, so it must switch threads for slow work.
 */
internal fun cachedThumbnailLoader(read: suspend (uri: String, type: MediaType, frameTime: Double) -> ImageBitmap?) =
    MediaThumbnailLoader { uri, type, time ->
        val frameTime = thumbnailFrameTime(type, time)
        val key = thumbnailKey(uri, frameTime)
        MediaThumbnailCache[key] ?: read(uri, type, frameTime)?.also { MediaThumbnailCache[key] = it }
    }

/**
 * Thumbnail of a photo or video. It shows the striped placeholder until the image loads.
 *
 * @param uri Location of the file. Null shows only the placeholder.
 * @param type Photo or video.
 * @param hue Hue of the placeholder.
 * @param modifier Modifier for the thumbnail.
 * @param time Position of the video frame in seconds. A photo ignores it.
 * @param shape Shape of the thumbnail.
 * @param colorFilter Optional color filter for the image and the placeholder.
 * @param content Content on top of the thumbnail, for example a badge.
 */
@Composable
fun MediaThumbnail(
    uri: String?,
    type: MediaType,
    hue: Int,
    modifier: Modifier = Modifier,
    time: Double = 0.0,
    shape: Shape = RectangleShape,
    colorFilter: ColorFilter? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    StripeBox(hue = hue.toFloat(), modifier = modifier, shape = shape, colorFilter = colorFilter) {
        val image = uri?.let { rememberMediaThumbnail(it, type, time) }
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
            )
        }
        content()
    }
}

/** Time step of video thumbnails, in seconds. Near times share one frame, so a trim drag loads fewer frames. */
private const val THUMBNAIL_TIME_STEP = 0.5

/**
 * Rounds the frame time to [THUMBNAIL_TIME_STEP].
 *
 * @return The frame time in seconds. It is always 0 for a photo.
 */
internal fun thumbnailFrameTime(type: MediaType, time: Double): Double =
    if (type == MediaType.PHOTO) 0.0 else (time / THUMBNAIL_TIME_STEP).roundToInt() * THUMBNAIL_TIME_STEP

/** Cache key of one thumbnail. */
internal fun thumbnailKey(uri: String, frameTime: Double): String = "$uri#$frameTime"

/** Keeps recent thumbnails in memory. Use it only on the main thread. */
internal val MediaThumbnailCache = ImageCache(maxSize = 32)

/**
 * Keeps the most recently used images in memory. Use it only on the main thread.
 *
 * @param maxSize Number of images to keep. The least recently used image goes out first.
 */
internal class ImageCache(private val maxSize: Int) {
    private val images = LinkedHashMap<String, ImageBitmap>()

    operator fun get(key: String): ImageBitmap? = images.remove(key)?.also { images[key] = it }

    operator fun set(key: String, image: ImageBitmap) {
        images.remove(key)
        images[key] = image
        if (images.size > maxSize) images.remove(images.keys.first())
    }
}
