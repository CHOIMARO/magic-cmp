package com.company.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.ExportOverlay
import com.company.domain.model.editor.FrameArea
import com.company.domain.model.editor.MAX_VOLUME
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.TransitionWindow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Frame rate of a photo clip. */
private const val PHOTO_FRAME_RATE = 30

/** Largest number of audio channels that the clip volume supports. It is 7.1 sound. */
private const val MAX_AUDIO_CHANNELS = 8

/**
 * Builds the Media3 composition of the clips. The preview player and the export use it.
 *
 * Every clip fills a [frameWidth] × [frameHeight] frame first. When [videoArea] is not the whole
 * frame, the clip then fills the area, and the area moves to its place in the frame. The parts
 * outside are cut off. The transitions change only the video. The overlay tracks go on top of
 * the whole frame. The volume of each video clip changes only the sound of that clip.
 *
 * @param clips Clips in play order. All clips must have a source file.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @param videoArea Area of the frame that shows the video.
 * @param overlayTracks Images on top of the video. Each track holds images in time order. The first track is at the bottom.
 * @param transitions Transitions of the video, in time order. They change only the video area. See [transitionLook].
 * @return The composition. The audio track has silence where a clip has no sound.
 */
@OptIn(UnstableApi::class, ExperimentalApi::class)
fun buildMediaComposition(
    clips: List<Clip>,
    frameWidth: Int,
    frameHeight: Int,
    videoArea: FrameArea = FrameArea.FULL,
    overlayTracks: List<List<ExportOverlay>> = emptyList(),
    transitions: List<TransitionWindow> = emptyList(),
): Composition {
    val areaWidth = (videoArea.width * frameWidth).roundToInt().coerceAtLeast(1)
    val areaHeight = (videoArea.height * frameHeight).roundToInt().coerceAtLeast(1)
    val itemEffects = buildList<Effect> {
        add(Presentation.createForWidthAndHeight(frameWidth, frameHeight, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
        if (!videoArea.isFullFrame) {
            add(Presentation.createForWidthAndHeight(areaWidth, areaHeight, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
        }
    }
    val compositionEffects = buildList<Effect> {
        // 전환은 영상 칸만 바꾼다. 칸을 제자리로 옮기기 전에 두어야 레이아웃과 글자 오버레이가 그대로 남는다.
        addAll(
            transitionEffects(
                clips = clips,
                transitions = transitions,
                clipStartsUs = clips.compositionStartsUs(),
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                areaWidth = areaWidth,
                areaHeight = areaHeight,
            ),
        )
        if (!videoArea.isFullFrame) add(AreaPlacement(frameWidth, frameHeight, videoArea))
        val tracks = overlayTracks.filter { it.isNotEmpty() }
        if (tracks.isNotEmpty()) add(OverlayEffect(tracks.map { TimedBitmapOverlay(it) }))
    }
    // 모든 클립이 같은 영상 효과 목록을 쓴다. 목록이 같으면 Media3가 클립마다 셰이더를 다시 만들지 않는다.
    val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_VIDEO))
        .addItems(clips.map { it.toEditedMediaItem(itemEffects) })
        .build()
    return Composition.Builder(sequence)
        .setEffects(Effects(emptyList(), compositionEffects))
        .build()
}

@OptIn(UnstableApi::class, ExperimentalApi::class)
private fun Clip.toEditedMediaItem(videoEffects: List<Effect>): EditedMediaItem {
    val uri = checkNotNull(sourceUri) { "Clip $id has no source file" }
    return when (type) {
        MediaType.PHOTO -> {
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setImageDurationMs(photoDurationMs())
                .build()
            EditedMediaItem.Builder(mediaItem)
                .setFrameRate(PHOTO_FRAME_RATE)
                .setEffects(Effects(emptyList(), videoEffects))
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
                .setEffects(Effects(volumeProcessors(), videoEffects))
                .apply {
                    // 배속을 바꿔도 소리의 높이는 그대로 둔다.
                    if (speed != 1.0) setSpeed(SpeedParameters(ConstantSpeedProvider(speed.toFloat()), true))
                }
                .build()
        }
    }
}

/** Show time of a photo clip in milliseconds. */
private fun Clip.photoDurationMs(): Long = duration.toMillis().coerceAtLeast(1L)

/**
 * Start time of each clip in the composition, in microseconds.
 *
 * It uses the same rounding as [toEditedMediaItem], so it is the time where Media3 starts the clip.
 */
private fun List<Clip>.compositionStartsUs(): List<Long> {
    var start = 0L
    return map { clip ->
        val clipStart = start
        start += when (clip.type) {
            MediaType.PHOTO -> clip.photoDurationMs() * 1000
            MediaType.VIDEO -> ((clip.outPoint.toMillis() - clip.inPoint.toMillis()) * 1000 / clip.speed).roundToLong()
        }
        clipStart
    }
}

/**
 * Audio processors that set the volume of the clip sound. The list is empty at full volume.
 *
 * A diagonal matrix multiplies each channel by the volume. The processor keeps the number of channels.
 */
@OptIn(UnstableApi::class)
private fun Clip.volumeProcessors(): List<AudioProcessor> {
    if (volume >= MAX_VOLUME) return emptyList()
    val gain = volume.coerceAtLeast(0) / MAX_VOLUME.toFloat()
    val processor = ChannelMixingAudioProcessor()
    for (channels in 1..MAX_AUDIO_CHANNELS) {
        val coefficients = FloatArray(channels * channels) { index -> if (index % (channels + 1) == 0) gain else 0f }
        processor.putChannelMixingMatrix(ChannelMixingMatrix(channels, channels, coefficients))
    }
    return listOf(processor)
}

/** Plays the whole clip at one speed. */
@OptIn(UnstableApi::class)
private class ConstantSpeedProvider(private val speed: Float) : SpeedProvider {
    override fun getSpeed(timeUs: Long): Float = speed

    override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
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
