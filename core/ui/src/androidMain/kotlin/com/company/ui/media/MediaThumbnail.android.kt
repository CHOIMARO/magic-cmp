package com.company.ui.media

import android.content.Context
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.company.core.domain.model.editor.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_SIZE = 720

@Composable
actual fun rememberMediaThumbnailLoader(): MediaThumbnailLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        cachedThumbnailLoader { uri, type, frameTime ->
            withContext(Dispatchers.IO) {
                when (type) {
                    MediaType.VIDEO -> loadVideoFrame(context, uri, frameTime)
                    MediaType.PHOTO -> loadPhoto(context, uri)
                }
            }
        }
    }
}

// OPTION_CLOSEST는 키 프레임이 아닌 위치도 정확히 읽는다. 0초는 키 프레임이라 비용이 같다.
private fun loadVideoFrame(context: Context, uri: String, time: Double): ImageBitmap? = runCatching {
    MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(context, Uri.parse(uri))
        val timeUs = (time * 1_000_000).toLong()
        retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
            ?.asImageBitmap()
    }
}.getOrNull()

// ImageDecoder는 사진의 회전 정보(EXIF)를 반영해서 읽는다. 긴 변이 THUMBNAIL_SIZE를 넘으면 줄여서 읽는다.
private fun loadPhoto(context: Context, uri: String): ImageBitmap? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val longSide = maxOf(info.size.width, info.size.height)
        if (longSide > THUMBNAIL_SIZE) {
            val scale = THUMBNAIL_SIZE.toFloat() / longSide
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    }.asImageBitmap()
}.getOrNull()
