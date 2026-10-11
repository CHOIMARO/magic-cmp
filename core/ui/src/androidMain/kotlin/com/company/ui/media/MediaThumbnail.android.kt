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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        MediaFrameReader { uri, type, time, maxSize, _ ->
            when (type) {
                // 영상은 항상 그 시간의 프레임을 읽는다. 휴대폰 영상은 키 프레임이 1초쯤마다 있어서, 키 프레임만 쓰면 이웃한 칸이 같은 그림이 된다.
                MediaType.VIDEO -> OpenVideoFrames.read(context, uri, time, maxSize)
                MediaType.PHOTO -> withContext(Dispatchers.IO) { loadPhoto(context, uri, maxSize) }
            }
        }
    }
}

/**
 * Reads video frames on one background thread and keeps the last opened videos open.
 *
 * Opening a file takes most of the time of one frame, so a series of frames from one video loads fast.
 * [MediaMetadataRetriever] is not thread-safe, so all calls use one thread.
 */
private object OpenVideoFrames {
    private const val MAX_OPEN = 2

    @OptIn(ExperimentalCoroutinesApi::class)
    private val thread = Dispatchers.IO.limitedParallelism(1)
    private val retrievers = LinkedHashMap<String, MediaMetadataRetriever>()

    suspend fun read(context: Context, uri: String, time: Double, maxSize: Int): ImageBitmap? = withContext(thread) {
        val retriever = retriever(context, uri) ?: return@withContext null
        runCatching {
            retriever.getScaledFrameAtTime((time * 1_000_000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST, maxSize, maxSize)
                ?.asImageBitmap()
        }.getOrNull()
    }

    private fun retriever(context: Context, uri: String): MediaMetadataRetriever? {
        retrievers.remove(uri)?.let { open ->
            retrievers[uri] = open
            return open
        }
        val retriever = MediaMetadataRetriever()
        val opened = runCatching { retriever.setDataSource(context, Uri.parse(uri)) }.isSuccess
        if (!opened) {
            retriever.release()
            return null
        }
        retrievers[uri] = retriever
        if (retrievers.size > MAX_OPEN) {
            val oldest = retrievers.keys.first()
            retrievers.remove(oldest)?.release()
        }
        return retriever
    }
}
