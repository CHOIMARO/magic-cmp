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
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.MediaType
import com.company.ui.editor.colorMatrix
import kotlin.math.roundToLong

private const val TAG = "VideoPreviewPlayer"

/** Size of the output frame. All clips are scaled and cut to fill it. */
private const val FRAME_WIDTH = 1080
private const val FRAME_HEIGHT = 1920

/** Frame rate of a photo clip. */
private const val PHOTO_FRAME_RATE = 30

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
 * The player uses the same [Composition] model as the Media3 Transformer, so the export can use it later.
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

    override fun setClips(clips: List<Clip>, filter: FilterType, time: Double) {
        if (clips.isEmpty()) return
        player.setComposition(clips.toComposition(filter), time.toMillis())
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
            val fillWidth = maxWidth / maxHeight > FRAME_WIDTH.toFloat() / FRAME_HEIGHT
            val width = if (fillWidth) maxWidth else maxHeight * FRAME_WIDTH.toFloat() / FRAME_HEIGHT.toFloat()
            val height = if (fillWidth) maxWidth * FRAME_HEIGHT.toFloat() / FRAME_WIDTH.toFloat() else maxHeight
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

/** Makes one sequence of all clips. Every clip gets the same frame size and filter. */
@OptIn(UnstableApi::class, ExperimentalApi::class)
private fun List<Clip>.toComposition(filter: FilterType): Composition {
    val videoEffects = buildList<Effect> {
        add(Presentation.createForWidthAndHeight(FRAME_WIDTH, FRAME_HEIGHT, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
        filter.colorMatrix()?.let { add(it.toRgbMatrix()) }
    }
    val effects = Effects(emptyList(), videoEffects)
    val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_VIDEO))
        .addItems(map { it.toEditedMediaItem(effects) })
        .build()
    return Composition.Builder(sequence).build()
}

@OptIn(UnstableApi::class, ExperimentalApi::class)
private fun Clip.toEditedMediaItem(effects: Effects): EditedMediaItem {
    val uri = checkNotNull(sourceUri) { "Clip $id has no source file" }
    return when (type) {
        MediaType.PHOTO -> {
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setImageDurationMs(duration.toMillis().coerceAtLeast(1L))
                .build()
            EditedMediaItem.Builder(mediaItem)
                .setFrameRate(PHOTO_FRAME_RATE)
                .setEffects(effects)
                .build()
        }
        MediaType.VIDEO -> {
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(inPoint.toMillis())
                .setEndPositionMs(outPoint.toMillis())
                .build()
            val mediaItem = MediaItem.Builder().setUri(uri).setClippingConfiguration(clipping).build()
            EditedMediaItem.Builder(mediaItem)
                .setDurationUs((sourceDuration * 1_000_000).roundToLong())
                .setEffects(effects)
                .apply {
                    // 배속을 바꿔도 소리의 높이는 그대로 둔다.
                    if (speed != 1.0) setSpeed(SpeedParameters(ConstantSpeedProvider(speed.toFloat()), true))
                }
                .build()
        }
    }
}

/** Plays the whole clip at one speed. */
@OptIn(UnstableApi::class)
private class ConstantSpeedProvider(private val speed: Float) : SpeedProvider {
    override fun getSpeed(timeUs: Long): Float = speed

    override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
}

/**
 * Converts a 4x5 Compose color matrix to a Media3 [RgbMatrix].
 *
 * Media3 multiplies the column-major 4x4 matrix with (r, g, b, 1). The last column holds the offsets in 0..1.
 * With the default working color space, SDR colors stay gamma-encoded, as in the Compose filter.
 */
@OptIn(UnstableApi::class)
private fun FloatArray.toRgbMatrix(): RgbMatrix {
    val m = this
    val matrix = floatArrayOf(
        m[0], m[5], m[10], 0f,
        m[1], m[6], m[11], 0f,
        m[2], m[7], m[12], 0f,
        m[4] / 255f, m[9] / 255f, m[14] / 255f, 1f,
    )
    return RgbMatrix { _, _ -> matrix }
}

private fun Double.toMillis(): Long = (this * 1000).roundToLong()
