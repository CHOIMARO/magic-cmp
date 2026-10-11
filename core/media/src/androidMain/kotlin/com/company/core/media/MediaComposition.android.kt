package com.company.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.ExportOverlay
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.FrameArea
import com.company.core.domain.model.editor.MediaType
import com.company.core.domain.model.editor.colorMatrix
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Frame rate of a photo clip. */
private const val PHOTO_FRAME_RATE = 30

/**
 * Builds the Media3 composition of the clips. The preview player and the export use it.
 *
 * Every clip fills a [frameWidth] × [frameHeight] frame first. When [videoArea] is not the whole
 * frame, the clip then fills the area, and the area moves to its place in the frame. The parts
 * outside are cut off. The filter changes only the video. The overlay tracks go on top of the whole frame.
 *
 * @param clips Clips in play order. All clips must have a source file.
 * @param filter Color filter for the video.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @param videoArea Area of the frame that shows the video.
 * @param overlayTracks Images on top of the video. Each track holds images in time order. The first track is at the bottom.
 * @return The composition. The audio track has silence where a clip has no sound.
 */
@OptIn(UnstableApi::class, ExperimentalApi::class)
fun buildMediaComposition(
    clips: List<Clip>,
    filter: FilterType,
    frameWidth: Int,
    frameHeight: Int,
    videoArea: FrameArea = FrameArea.FULL,
    overlayTracks: List<List<ExportOverlay>> = emptyList(),
): Composition {
    val itemEffects = buildList<Effect> {
        add(Presentation.createForWidthAndHeight(frameWidth, frameHeight, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
        if (!videoArea.isFullFrame) {
            val areaWidth = (videoArea.width * frameWidth).roundToInt().coerceAtLeast(1)
            val areaHeight = (videoArea.height * frameHeight).roundToInt().coerceAtLeast(1)
            add(Presentation.createForWidthAndHeight(areaWidth, areaHeight, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
        }
        filter.colorMatrix()?.let { add(it.toRgbMatrix()) }
    }
    val compositionEffects = buildList<Effect> {
        if (!videoArea.isFullFrame) add(AreaPlacement(frameWidth, frameHeight, videoArea))
        val tracks = overlayTracks.filter { it.isNotEmpty() }
        if (tracks.isNotEmpty()) add(OverlayEffect(tracks.map { TimedBitmapOverlay(it) }))
    }
    val effects = Effects(emptyList(), itemEffects)
    val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_VIDEO))
        .addItems(clips.map { it.toEditedMediaItem(effects) })
        .build()
    return Composition.Builder(sequence)
        .setEffects(Effects(emptyList(), compositionEffects))
        .build()
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
 * Converts a 4x5 color matrix to a Media3 [RgbMatrix].
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

/**
 * Puts the video frame into [area] of a [frameWidth] × [frameHeight] frame. The rest of the frame is empty.
 *
 * The shader first stretches the input over the whole output in normalized device coordinates
 * (-1 to 1, y up). The matrix then scales it to the area size and moves it to the area center.
 */
@OptIn(UnstableApi::class)
private class AreaPlacement(
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val area: FrameArea,
) : MatrixTransformation {
    private val matrix = Matrix().apply {
        setScale(area.width, area.height)
        postTranslate((area.left + area.width / 2) * 2 - 1, 1 - (area.top + area.height / 2) * 2)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size = Size(frameWidth, frameHeight)

    override fun getMatrix(presentationTimeUs: Long): Matrix = matrix
}

/**
 * Shows the overlay image of the current time. It decodes an image only when the time reaches it.
 *
 * Media3 draws a bitmap at its pixel size, so an overlay with the frame size covers the whole frame.
 */
@OptIn(UnstableApi::class)
private class TimedBitmapOverlay(private val overlays: List<ExportOverlay>) : BitmapOverlay() {
    /** Transparent bitmap for a time without an overlay. */
    private val empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var index = -1
    private var bitmap: Bitmap? = null

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val time = presentationTimeUs / 1_000_000.0
        val next = overlays.indexOfFirst { time >= it.start - TIME_EPSILON && time < it.end - TIME_EPSILON }
        if (next != index) {
            index = next
            bitmap = overlays.getOrNull(next)?.let { BitmapFactory.decodeByteArray(it.png, 0, it.png.size) }
        }
        return bitmap ?: empty
    }
}

private fun Double.toMillis(): Long = (this * 1000).roundToLong()
