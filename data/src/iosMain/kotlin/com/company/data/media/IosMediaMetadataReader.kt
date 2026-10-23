package com.company.data.media

import com.company.domain.model.editor.MediaType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.duration
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.conformsToType

/** Reads photo and video details from a file URL. The system picker copied the file into the app storage. */
internal class IosMediaMetadataReader : MediaMetadataReader {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun read(uri: String): MediaMetadata? = withContext(Dispatchers.IO) {
        val url = NSURL.URLWithString(uri) ?: return@withContext null
        val path = url.path ?: return@withContext null
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return@withContext null
        val name = (url.lastPathComponent ?: DEFAULT_NAME).substringBeforeLast('.')
        val isPhoto = UTType.typeWithFilenameExtension(url.pathExtension.orEmpty())?.conformsToType(UTTypeImage) == true
        if (isPhoto) return@withContext MediaMetadata(name = name, type = MediaType.PHOTO, duration = null)

        val seconds = CMTimeGetSeconds(AVURLAsset(uRL = url, options = null).duration)
        if (seconds.isNaN() || seconds <= 0.0) return@withContext null
        MediaMetadata(name = name, type = MediaType.VIDEO, duration = seconds)
    }

    private companion object {
        const val DEFAULT_NAME = "media"
    }
}
