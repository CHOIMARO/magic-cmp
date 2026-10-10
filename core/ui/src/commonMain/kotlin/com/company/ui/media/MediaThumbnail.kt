package com.company.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.company.core.designsystem.component.StripeBox
import com.company.core.domain.model.editor.MediaType

/**
 * Loads a small image of a photo, or one frame of a video, for a thumbnail.
 *
 * @param uri Location of the file.
 * @param type Photo or video.
 * @return The image, or null while it loads or when the app cannot read the file.
 */
@Composable
expect fun rememberMediaThumbnail(uri: String, type: MediaType): ImageBitmap?

/**
 * Thumbnail of a photo or video. It shows the striped placeholder until the image loads.
 *
 * @param uri Location of the file. Null shows only the placeholder.
 * @param type Photo or video.
 * @param hue Hue of the placeholder.
 * @param modifier Modifier for the thumbnail.
 * @param shape Shape of the thumbnail.
 * @param content Content on top of the thumbnail, for example a badge.
 */
@Composable
fun MediaThumbnail(
    uri: String?,
    type: MediaType,
    hue: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {},
) {
    StripeBox(hue = hue.toFloat(), modifier = modifier, shape = shape) {
        val image = uri?.let { rememberMediaThumbnail(it, type) }
        if (image != null) {
            Image(bitmap = image, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
        content()
    }
}

/** Keeps recent thumbnails in memory. Use it only on the main thread. */
internal object MediaThumbnailCache {
    private const val MAX_SIZE = 32
    private val images = LinkedHashMap<String, ImageBitmap>()

    operator fun get(uri: String): ImageBitmap? = images[uri]

    operator fun set(uri: String, image: ImageBitmap) {
        images.remove(uri)
        images[uri] = image
        if (images.size > MAX_SIZE) images.remove(images.keys.first())
    }
}
