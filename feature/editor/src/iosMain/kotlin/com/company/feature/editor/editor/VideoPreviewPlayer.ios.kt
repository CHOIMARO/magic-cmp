package com.company.feature.editor.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.MediaType
import com.company.ui.editor.colorMatrix
import kotlin.coroutines.resume
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterInputPixelBufferAdaptor
import platform.AVFoundation.AVAssetWriterStatusCompleted
import platform.AVFoundation.AVAsynchronousCIImageFilteringRequest
import platform.AVFoundation.AVAudioTimePitchAlgorithmSpectral
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVMutableCompositionTrack
import platform.AVFoundation.AVMutableVideoComposition
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerActionAtItemEndPause
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.audioTimePitchAlgorithm
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeH264
import platform.AVFoundation.AVVideoHeightKey
import platform.AVFoundation.AVVideoWidthKey
import platform.AVFoundation.addMutableTrackWithMediaType
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.loadTracksWithMediaType
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.removeTrack
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeRange
import platform.AVFoundation.videoComposition
import platform.AVFoundation.videoCompositionWithAsset
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.CoreFoundation.kCFAllocatorDefault
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
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObjectProtocol
import platform.posix.memset

/** Size of the output frame. All clips are scaled and cut to fill it. */
private const val FRAME_WIDTH = 1080.0
private const val FRAME_HEIGHT = 1920.0

/** Frame rate of the video composition. */
private const val FRAME_RATE = 30

/** Time scale of all CMTime values. */
private const val TIMESCALE = 600

/** Time difference, in seconds, that counts as the same time when the frame handler finds a clip. */
private const val TIME_EPSILON = 0.0001

@Composable
internal actual fun rememberVideoPreviewPlayer(): VideoPreviewPlayer {
    val scope = rememberCoroutineScope()
    val player = remember { IosVideoPreviewPlayer(scope) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * Plays the clips with [AVPlayer].
 *
 * The player puts all clips into one [AVMutableComposition]. A Core Image handler turns each frame
 * upright, fills the 9:16 frame, and applies the filter. A photo range uses a short black video as
 * a placeholder track, and the handler draws the photo there.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosVideoPreviewPlayer(private val scope: CoroutineScope) : VideoPreviewPlayer {
    private val player = AVPlayer().apply { actionAtItemEnd = AVPlayerActionAtItemEndPause }
    private var loadJob: Job? = null
    private var endObserver: NSObjectProtocol? = null

    /** True while the player item for new clips loads. */
    private var loading = false

    /** True while a seek runs. */
    private var seeking = false

    /** Last position that the screen asked for. */
    private var targetTime = 0.0

    /** True when the screen wants playback. The player starts after the item loads. */
    private var wantsPlay = false

    override var ended = false
        private set

    init {
        // 무음 스위치가 켜져 있어도 미리보기 소리가 나오게 한다.
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
    }

    override val currentTime: Double
        get() {
            if (loading || seeking || player.currentItem == null) return targetTime
            val seconds = CMTimeGetSeconds(player.currentTime())
            return if (seconds.isNaN()) targetTime else seconds
        }

    override fun setClips(clips: List<Clip>, filter: FilterType, time: Double) {
        if (clips.isEmpty()) return
        targetTime = time
        ended = false
        loading = true
        loadJob?.cancel()
        loadJob = scope.launch {
            val item = buildPlayerItem(clips, filter)
            loading = false
            if (item == null) {
                NSLog("Preview: cannot build the player item")
                return@launch
            }
            observeEnd(item)
            player.replaceCurrentItemWithPlayerItem(item)
            seekPlayer(targetTime)
            if (wantsPlay) player.play()
        }
    }

    override fun play() {
        wantsPlay = true
        ended = false
        if (!loading) player.play()
    }

    override fun pause() {
        wantsPlay = false
        player.pause()
    }

    override fun seekTo(time: Double) {
        targetTime = time
        ended = false
        if (!loading) seekPlayer(time)
    }

    override fun release() {
        loadJob?.cancel()
        removeEndObserver()
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun VideoSurface(modifier: Modifier) {
        UIKitView(
            factory = { PlayerLayerView(player) },
            modifier = modifier,
            onRelease = { it.release() },
            // 터치는 Compose가 받는다. 무대를 누르면 재생과 일시정지가 바뀐다.
            properties = UIKitInteropProperties(interactionMode = null, isNativeAccessibilityEnabled = false),
        )
    }

    private fun seekPlayer(time: Double) {
        seeking = true
        val target = CMTimeMakeWithSeconds(time, TIMESCALE)
        val zero = CMTimeMake(value = 0, timescale = TIMESCALE)
        player.seekToTime(target, toleranceBefore = zero, toleranceAfter = zero) { finished ->
            // 다음 이동이 이번 이동을 취소하면 finished가 false다. 그때는 다음 이동이 끝날 때까지 기다린다.
            if (finished) seeking = false
        }
    }

    private fun observeEnd(item: AVPlayerItem) {
        removeEndObserver()
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> ended = true }
    }

    private fun removeEndObserver() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
    }
}

/** Shows the frames of an [AVPlayer]. The frame fills the view, and the parts outside are cut off. */
@OptIn(ExperimentalForeignApi::class)
private class PlayerLayerView(player: AVPlayer) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // 크기가 바뀔 때 레이어가 애니메이션으로 따라오지 않게 한다.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }

    fun release() {
        playerLayer.player = null
    }
}

// =====================================================================
// Composition
// =====================================================================

/**
 * Frame source of one clip on the timeline.
 *
 * @property start Start time on the timeline, in seconds.
 * @property transform Rotation of the video frame in Core Image coordinates. Null for a photo.
 * @property photo Photo image, or null for a video.
 */
@OptIn(ExperimentalForeignApi::class)
private class FrameSource(
    val start: Double,
    val transform: CValue<platform.CoreGraphics.CGAffineTransform>?,
    val photo: CIImage?,
)

@OptIn(ExperimentalForeignApi::class)
private suspend fun buildPlayerItem(clips: List<Clip>, filter: FilterType): AVPlayerItem? {
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

    val matrix = filter.colorMatrix()
    val videoComposition = suspendCancellableCoroutine<AVMutableVideoComposition?> { continuation ->
        AVMutableVideoComposition.videoCompositionWithAsset(
            asset = composition,
            applyingCIFiltersWithHandler = { request: AVAsynchronousCIImageFilteringRequest? ->
                request?.let { renderFrame(it, sources, matrix) }
            },
            completionHandler = { result: AVMutableVideoComposition?, _: platform.Foundation.NSError? -> continuation.resume(result) },
        )
    } ?: return null
    videoComposition.setRenderSize(CGSizeMake(FRAME_WIDTH, FRAME_HEIGHT))
    videoComposition.setFrameDuration(CMTimeMake(value = 1, timescale = FRAME_RATE))

    return AVPlayerItem(asset = composition).apply {
        this.videoComposition = videoComposition
        // 배속을 바꿔도 소리의 높이는 그대로 둔다.
        audioTimePitchAlgorithm = AVAudioTimePitchAlgorithmSpectral
    }
}

/**
 * Inserts [range] of [track] at [at], then changes its length to [length].
 *
 * @return False when the track cannot be inserted.
 */
@OptIn(ExperimentalForeignApi::class)
private fun AVMutableCompositionTrack.insertScaled(
    track: AVAssetTrack,
    range: CValue<platform.CoreMedia.CMTimeRange>,
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
private fun AVAssetTrack.coreImageTransform(): CValue<platform.CoreGraphics.CGAffineTransform> =
    preferredTransform.useContents { CGAffineTransformMake(a, -b, -c, d, 0.0, 0.0) }

// =====================================================================
// Frame handler
// =====================================================================

/** Draws one frame. It runs on a thread of AVFoundation. */
@OptIn(ExperimentalForeignApi::class)
private fun renderFrame(request: AVAsynchronousCIImageFilteringRequest, sources: List<FrameSource>, matrix: FloatArray?) {
    val time = CMTimeGetSeconds(request.compositionTime)
    val source = sources.lastOrNull { time >= it.start - TIME_EPSILON } ?: sources.first()
    // AVFoundation은 CIImage를 전방 선언으로만 알기 때문에 Core Image 타입으로 바꿔서 쓴다.
    @Suppress("CAST_NEVER_SUCCEEDS")
    val sourceImage = request.sourceImage as CIImage
    val frame = source.photo ?: source.transform?.let { sourceImage.imageByApplyingTransform(it) }
    var image = frame?.takeUnless { CGRectIsInfinite(it.extent) }?.fillFrame() ?: blackFrame()
    if (matrix != null) image = image.applyColorMatrix(matrix)
    request.finishWithImage(image as objcnames.classes.CIImage, context = null)
}

/** Scales the image to fill the output frame, keeps the center, and cuts off the rest. */
@OptIn(ExperimentalForeignApi::class)
private fun CIImage.fillFrame(): CIImage {
    val (x, y, width, height) = extent.useContents { listOf(origin.x, origin.y, size.width, size.height) }
    if (width <= 0.0 || height <= 0.0) return blackFrame()
    val scale = maxOf(FRAME_WIDTH / width, FRAME_HEIGHT / height)
    val dx = (width * scale - FRAME_WIDTH) / 2
    val dy = (height * scale - FRAME_HEIGHT) / 2
    return imageByApplyingTransform(CGAffineTransformMakeTranslation(-x, -y))
        .imageByApplyingTransform(CGAffineTransformMakeScale(scale, scale))
        .imageByApplyingTransform(CGAffineTransformMakeTranslation(-dx, -dy))
        .imageByCroppingToRect(CGRectMake(0.0, 0.0, FRAME_WIDTH, FRAME_HEIGHT))
}

@OptIn(ExperimentalForeignApi::class)
private fun blackFrame(): CIImage =
    CIImage.imageWithColor(CIColor.blackColor).imageByCroppingToRect(CGRectMake(0.0, 0.0, FRAME_WIDTH, FRAME_HEIGHT))

/**
 * Applies a 4x5 Compose color matrix.
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
