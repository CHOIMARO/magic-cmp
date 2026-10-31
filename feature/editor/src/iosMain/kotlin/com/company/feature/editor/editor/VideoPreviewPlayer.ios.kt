package com.company.feature.editor.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.TransitionWindow
import com.company.core.media.PREVIEW_FRAME_HEIGHT
import com.company.core.media.PREVIEW_FRAME_WIDTH
import com.company.core.media.buildMediaComposition
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFoundation.AVAudioTimePitchAlgorithmSpectral
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerActionAtItemEndPause
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.audioTimePitchAlgorithm
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.audioMix
import platform.AVFoundation.videoComposition
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSLog
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObjectProtocol

/** Time scale of all CMTime values. */
private const val TIMESCALE = 600

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
 * The player uses the same composition as the export ([buildMediaComposition]), so the preview and the saved video match.
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

    override fun setClips(clips: List<Clip>, transitions: List<TransitionWindow>, time: Double) {
        if (clips.isEmpty()) return
        targetTime = time
        ended = false
        loading = true
        loadJob?.cancel()
        loadJob = scope.launch {
            val item = buildPlayerItem(clips, transitions)
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

/** Makes the player item of the clips with the same composition as the export. */
@OptIn(ExperimentalForeignApi::class)
private suspend fun buildPlayerItem(clips: List<Clip>, transitions: List<TransitionWindow>): AVPlayerItem? {
    val composition = buildMediaComposition(
        clips = clips,
        frameWidth = PREVIEW_FRAME_WIDTH,
        frameHeight = PREVIEW_FRAME_HEIGHT,
        transitions = transitions,
    ) ?: return null
    return AVPlayerItem(asset = composition.asset).apply {
        videoComposition = composition.videoComposition
        composition.audioMix?.let { audioMix = it }
        // 배속을 바꿔도 소리의 높이는 그대로 둔다.
        audioTimePitchAlgorithm = AVAudioTimePitchAlgorithmSpectral
    }
}
