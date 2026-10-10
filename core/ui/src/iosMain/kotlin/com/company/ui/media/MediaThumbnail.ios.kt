package com.company.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.company.core.domain.model.editor.MediaType
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

private const val THUMBNAIL_SIZE = 720.0
private const val JPEG_QUALITY = 0.8
private const val TIMESCALE = 600
private const val FRAME_TIME_TOLERANCE = 0.25

@Composable
actual fun rememberMediaThumbnail(uri: String, type: MediaType, time: Double): ImageBitmap? {
    val frameTime = thumbnailFrameTime(type, time)
    val key = thumbnailKey(uri, frameTime)
    var image by remember(uri, type) { mutableStateOf(MediaThumbnailCache[key]) }
    LaunchedEffect(key) {
        val loaded = MediaThumbnailCache[key] ?: when (type) {
            MediaType.VIDEO -> loadVideoFrame(uri, frameTime)
            MediaType.PHOTO -> withContext(Dispatchers.IO) { loadPhoto(uri) }
        }?.also { MediaThumbnailCache[key] = it }
        if (loaded != null) image = loaded
    }
    return image
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun loadVideoFrame(uri: String, time: Double): ImageBitmap? = suspendCancellableCoroutine { continuation ->
    val url = NSURL.URLWithString(uri)
    if (url == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }
    val generator = AVAssetImageGenerator(asset = AVURLAsset(uRL = url, options = null)).apply {
        appliesPreferredTrackTransform = true
        maximumSize = CGSizeMake(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
        // 썸네일 시간 간격의 절반까지는 가까운 키 프레임을 써서 빨리 읽는다.
        requestedTimeToleranceBefore = CMTimeMakeWithSeconds(FRAME_TIME_TOLERANCE, TIMESCALE)
        requestedTimeToleranceAfter = CMTimeMakeWithSeconds(FRAME_TIME_TOLERANCE, TIMESCALE)
    }
    // CGImage는 콜백 안에서만 유효하므로, 콜백 안에서 JPEG 바이트로 바꾼다.
    generator.generateCGImageAsynchronouslyForTime(CMTimeMakeWithSeconds(time, TIMESCALE)) { image, _, _ ->
        continuation.resume(image?.let { UIImage.imageWithCGImage(it) }?.toImageBitmap())
    }
}

// imageByPreparingThumbnailOfSize는 사진의 회전 정보를 반영한 작은 이미지를 만든다.
@OptIn(ExperimentalForeignApi::class)
private fun loadPhoto(uri: String): ImageBitmap? {
    val path = NSURL.URLWithString(uri)?.path ?: return null
    val photo = UIImage.imageWithContentsOfFile(path) ?: return null
    val (width, height) = photo.size.useContents { width to height }
    val scale = minOf(1.0, THUMBNAIL_SIZE / maxOf(width, height))
    val thumbnail = photo.imageByPreparingThumbnailOfSize(CGSizeMake(width * scale, height * scale)) ?: photo
    return thumbnail.toImageBitmap()
}

private fun UIImage.toImageBitmap(): ImageBitmap? {
    val bytes = UIImageJPEGRepresentation(this, JPEG_QUALITY)?.toByteArray() ?: return null
    return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return result
}
