package com.company.feature.capture.camera

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.newEditorId
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.EditorSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * One recorded part. The user can record several parts in a row.
 *
 * @property duration Length in seconds.
 * @property hue Hue of the placeholder preview.
 */
data class RecordedSegment(val duration: Double, val hue: Int)

/**
 * State of the camera screen.
 *
 * @property segments Recorded parts in order.
 * @property recording True while the camera records.
 * @property maxSeconds Maximum total length of the recording.
 */
data class CameraState(
    val segments: List<RecordedSegment> = emptyList(),
    val recording: Boolean = false,
    val maxSeconds: Int = 15,
) : State() {
    /** Total recorded length in seconds. */
    val recordedSeconds: Double
        get() = segments.sumOf { it.duration }

    /** True when the user can remove the last part or continue. */
    val canUndoOrContinue: Boolean
        get() = segments.isNotEmpty() && !recording
}

/** User actions on the camera screen. */
sealed class CameraIntent : Intent() {
    /** ToggleRecord : Starts or pauses the recording. */
    data object ToggleRecord : CameraIntent()

    /** SelectMaxDuration(seconds) : Changes the maximum length. Ignored while recording. */
    data class SelectMaxDuration(val seconds: Int) : CameraIntent()

    /** RemoveLastSegment : Removes the last recorded part. */
    data object RemoveLastSegment : CameraIntent()

    /** Done : Continues with the recorded parts. */
    data object Done : CameraIntent()
}

/** One-time events of the camera screen. */
sealed class CameraSideEffect : SideEffect() {
    /** OpenMagic : Navigates to the magic screen with the new project. */
    data object OpenMagic : CameraSideEffect()

    /** OpenEditor : Navigates to the editor with the new project. */
    data object OpenEditor : CameraSideEffect()
}

/**
 * ViewModel of the camera screen.
 *
 * The prototype has no real camera. A timer simulates the recording.
 */
class CameraViewModel(
    private val settingsRepository: EditorSettingsRepository,
    private val projectRepository: EditorProjectRepository,
) : BaseViewModel<CameraState, CameraIntent, CameraSideEffect>(
    initialState = CameraState()
) {
    private var recordJob: Job? = null

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: CameraIntent) {
        when (intent) {
            CameraIntent.ToggleRecord -> if (currentState.recording) pauseRecording() else startRecording()
            is CameraIntent.SelectMaxDuration -> if (!currentState.recording) reduce { copy(maxSeconds = intent.seconds) }
            CameraIntent.RemoveLastSegment -> reduce { copy(segments = segments.dropLast(1)) }
            CameraIntent.Done -> done()
        }
    }

    // =====================================================================
    // Recording
    // =====================================================================

    private fun startRecording() {
        val state = currentState
        if (state.recordedSeconds >= state.maxSeconds - 0.1) return
        val hue = SegmentHues[(state.segments.size * 5 + 3) % SegmentHues.size]
        reduce { copy(recording = true, segments = segments + RecordedSegment(duration = 0.0, hue = hue)) }

        recordJob?.cancel()
        recordJob = viewModelScope.launch {
            var mark = TimeSource.Monotonic.markNow()
            while (isActive) {
                delay(TICK_MILLIS)
                val elapsed = mark.elapsedNow().inWholeMilliseconds / 1000.0
                mark = TimeSource.Monotonic.markNow()
                if (!extendLastSegment(elapsed)) break
            }
        }
    }

    /**
     * Adds time to the last part. Stops the recording at the maximum length.
     *
     * @return True to continue the recording.
     */
    private fun extendLastSegment(seconds: Double): Boolean {
        val state = currentState
        val last = state.segments.lastOrNull() ?: return false
        val before = state.recordedSeconds - last.duration
        val duration = minOf(last.duration + seconds, state.maxSeconds - before)
        val full = before + duration >= state.maxSeconds - 0.01
        reduce { copy(segments = segments.dropLast(1) + last.copy(duration = duration), recording = !full) }
        return !full
    }

    private fun pauseRecording() {
        recordJob?.cancel()
        reduce { copy(recording = false) }
    }

    private fun done() {
        val clips = currentState.segments.filter { it.duration > MIN_SEGMENT_SECONDS }.mapIndexed { index, segment ->
            Clip(
                id = newEditorId(),
                name = "촬영 ${index + 1}",
                sourceDuration = segment.duration,
                inPoint = 0.0,
                outPoint = segment.duration,
                thumbnailHue = segment.hue,
            )
        }
        if (clips.isEmpty()) return
        projectRepository.start(EditorProject(name = NEW_RECORDING_NAME, clips = clips))
        val autoMagic = settingsRepository.settings.value.autoMagic
        postSideEffect { if (autoMagic) CameraSideEffect.OpenMagic else CameraSideEffect.OpenEditor }
    }

    private companion object {
        /** Update interval of the simulated recording. */
        const val TICK_MILLIS = 50L

        /** Shorter parts are not used as clips. */
        const val MIN_SEGMENT_SECONDS = 0.3

        /** Name of a project that starts from a recording. */
        const val NEW_RECORDING_NAME = "새 촬영"

        /** Hues of the placeholder previews of the recorded parts. */
        val SegmentHues = listOf(35, 80, 150, 200, 250, 300, 20, 120, 180, 270, 330, 60, 220, 100, 0, 160, 45, 240)
    }
}
