package com.company.core.data.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.company.core.domain.model.editor.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads photo and video details from a content URI that the system picker gave.
 *
 * @param context Context to open the content URI.
 */
internal class AndroidMediaMetadataReader(private val context: Context) : MediaMetadataReader {

    override suspend fun read(uri: String): MediaMetadata? = withContext(Dispatchers.IO) {
        val contentUri = Uri.parse(uri)
        runCatching {
            val name = (displayName(contentUri) ?: contentUri.lastPathSegment ?: DEFAULT_NAME).substringBeforeLast('.')
            val mimeType = context.contentResolver.getType(contentUri).orEmpty()
            if (mimeType.startsWith(IMAGE_MIME_PREFIX)) {
                if (canDecodeImage(contentUri)) MediaMetadata(name = name, type = MediaType.PHOTO, duration = null) else null
            } else {
                videoDuration(contentUri)?.let { MediaMetadata(name = name, type = MediaType.VIDEO, duration = it) }
            }
        }.getOrNull()
    }

    private fun videoDuration(uri: Uri): Double? = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { it / 1000.0 }
    }

    // 픽셀은 읽지 않고 크기만 읽어서, 앱이 열 수 있는 사진인지 확인한다.
    private fun canDecodeImage(uri: Uri): Boolean =
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            options.outWidth > 0 && options.outHeight > 0
        } ?: false

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private companion object {
        const val DEFAULT_NAME = "media"
        const val IMAGE_MIME_PREFIX = "image/"
    }
}
