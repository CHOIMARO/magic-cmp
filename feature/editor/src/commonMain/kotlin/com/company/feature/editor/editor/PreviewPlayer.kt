package com.company.feature.editor.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.TransitionWindow
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Plays the clips of the project for the editor preview.
 *
 * All times are in seconds on the project timeline. Call the functions on the main thread.
 */
internal interface PreviewPlayer {
    /** Position of the player. */
    val currentTime: Double

    /** True when the playback reached the end of the project. */
    val ended: Boolean

    /**
     * Sets the clips, the filter, and the transitions to play.
     *
     * @param clips Clips in play order.
     * @param filter Color filter for all clips.
     * @param transitions Transitions in time order.
     * @param time Position to show after the change.
     */
    fun setClips(clips: List<Clip>, filter: FilterType, transitions: List<TransitionWindow>, time: Double)

    /** Starts the playback from the current position. */
    fun play()

    /** Pauses the playback. */
    fun pause()

    /**
     * Moves the position.
     *
     * @param time New position.
     */
    fun seekTo(time: Double)

    /** Releases the resources of the player. Do not use the player after this call. */
    fun release()
}

/** Preview player that also shows the video frames. */
internal interface VideoPreviewPlayer : PreviewPlayer {
    /**
     * Shows the video frames. The 9:16 frame fills the bounds, and the parts outside are cut off.
     *
     * Use only one surface at a time.
     *
     * @param modifier Modifier for the surface. Give it a size.
     */
    @Composable
    fun VideoSurface(modifier: Modifier)
}

/**
 * Creates the platform player that plays the source files of the clips.
 *
 * The player is released when it leaves the composition.
 */
@Composable
internal expect fun rememberVideoPreviewPlayer(): VideoPreviewPlayer

/**
 * Preview player for clips that have no source file, for example the sample clips.
 *
 * It only moves the position with a clock. The stage shows placeholders.
 */
internal class ClockPreviewPlayer : PreviewPlayer {
    private var duration = 0.0

    /** Position when the clock started, or the position while paused. */
    private var startTime = 0.0

    /** Start of the clock. Null while paused. */
    private var mark: TimeMark? = null

    override val currentTime: Double
        get() {
            val elapsed = mark?.elapsedNow()?.inWholeMicroseconds?.div(1_000_000.0) ?: 0.0
            return (startTime + elapsed).coerceAtMost(duration)
        }

    override val ended: Boolean
        get() = mark != null && currentTime >= duration

    override fun setClips(clips: List<Clip>, filter: FilterType, transitions: List<TransitionWindow>, time: Double) {
        duration = clips.sumOf { it.duration }
        seekTo(time)
    }

    override fun play() {
        if (mark == null) mark = TimeSource.Monotonic.markNow()
    }

    override fun pause() {
        startTime = currentTime
        mark = null
    }

    override fun seekTo(time: Double) {
        startTime = time.coerceIn(0.0, duration)
        if (mark != null) mark = TimeSource.Monotonic.markNow()
    }

    override fun release() = Unit
}
