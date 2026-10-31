package com.company.core.media

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.ExportOverlay
import com.company.domain.model.editor.FrameArea
import com.company.domain.model.editor.MAX_VOLUME
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.TransitionWindow
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterInputPixelBufferAdaptor
import platform.AVFoundation.AVAssetWriterStatusCompleted
import platform.AVFoundation.AVAsynchronousCIImageFilteringRequest
import platform.AVFoundation.AVAudioMix
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableAudioMix
import platform.AVFoundation.AVMutableAudioMixInputParameters
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
import platform.AVFoundation.naturalSize
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
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeApplyAffineTransform
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

/** Time from the end of a clip to its last frame, in seconds. It is one frame of the composition. */
private const val LAST_FRAME_OFFSET = 1.0 / FRAME_RATE

/** Time tolerance of the second read of a still frame, in seconds. The first read gets the exact frame. */
private const val STILL_FRAME_TOLERANCE = 0.5

/**
 * Composition of the clips for AVFoundation.
 *
 * @property asset Video and audio tracks of all clips.
 * @property videoComposition Draws each frame: upright, filled, with the transitions, and with the overlays.
 * @property audioMix Volume of the sound of each clip, or null when all clips use the full volume.
 */
class MediaComposition(
    val asset: AVMutableComposition,
    val videoComposition: AVMutableVideoComposition,
    val audioMix: AVAudioMix? = null,
)

/**
 * Builds the composition of the clips. The preview player and the export use it.
 *
 * Every clip fills a [frameWidth] × [frameHeight] frame first. When [videoArea] is not the whole
 * frame, the clip then fills the area, and the area moves to its place in the frame. The parts
 * outside are cut off. The overlay tracks go on top of the whole frame.
 *
 * A photo range uses a short black video as a placeholder track, and the frame handler draws the photo there.
 *
 * A transition changes the filled video in the video area. The overlay tracks do not change. During a
 * boundary transition, the clip at the time plays, and the other clip shows a still frame. Before the boundary, it is
 * the first frame of the next clip. After the boundary, it is the last frame of the previous clip.
 *
 * The volume of each clip goes into the audio mix of the composition.
 *
 * @param clips Clips in play order. All clips must have a source file.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @param videoArea Area of the frame that shows the video.
 * @param overlayTracks Images on top of the video. Each track holds images in time order. The first track is at the bottom.
 * @param transitions Transitions of the video, in time order. They change only the video area. See [transitionLook].
 * @return The composition, or null when a clip has no file or AVFoundation cannot make the video composition.
 */
@OptIn(ExperimentalForeignApi::class)
suspend fun buildMediaComposition(
    clips: List<Clip>,
    frameWidth: Int,
    frameHeight: Int,
    videoArea: FrameArea = FrameArea.FULL,
    overlayTracks: List<List<ExportOverlay>> = emptyList(),
    transitions: List<TransitionWindow> = emptyList(),
): MediaComposition? {
    val composition = AVMutableComposition()
    val videoTrack = composition.addMutableTrackWithMediaType(AVMediaTypeVideo, kCMPersistentTrackID_Invalid) ?: return null
    val audioTrack = composition.addMutableTrackWithMediaType(AVMediaTypeAudio, kCMPersistentTrackID_Invalid) ?: return null
    val sources = mutableListOf<FrameSource>()
    val stills = mutableListOf<StillFrameSource>()
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
                stills += StillFrameSource(clip = clip, photo = photo, asset = null, track = null)
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
                stills += StillFrameSource(clip = clip, photo = null, asset = asset, track = video)
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
        overlayTracks = overlayTracks.map { track ->
            track.map { OverlayImage(it.start, it.end, it.png.toNSData()?.let { data -> CIImage.imageWithData(data) }) }
        },
        transitions = loadTransitionFrames(transitions, stills, frameWidth.toDouble(), frameHeight.toDouble()),
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
    return MediaComposition(
        asset = composition,
        videoComposition = videoComposition,
        audioMix = if (hasAudio) clipVolumeMix(audioTrack, clips) else null,
    )
}

/**
 * Makes the audio mix that sets the volume of each clip.
 *
 * Each clip sets its volume at its start, so a clip with the full volume sets the full volume again.
 *
 * @param track Audio track of the composition.
 * @param clips Clips in play order.
 * @return The audio mix, or null when all clips use the full volume.
 */
@OptIn(ExperimentalForeignApi::class)
private fun clipVolumeMix(track: AVAssetTrack, clips: List<Clip>): AVAudioMix? {
    if (clips.all { it.volume == MAX_VOLUME }) return null
    val parameters = AVMutableAudioMixInputParameters.audioMixInputParametersWithTrack(track)
    // 합성 트랙에 클립을 넣을 때와 같은 방법으로 시작 시간을 더한다.
    var start = 0.0
    for (clip in clips) {
        val volume = clip.volume.coerceIn(0, MAX_VOLUME).toFloat() / MAX_VOLUME
        parameters.setVolume(volume, atTime = CMTimeMakeWithSeconds(start, TIMESCALE))
        start += clip.duration
    }
    return AVMutableAudioMix().apply { setInputParameters(listOf(parameters)) }
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
// Still frames of the transitions
// =====================================================================

/**
 * Source of the still frames of one clip.
 *
 * @property clip The clip.
 * @property photo Photo image, or null for a video.
 * @property asset Video file, or null for a photo.
 * @property track Video track of [asset], or null when the file has no video track.
 */
@OptIn(ExperimentalForeignApi::class)
private class StillFrameSource(val clip: Clip, val photo: CIImage?, val asset: AVURLAsset?, val track: AVAssetTrack?) {
    /**
     * Reads the upright frame at the in point of the clip. A photo clip gives the photo.
     *
     * @param frameWidth Width of the output frame in pixels.
     * @param frameHeight Height of the output frame in pixels.
     */
    suspend fun firstFrame(frameWidth: Double, frameHeight: Double): CIImage? = frameAt(clip.inPoint, frameWidth, frameHeight)

    /**
     * Reads the upright frame one frame before the out point of the clip. A photo clip gives the photo.
     *
     * @param frameWidth Width of the output frame in pixels.
     * @param frameHeight Height of the output frame in pixels.
     */
    suspend fun lastFrame(frameWidth: Double, frameHeight: Double): CIImage? =
        frameAt(maxOf(clip.inPoint, clip.outPoint - LAST_FRAME_OFFSET), frameWidth, frameHeight)

    private suspend fun frameAt(seconds: Double, frameWidth: Double, frameHeight: Double): CIImage? {
        if (photo != null) return photo
        return readVideoFrame(asset ?: return null, track ?: return null, seconds, frameWidth, frameHeight)
    }
}

/**
 * Loads the still frames of the boundary transitions. All frames load at the same time.
 *
 * @param windows Transitions in time order.
 * @param stills Still frame sources of the clips, in play order.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @return One item for each window, in the same order.
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun loadTransitionFrames(
    windows: List<TransitionWindow>,
    stills: List<StillFrameSource>,
    frameWidth: Double,
    frameHeight: Double,
): List<TransitionFrames> = coroutineScope {
    windows.map { window ->
        // 시작 전환은 검은 화면에서 시작하므로 정지 프레임이 필요 없다.
        val previous = window.fromClip?.let { stills.getOrNull(it) }
        val next = if (window.fromClip == null) null else stills.getOrNull(window.toClip)
        val last = previous?.let { async { it.lastFrame(frameWidth, frameHeight) } }
        val first = next?.let { async { it.firstFrame(frameWidth, frameHeight) } }
        Triple(window, last, first)
    }.map { (window, last, first) -> TransitionFrames(window, lastFrame = last?.await(), firstFrame = first?.await()) }
}

/**
 * Reads the upright frame of a video track at a time in the source.
 *
 * The first read gets the exact frame. When it fails, for example at the end of the track, the second read
 * accepts a near frame. The frame is not larger than the size that fills the output frame.
 *
 * @param asset Video file.
 * @param track Video track of [asset].
 * @param seconds Time in the source.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @return The frame, or null when AVFoundation cannot read it.
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun readVideoFrame(
    asset: AVURLAsset,
    track: AVAssetTrack,
    seconds: Double,
    frameWidth: Double,
    frameHeight: Double,
): CIImage? {
    val generator = AVAssetImageGenerator(asset = asset).apply {
        appliesPreferredTrackTransform = true
        track.stillFrameBox(frameWidth, frameHeight)?.let { maximumSize = it }
        requestedTimeToleranceBefore = CMTimeMakeWithSeconds(0.0, TIMESCALE)
        requestedTimeToleranceAfter = CMTimeMakeWithSeconds(0.0, TIMESCALE)
    }
    val time = CMTimeMakeWithSeconds(seconds, TIMESCALE)
    generator.readFrame(time)?.let { return it }
    generator.requestedTimeToleranceBefore = CMTimeMakeWithSeconds(STILL_FRAME_TOLERANCE, TIMESCALE)
    generator.requestedTimeToleranceAfter = CMTimeMakeWithSeconds(STILL_FRAME_TOLERANCE, TIMESCALE)
    return generator.readFrame(time)
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun AVAssetImageGenerator.readFrame(time: CValue<CMTime>): CIImage? =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancelAllCGImageGeneration() }
        // CGImage는 콜백 안에서만 유효하므로, 콜백 안에서 CIImage로 감싸서 붙잡아 둔다.
        generateCGImageAsynchronouslyForTime(time) { image, _, _ ->
            continuation.resume(image?.let { CIImage.imageWithCGImage(it) })
        }
    }

/**
 * Returns the square box for the image generator. A frame in the box has enough pixels to fill the output frame.
 *
 * The generator scales the image to fit in the box. A square box gives the same scale before and after the rotation.
 *
 * @return The box, or null when the size of the track is not known.
 */
@OptIn(ExperimentalForeignApi::class)
private fun AVAssetTrack.stillFrameBox(frameWidth: Double, frameHeight: Double): CValue<CGSize>? {
    val (width, height) = CGSizeApplyAffineTransform(naturalSize, preferredTransform).useContents { abs(width) to abs(height) }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = maxOf(frameWidth / width, frameHeight / height)
    val side = ceil(maxOf(width, height) * scale)
    return CGSizeMake(side, side)
}

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
 * Transition with the still frames of the clips at its boundary.
 *
 * During a boundary transition, one clip plays, and the other clip shows a still frame.
 *
 * @property window Time range and kind of the transition.
 * @property lastFrame Last frame of the clip before the boundary. It shows after the boundary. Null for the opening
 *   transition, or when AVFoundation cannot read the frame. Then the transition draws only the clip that plays.
 * @property firstFrame First frame of the clip after the boundary. It shows before the boundary. Null for the opening
 *   transition, or when AVFoundation cannot read the frame.
 */
private class TransitionFrames(val window: TransitionWindow, val lastFrame: CIImage?, val firstFrame: CIImage?)

/**
 * Draws the frames of the composition. AVFoundation calls [render] on its own threads, so the class keeps no changing state.
 *
 * @param transitions Transitions with upright still frames. The renderer fills the still frames one time.
 */
@OptIn(ExperimentalForeignApi::class)
private class FrameRenderer(
    private val sources: List<FrameSource>,
    private val frameWidth: Double,
    private val frameHeight: Double,
    private val videoArea: FrameArea,
    private val overlayTracks: List<List<OverlayImage>>,
    transitions: List<TransitionFrames>,
) {
    private val frameRect = CGRectMake(0.0, 0.0, frameWidth, frameHeight)

    // 영상 영역의 크기와 위치. Core Image는 원점이 왼쪽 아래라서 y는 아래쪽 여백이다.
    private val areaWidth = videoArea.width * frameWidth
    private val areaHeight = videoArea.height * frameHeight
    private val areaX = videoArea.left * frameWidth
    private val areaY = (1.0 - videoArea.top - videoArea.height) * frameHeight
    private val areaRect = CGRectMake(0.0, 0.0, areaWidth, areaHeight)
    private val areaBlack = CIImage.imageWithColor(CIColor.blackColor).imageByCroppingToRect(areaRect)
    private val areaWhite = CIImage.imageWithColor(CIColor.whiteColor).imageByCroppingToRect(areaRect)

    /** Transitions with still scenes: the still frames, filled to the video area. */
    private val transitionScenes = transitions.map { transition ->
        TransitionFrames(
            window = transition.window,
            lastFrame = transition.lastFrame?.let { sceneImage(it) },
            firstFrame = transition.firstFrame?.let { sceneImage(it) },
        )
    }

    fun render(request: AVAsynchronousCIImageFilteringRequest) {
        val time = CMTimeGetSeconds(request.compositionTime)
        val source = sources.lastOrNull { time >= it.start - TIME_EPSILON } ?: sources.first()
        // AVFoundation은 CIImage를 전방 선언으로만 알기 때문에 Core Image 타입으로 바꿔서 쓴다.
        @Suppress("CAST_NEVER_SUCCEEDS")
        val sourceImage = request.sourceImage as CIImage
        val frame = source.photo ?: source.transform?.let { sourceImage.imageByApplyingTransform(it) }
        var scene = sceneImage(frame)
        transitionScenes.firstOrNull { it.window.fromClip != null && it.window.covers(time) }?.let {
            scene = drawBoundary(it, scene, time)
        }
        transitionScenes.firstOrNull { it.window.fromClip == null && it.window.covers(time) }?.let {
            scene = drawOpening(it, scene, time)
        }
        val video = if (videoArea.isFullFrame) {
            scene
        } else {
            scene.imageByApplyingTransform(CGAffineTransformMakeTranslation(areaX, areaY))
        }
        var image = video.imageByCompositingOverImage(blackImage())
        overlayTracks.forEach { track ->
            track.firstOrNull { time >= it.start - TIME_EPSILON && time < it.end - TIME_EPSILON }?.image?.let {
                image = it.imageByCompositingOverImage(image)
            }
        }
        request.finishWithImage(image.imageByCroppingToRect(frameRect) as objcnames.classes.CIImage, context = null)
    }

    /**
     * Fills the frame with the image, then fills the video area.
     *
     * @param frame Upright frame, or null for black.
     * @return The scene. It has the size of the video area, and it is at the origin.
     */
    private fun sceneImage(frame: CIImage?): CIImage {
        var scene = frame?.takeUnless { CGRectIsInfinite(it.extent) }?.fill(frameWidth, frameHeight) ?: blackImage()
        if (!videoArea.isFullFrame) scene = scene.fill(areaWidth, areaHeight)
        return scene
    }

    // =====================================================================
    // Transitions
    // =====================================================================

    /**
     * Draws a boundary transition. The clip at the time plays, and the other clip shows its still scene.
     *
     * @param transition Transition with the still scenes.
     * @param live Scene of the clip that plays.
     * @param time Time on the timeline.
     */
    private fun drawBoundary(transition: TransitionFrames, live: CIImage, time: Double): CIImage {
        val window = transition.window
        val look = transitionLook(window.transition.type, window.progressAt(time).toFloat(), window.transition.direction)
        // 경계 전에는 앞 클립이 재생되고 뒤 클립은 첫 프레임에 멈춰 있다. 경계 뒤에는 그 반대다.
        return if (time >= window.boundary - TIME_EPSILON) {
            drawLook(look, from = transition.lastFrame, to = live)
        } else {
            drawLook(look, from = live, to = transition.firstFrame)
        }
    }

    /**
     * Draws the opening transition. The scene comes out of black.
     *
     * A very short first clip can have the opening and the next boundary transition at the same time.
     * Then [scene] already shows the boundary transition.
     *
     * @param transition The opening transition.
     * @param scene Scene at the time.
     * @param time Time on the timeline.
     */
    private fun drawOpening(transition: TransitionFrames, scene: CIImage, time: Double): CIImage {
        val window = transition.window
        return drawLook(openingLook(window.transition.type, window.progressAt(time).toFloat()), from = null, to = scene)
    }

    /**
     * Draws one moment of a transition in the video area: black, then [from], then [to], then the cover color.
     *
     * @param look Look of the moment.
     * @param from Scene before the boundary, or null to draw no scene there.
     * @param to Scene after the boundary, or null to draw no scene there.
     */
    private fun drawLook(look: TransitionLook, from: CIImage?, to: CIImage?): CIImage {
        val toScene = to?.withLook(look.to)
        // 뒤 장면이 불투명하고 움직이지 않으면 영역을 다 덮으므로 앞 장면은 그리지 않는다.
        val toCoversArea = toScene != null && look.to.alpha >= 1f && !look.to.movesScene
        var image = areaBlack
        if (!look.fromIsBlack && !toCoversArea) from?.withLook(look.from)?.let { image = it.imageByCompositingOverImage(image) }
        toScene?.let { image = it.imageByCompositingOverImage(image) }
        if (look.coverAlpha > 0f) {
            val cover = if (look.cover == CoverColor.WHITE) areaWhite else areaBlack
            image = cover.withAlpha(look.coverAlpha).imageByCompositingOverImage(image)
        }
        return image
    }

    /**
     * Changes a scene as [look] tells: colors, blur, move, size, skew, turn, and opacity. The result stays in the video
     * area.
     *
     * @return The changed scene, or null when the scene is not visible.
     */
    private fun CIImage.withLook(look: SceneLook): CIImage? {
        if (look.alpha <= 0f) return null
        var scene = this
        if (look.hueDegrees != 0f || look.saturation != 1f) {
            scene = scene.applyColorMatrix(hueSaturationMatrix(look.hueDegrees, look.saturation))
        }
        if (look.blur > 0f) scene = scene.blurred(look.blur * areaWidth)
        sceneTransform(look)?.let { scene = scene.imageByApplyingTransform(it).imageByCroppingToRect(areaRect) }
        if (look.alpha < 1f) scene = scene.withAlpha(look.alpha)
        return scene
    }

    /**
     * Returns the move, the size change, the horizontal skew, and the turn of [look] around the center of the video
     * area.
     *
     * @return The transform, or null when the look does not move the scene.
     */
    private fun sceneTransform(look: SceneLook): CValue<CGAffineTransform>? {
        if (!look.movesScene) return null
        val scale = look.scale.toDouble()
        // Core Image의 y축은 위쪽이 양수라서, 화면 기준 값인 기울기, 회전, 세로 이동의 부호를 바꾼다.
        val shear = -scale * tan(look.skewXDegrees * PI / 180)
        val angle = -look.rotationDegrees * PI / 180
        val cosA = cos(angle)
        val sinA = sin(angle)
        // 크기와 기울기 [[scale, shear], [0, scale]]를 먼저 주고 돌린다.
        val a = cosA * scale
        val b = sinA * scale
        val c = cosA * shear - sinA * scale
        val d = sinA * shear + cosA * scale
        val centerX = areaWidth / 2
        val centerY = areaHeight / 2
        return CGAffineTransformMake(
            a,
            b,
            c,
            d,
            centerX - a * centerX - c * centerY + look.translateX * areaWidth,
            centerY - b * centerX - d * centerY - look.translateY * areaHeight,
        )
    }

    private fun blackImage(): CIImage = CIImage.imageWithColor(CIColor.blackColor).imageByCroppingToRect(frameRect)
}

/** True when the transition runs at [time]. */
private fun TransitionWindow.covers(time: Double): Boolean = time >= start - TIME_EPSILON && time < end - TIME_EPSILON

/** True when the look moves, scales, skews, or turns the scene. */
private val SceneLook.movesScene: Boolean
    get() = scale != 1f || translateX != 0f || translateY != 0f || skewXDegrees != 0f || rotationDegrees != 0f

/**
 * Blurs the image. The edge pixels repeat outside the image, so the edges do not become dark.
 *
 * @param radius Blur radius in pixels.
 */
@OptIn(ExperimentalForeignApi::class)
private fun CIImage.blurred(radius: Double): CIImage = imageByClampingToExtent()
    .imageByApplyingFilter("CIGaussianBlur", withInputParameters = mapOf<Any?, Any?>("inputRadius" to radius))
    .imageByCroppingToRect(extent)

/**
 * Multiplies the opacity of the image.
 *
 * @param alpha Opacity factor from 0 to 1.
 */
@OptIn(ExperimentalForeignApi::class)
private fun CIImage.withAlpha(alpha: Float): CIImage = imageByApplyingFilter(
    "CIColorMatrix",
    withInputParameters = mapOf<Any?, Any?>("inputAVector" to CIVector.vectorWithX(0.0, Y = 0.0, Z = 0.0, W = alpha.toDouble())),
)

/**
 * Returns the 4x5 color matrix of a hue rotation and a saturation change, the same as the CSS filters `hue-rotate()`
 * and `saturate()` of the prototype. Use it with [applyColorMatrix].
 *
 * Both filters use the same luminance weights, so the order of the two filters does not change the result.
 *
 * @param hueDegrees Hue rotation in degrees.
 * @param saturation Saturation factor. 1 keeps the colors.
 */
private fun hueSaturationMatrix(hueDegrees: Float, saturation: Float): FloatArray {
    val angle = hueDegrees * PI / 180
    val keep = saturation * cos(angle)
    val turn = saturation * sin(angle)
    // CSS 필터 명세의 휘도 가중치와 hue-rotate의 sin 계수다.
    val luminance = doubleArrayOf(0.213, 0.715, 0.072)
    val rotation = arrayOf(
        doubleArrayOf(-0.213, -0.715, 0.928),
        doubleArrayOf(0.143, 0.140, -0.283),
        doubleArrayOf(-0.787, 0.715, 0.072),
    )
    return FloatArray(20) { index ->
        val row = index / 5
        val column = index % 5
        when {
            row == 3 -> if (column == 3) 1f else 0f
            column >= 3 -> 0f
            else -> {
                val identity = if (row == column) 1.0 else 0.0
                (luminance[column] + keep * (identity - luminance[column]) + turn * rotation[row][column]).toFloat()
            }
        }
    }
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
