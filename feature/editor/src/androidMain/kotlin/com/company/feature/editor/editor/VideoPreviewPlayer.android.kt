package com.company.feature.editor.editor

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.FilterType
import com.company.domain.model.editor.TransitionWindow
import com.company.core.media.PREVIEW_FRAME_HEIGHT
import com.company.core.media.PREVIEW_FRAME_WIDTH
import com.company.core.media.buildMediaComposition
import kotlin.math.roundToLong

private const val TAG = "VideoPreviewPlayer"

@Composable
internal actual fun rememberVideoPreviewPlayer(): VideoPreviewPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember { AndroidVideoPreviewPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * Plays the clips with the Media3 [CompositionPlayer].
 *
 * The player uses the same composition as the export ([buildMediaComposition]), so the preview and the saved video match.
 */
@OptIn(UnstableApi::class, ExperimentalApi::class)
private class AndroidVideoPreviewPlayer(context: Context) : VideoPreviewPlayer {
    private val player = CompositionPlayer.Builder(context).build()

    /** True after the first [CompositionPlayer.prepare]. A playback error makes it false again. */
    private var prepared = false

    /** True after the first [CompositionPlayer.setComposition]. The player cannot seek before it. */
    private var hasContent = false
    private var released = false

    /** Surface that gets the frames, or null. */
    private var surface: Surface? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "Preview playback failed", error)
                prepared = false
            }
        })
    }

    override val currentTime: Double
        get() = player.currentPosition / 1000.0

    override val ended: Boolean
        get() = player.playbackState == Player.STATE_ENDED

    override fun setClips(clips: List<Clip>, filter: FilterType, transitions: List<TransitionWindow>, time: Double) {
        if (clips.isEmpty()) return
        val composition = buildMediaComposition(
            clips = clips,
            filter = filter,
            frameWidth = PREVIEW_FRAME_WIDTH,
            frameHeight = PREVIEW_FRAME_HEIGHT,
            transitions = transitions,
        )
        player.setComposition(composition, time.toMillis())
        hasContent = true
        if (!prepared) {
            player.prepare()
            prepared = true
        }
    }

    override fun play() = player.play()

    override fun pause() = player.pause()

    override fun seekTo(time: Double) {
        if (hasContent) player.seekTo(time.toMillis())
    }

    override fun release() {
        released = true
        player.release()
    }

    @Composable
    override fun VideoSurface(modifier: Modifier) {
        BoxWithConstraints(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
            // TextureView는 화면 비율대로 늘려 그린다. 9:16 크기로 칸을 덮고, 칸 밖은 잘라서 가운데만 보인다.
            val fillWidth = maxWidth / maxHeight > PREVIEW_FRAME_WIDTH.toFloat() / PREVIEW_FRAME_HEIGHT
            val width = if (fillWidth) maxWidth else maxHeight * PREVIEW_FRAME_WIDTH.toFloat() / PREVIEW_FRAME_HEIGHT.toFloat()
            val height = if (fillWidth) maxWidth * PREVIEW_FRAME_HEIGHT.toFloat() / PREVIEW_FRAME_WIDTH.toFloat() else maxHeight
            AndroidView(
                factory = { context -> TextureView(context).apply { surfaceTextureListener = SurfaceListener() } },
                modifier = Modifier.requiredSize(width, height),
            )
        }
    }

    /** Gives the surface of one [TextureView] to the player. */
    private inner class SurfaceListener : TextureView.SurfaceTextureListener {
        private var textureSurface: Surface? = null

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            val newSurface = Surface(texture)
            textureSurface = newSurface
            attachSurface(newSurface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            textureSurface?.let { attachSurface(it, width, height) }
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            textureSurface?.let { detachSurface(it) }
            textureSurface = null
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    private fun attachSurface(newSurface: Surface, width: Int, height: Int) {
        if (released) return
        surface = newSurface
        player.setVideoSurface(newSurface, Size(width, height))
        // 멈춘 상태에서는 새 프레임이 오지 않으므로, 같은 위치로 이동해서 지금 화면을 다시 그린다.
        if (hasContent && !player.isPlaying) player.seekTo(player.currentPosition)
    }

    // 레이아웃이 바뀌면 새 TextureView가 먼저 붙을 수 있다. 지금 쓰는 Surface일 때만 뗀다.
    private fun detachSurface(oldSurface: Surface) {
        if (!released && surface == oldSurface) {
            player.clearVideoSurface(oldSurface)
            surface = null
        }
        oldSurface.release()
    }
}

private fun Double.toMillis(): Long = (this * 1000).roundToLong()
