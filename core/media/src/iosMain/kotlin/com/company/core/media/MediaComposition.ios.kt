package com.company.core.media

import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.ExportOverlay
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.FrameArea
import com.company.core.domain.model.editor.MediaType
import com.company.core.domain.model.editor.colorMatrix
import kotlin.coroutines.resume
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterInputPixelBufferAdaptor
import platform.AVFoundation.AVAssetWriterStatusCompleted
import platform.AVFoundation.AVAsynchronousCIImageFilteringRequest
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVMutableCompositionTrack
import platform.AVFoundation.AVMutableVideoComposition
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeH264
import platform.AVFoundation.AVVideoHeightKey
import platform.AVFoundation.AVVideoWidthKey
import platform.AVFoundation.addMutableTrackWithMediaType
import platform.AVFoundation.loadTracksWithMediaType
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.removeTrack
import platform.AVFoundation.timeRange
import platform.AVFoundation.videoCompositionWithAsset
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGRectIsInfinite
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreImage.CIColor
import platform.CoreImage.CIImage
import platform.CoreImage.CIVector
import platform.CoreImage.kCIImageApplyOrientationProperty
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.CMTimeRange
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMPersistentTrackID_Invalid
import platform.CoreVideo.CVPixelBufferCreate
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetDataSize
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferRefVar
import platform.CoreVideo.CVPixelBufferRelease
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.CoreVideo.kCVReturnSuccess
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithBytes
import platform.posix.memset

/** Frame rate of the video composition. */
private const val FRAME_RATE = 30

/** Time scale of all CMTime values. */
private const val TIMESCALE = 600

/**
 * Composition of the clips for AVFoundation.
 *
 * @property asset Video and audio tracks of all clips.
 * @property videoComposition Draws each frame: upright, filled, filtered, and with the overlays.
 */
class MediaComposition(val asset: AVMutableComposition, val videoComposition: AVMutableVideoComposition)

/**
 * Builds the composition of the clips. The preview player and the export use it.
 *
 * Every clip fills a [frameWidth] × [frameHeight] frame first. When [videoArea] is not the whole
 * frame, the clip then fills the area, and the area moves to its place in the frame. The parts
 * outside are cut off. The filter changes only the video. The overlay tracks go on top of the whole frame.
 *
 * A photo range uses a short black video as a placeholder track, and the frame handler draws the photo there.
 *
 * @param clips Clips in play order. All clips must have a source file.
 * @param filter Color filter for the video.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @param videoArea Area of the frame that shows the video.
 * @param overlayTracks Images on top of the video. Each track holds images in time order. The first track is at the bottom.
 * @return The composition, or null when a clip has no file or AVFoundation cannot make the video composition.
 */
@OptIn(ExperimentalForeignApi::class)
suspend fun buildMediaComposition(
    clips: List<Clip>,
    filter: FilterType,
    frameWidth: Int,
    frameHeight: Int,
    videoArea: FrameArea = FrameArea.FULL,
    overlayTracks: List<List<ExportOverlay>> = emptyList(),
): MediaComposition? {
    val composition = AVMutableComposition()
    val videoTrack = composition.addMutableTrackWithMediaType(AVMediaTypeVideo, kCMPersistentTrackID_Invalid) ?: return null
    val audioTrack = composition.addMutableTrackWithMediaType(AVMediaTypeAudio, kCMPersistentTrackID_Invalid) ?: return null
    val sources = mutableListOf<FrameSource>()
    var hasAudio = false
    var start = 0.0

    for (clip in clips) {
        val url = clip.sourceUri?.let { NSURL.URLWithString(it) } ?: return null
        val at = CMTimeMakeWithSeconds(start, TIMESCALE)
        val length = CMTimeMakeWithSeconds(clip.duration, TIMESCALE)
        when (clip.type) {
            MediaType.PHOTO -> {
                val filler = fillerVideoTrack()
                if (filler == null || !videoTrack.insertScaled(filler, filler.timeRange, at, length)) {
                    videoTrack.insertEmptyTimeRange(CMTimeRangeMake(at, length))
                }
                audioTrack.insertEmptyTimeRange(CMTimeRangeMake(at, length))
                val photo = CIImage.imageWithContentsOfURL(url, options = mapOf<Any?, Any?>(kCIImageApplyOrientationProperty to true))
                sources += FrameSource(start = start, transform = null, photo = photo)
            }
            MediaType.VIDEO -> {
                val asset = AVURLAsset(uRL = url, options = null)
                val video = asset.loadFirstTrack(AVMediaTypeVideo)
                val audio = asset.loadFirstTrack(AVMediaTypeAudio)
                val range = CMTimeRangeMake(
                    CMTimeMakeWithSeconds(clip.inPoint, TIMESCALE),
                    CMTimeMakeWithSeconds(clip.outPoint - clip.inPoint, TIMESCALE),
                )
                if (video == null || !videoTrack.insertScaled(video, range, at, length)) {
                    videoTrack.insertEmptyTimeRange(CMTimeRangeMake(at, length))
                }
                if (audio != null && audioTrack.insertScaled(audio, range, at, length)) {
                    hasAudio = true
                } else {
                    audioTrack.insertEmptyTimeRange(CMTimeRangeMake(at, length))
                }
                sources += FrameSource(start = start, transform = video?.coreImageTransform(), photo = null)
            }
        }
        start += clip.duration
    }
    if (!hasAudio) composition.removeTrack(audioTrack)

    val renderer = FrameRenderer(
        sources = sources,
        frameWidth = frameWidth.toDouble(),
        frameHeight = frameHeight.toDouble(),
        videoArea = videoArea,
        matrix = filter.colorMatrix(),
        overlayTracks = overlayTracks.map { track ->
            track.map { OverlayImage(it.start, it.end, it.png.toNSData()?.let { data -> CIImage.imageWithData(data) }) }
        },
    )
    val videoComposition = suspendCancellableCoroutine<AVMutableVideoComposition?> { continuation ->
        AVMutableVideoComposition.videoCompositionWithAsset(
            asset = composition,
            applyingCIFiltersWithHandler = { request: AVAsynchronousCIImageFilteringRequest? -> request?.let(renderer::render) },
            completionHandler = { result: AVMutableVideoComposition?, _: NSError? -> continuation.resume(result) },
        )
    } ?: return null
    videoComposition.setRenderSize(CGSizeMake(frameWidth.toDouble(), frameHeight.toDouble()))
    videoComposition.setFrameDuration(CMTimeMake(value = 1, timescale = FRAME_RATE))
    return MediaComposition(asset = composition, videoComposition = videoComposition)
}

/**
 * Inserts [range] of [track] at [at], then changes its length to [length].
 *
 * @return False when the track cannot be inserted.
 */
@OptIn(ExperimentalForeignApi::class)
private fun AVMutableCompositionTrack.insertScaled(
    track: AVAssetTrack,
    range: CValue<CMTimeRange>,
    at: CValue<CMTime>,
    length: CValue<CMTime>,
): Boolean {
    if (!insertTimeRange(range, ofTrack = track, atTime = at, error = null)) return false
    val duration = range.useContents { duration.readValue() }
    scaleTimeRange(CMTimeRangeMake(at, duration), toDuration = length)
    return true
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun AVURLAsset.loadFirstTrack(mediaType: String?): AVAssetTrack? = suspendCancellableCoroutine { continuation ->
    loadTracksWithMediaType(mediaType) { tracks, _ -> continuation.resume(tracks?.firstOrNull() as? AVAssetTrack) }
}

/**
 * Converts the preferred transform of a video track to Core Image coordinates.
 *
 * The track transform uses a y-down space, but Core Image uses a y-up space. The y flip changes the
 * sign of b and c. The frame handler moves the image back to the origin, so the translation is not used.
 */
@OptIn(ExperimentalForeignApi::class)
private fun AVAssetTrack.coreImageTransform(): CValue<CGAffineTransform> =
    preferredTransform.useContents { CGAffineTransformMake(a, -b, -c, d, 0.0, 0.0) }

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData? = usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }

// =====================================================================
// Frame handler
// =====================================================================

/**
 * Frame source of one clip on the timeline.
 *
 * @property start Start time on the timeline, in seconds.
 * @property transform Rotation of the video frame in Core Image coordinates. Null for a photo.
 * @property photo Photo image, or null for a video.
 */
@OptIn(ExperimentalForeignApi::class)
private class FrameSource(val start: Double, val transform: CValue<CGAffineTransform>?, val photo: CIImage?)

/**
 * Overlay image for a time range.
 *
 * @property start Start time in seconds.
 * @property end End time in seconds.
 * @property image Image with the frame size, or null when the PNG data is not valid.
 */
private class OverlayImage(val start: Double, val end: Double, val image: CIImage?)

/**
 * Draws the frames of the composition. AVFoundation calls [render] on its own threads, so the class keeps no changing state.
 */
@OptIn(ExperimentalForeignApi::class)
private class FrameRenderer(
    private val sources: List<FrameSource>,
    private val frameWidth: Double,
    private val frameHeight: Double,
    private val videoArea: FrameArea,
    private val matrix: FloatArray?,
    private val overlayTracks: List<List<OverlayImage>>,
) {
    private val frameRect = CGRectMake(0.0, 0.0, frameWidth, frameHeight)

    fun render(request: AVAsynchronousCIImageFilteringRequest) {
        val time = CMTimeGetSeconds(request.compositionTime)
        val source = sources.lastOrNull { time >= it.start - TIME_EPSILON } ?: sources.first()
        // AVFoundation은 CIImage를 전방 선언으로만 알기 때문에 Core Image 타입으로 바꿔서 쓴다.
        @Suppress("CAST_NEVER_SUCCEEDS")
        val sourceImage = request.sourceImage as CIImage
        val frame = source.photo ?: source.transform?.let { sourceImage.imageByApplyingTransform(it) }
        var video = frame?.takeUnless { CGRectIsInfinite(it.extent) }?.fill(frameWidth, frameHeight) ?: blackImage()
        if (!videoArea.isFullFrame) video = video.placeInArea()
        if (matrix != null) video = video.applyColorMatrix(matrix)
        var image = video.imageByCompositingOverImage(blackImage())
        overlayTracks.forEach { track ->
            track.firstOrNull { time >= it.start - TIME_EPSILON && time < it.end - TIME_EPSILON }?.image?.let {
                image = it.imageByCompositingOverImage(image)
            }
        }
        request.finishWithImage(image.imageByCroppingToRect(frameRect) as objcnames.classes.CIImage, context = null)
    }

    /** Fills the area of the frame with the image. Core Image puts the origin at the bottom left. */
    private fun CIImage.placeInArea(): CIImage {
        val width = videoArea.width * frameWidth
        val height = videoArea.height * frameHeight
        val x = videoArea.left * frameWidth
        val y = (1.0 - videoArea.top - videoArea.height) * frameHeight
        return fill(width, height).imageByApplyingTransform(CGAffineTransformMakeTranslation(x, y))
    }

    private fun blackImage(): CIImage = CIImage.imageWithColor(CIColor.blackColor).imageByCroppingToRect(frameRect)
}

/** Scales the image to fill a [width] × [height] box at the origin, keeps the center, and cuts off the rest. */
@OptIn(ExperimentalForeignApi::class)
private fun CIImage.fill(width: Double, height: Double): CIImage {
    val (x, y, imageWidth, imageHeight) = extent.useContents { listOf(origin.x, origin.y, size.width, size.height) }
    if (imageWidth <= 0.0 || imageHeight <= 0.0) return this
    val scale = maxOf(width / imageWidth, height / imageHeight)
    val dx = (imageWidth * scale - width) / 2
    val dy = (imageHeight * scale - height) / 2
    return imageByApplyingTransform(CGAffineTransformMakeTranslation(-x, -y))
        .imageByApplyingTransform(CGAffineTransformMakeScale(scale, scale))
        .imageByApplyingTransform(CGAffineTransformMakeTranslation(-dx, -dy))
        .imageByCroppingToRect(CGRectMake(0.0, 0.0, width, height))
}

/**
 * Applies a 4x5 color matrix.
 *
 * Core Image filters work on linear values. The matrix is for gamma-encoded sRGB values, as in the
 * Compose filter. So the image goes to sRGB first, and comes back to linear after the matrix.
 */
@OptIn(ExperimentalForeignApi::class)
private fun CIImage.applyColorMatrix(m: FloatArray): CIImage {
    fun row(index: Int) = CIVector.vectorWithX(
        m[index * 5].toDouble(),
        Y = m[index * 5 + 1].toDouble(),
        Z = m[index * 5 + 2].toDouble(),
        W = m[index * 5 + 3].toDouble(),
    )
    val bias = CIVector.vectorWithX(m[4] / 255.0, Y = m[9] / 255.0, Z = m[14] / 255.0, W = m[19] / 255.0)
    return imageByApplyingFilter("CILinearToSRGBToneCurve", withInputParameters = emptyMap<Any?, Any?>())
        .imageByApplyingFilter(
            "CIColorMatrix",
            withInputParameters = mapOf<Any?, Any?>(
                "inputRVector" to row(0),
                "inputGVector" to row(1),
                "inputBVector" to row(2),
                "inputAVector" to row(3),
                "inputBiasVector" to bias,
            ),
        )
        .imageByApplyingFilter("CISRGBToneCurveToLinear", withInputParameters = emptyMap<Any?, Any?>())
}

// =====================================================================
// Placeholder video for photos
// =====================================================================

/** Name of the placeholder video in the temporary directory. Change the version when the content changes. */
private const val FILLER_FILE_NAME = "magic-preview-filler-v1.mp4"

/** Width and height of the placeholder video. */
private const val FILLER_SIZE = 16

/**
 * Returns the video track of a 1-second black video. The app writes the file once.
 *
 * A composition needs a real video track for its length. Empty ranges only make a composition of photos zero seconds long.
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun fillerVideoTrack(): AVAssetTrack? {
    val path = NSTemporaryDirectory() + FILLER_FILE_NAME
    if (!NSFileManager.defaultManager.fileExistsAtPath(path) && !writeFillerVideo(path)) return null
    return AVURLAsset(uRL = NSURL.fileURLWithPath(path), options = null).loadFirstTrack(AVMediaTypeVideo)
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun writeFillerVideo(path: String): Boolean {
    // 다른 호출이 반쯤 쓴 파일을 읽지 않게 임시 파일에 쓰고 마지막에 옮긴다.
    val tempPath = NSTemporaryDirectory() + NSUUID().UUIDString + ".mp4"
    val writer = AVAssetWriter.assetWriterWithURL(NSURL.fileURLWithPath(tempPath), fileType = AVFileTypeMPEG4, error = null)
        ?: return false
    val input = AVAssetWriterInput.assetWriterInputWithMediaType(
        AVMediaTypeVideo,
        outputSettings = mapOf<Any?, Any?>(
            AVVideoCodecKey to AVVideoCodecTypeH264,
            AVVideoWidthKey to FILLER_SIZE,
            AVVideoHeightKey to FILLER_SIZE,
        ),
    ).apply { expectsMediaDataInRealTime = false }
    val adaptor = AVAssetWriterInputPixelBufferAdaptor.assetWriterInputPixelBufferAdaptorWithAssetWriterInput(
        input,
        sourcePixelBufferAttributes = null,
    )
    writer.addInput(input)
    if (!writer.startWriting()) return false
    writer.startSessionAtSourceTime(CMTimeMake(value = 0, timescale = 1))

    val buffer = memScoped {
        val ref = alloc<CVPixelBufferRefVar>()
        val status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            FILLER_SIZE.toULong(),
            FILLER_SIZE.toULong(),
            kCVPixelFormatType_32BGRA,
            null,
            ref.ptr,
        )
        if (status != kCVReturnSuccess) null else ref.value
    } ?: return false
    CVPixelBufferLockBaseAddress(buffer, 0u)
    memset(CVPixelBufferGetBaseAddress(buffer), 0, CVPixelBufferGetDataSize(buffer))
    CVPixelBufferUnlockBaseAddress(buffer, 0u)

    // 0초와 0.5초에 검은 프레임을 넣고 1초에서 끝낸다.
    for (frame in 0..1) {
        while (!input.readyForMoreMediaData) delay(5)
        adaptor.appendPixelBuffer(buffer, withPresentationTime = CMTimeMake(value = frame.toLong(), timescale = 2))
    }
    CVPixelBufferRelease(buffer)
    input.markAsFinished()
    writer.endSessionAtSourceTime(CMTimeMake(value = 1, timescale = 1))
    suspendCancellableCoroutine { continuation -> writer.finishWritingWithCompletionHandler { continuation.resume(Unit) } }
    if (writer.status != AVAssetWriterStatusCompleted) return false

    val manager = NSFileManager.defaultManager
    if (!manager.fileExistsAtPath(path)) manager.moveItemAtPath(tempPath, toPath = path, error = null)
    manager.removeItemAtPath(tempPath, error = null)
    return manager.fileExistsAtPath(path)
}
