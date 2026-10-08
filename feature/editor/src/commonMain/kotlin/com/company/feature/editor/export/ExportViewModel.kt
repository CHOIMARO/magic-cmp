package com.company.feature.editor.export

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.VideoSummary
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.MyVideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Video quality of the export.
 *
 * @property label Label of the option.
 * @property note Short note under the label.
 * @property megabytesPerSecond Estimated file size per second of video.
 */
enum class ExportResolution(val label: String, val note: String, val megabytesPerSecond: Double) {
    /** Small file. */
    HD("720p", "가볍게", 0.5),

    /** Recommended quality. */
    FULL_HD("1080p", "추천", 1.1),

    /** Best quality. */
    UHD("4K", "최고 화질", 4.2),
}

/** Step of the export. */
enum class ExportPhase {
    /** The user picks the quality. */
    SETUP,

    /** The export runs. */
    RUNNING,

    /** The video is saved. */
    DONE,
}

/**
 * State of the export screen.
 *
 * @property project Project to export.
 * @property resolution Selected quality.
 * @property phase Current step.
 * @property progress Export progress from 0 to 100.
 * @property showOptions True to show the quality options.
 */
data class ExportState(
    val project: EditorProject = EditorProject(name = ""),
    val resolution: ExportResolution = ExportResolution.FULL_HD,
    val phase: ExportPhase = ExportPhase.SETUP,
    val progress: Int = 0,
    val showOptions: Boolean = false,
) : State() {
    /** Estimated file size in megabytes. */
    val estimatedMegabytes: Int
        get() = maxOf(1, (resolution.megabytesPerSecond * project.totalDuration).roundToInt())
}

/** User actions on the export screen. */
sealed class ExportIntent : Intent() {
    /** SelectResolution(resolution) : Changes the quality. */
    data class SelectResolution(val resolution: ExportResolution) : ExportIntent()

    /** ToggleOptions : Shows or hides the quality options. */
    data object ToggleOptions : ExportIntent()

    /** Start : Starts the export. */
    data object Start : ExportIntent()

    /** Cancel : Stops the export and goes back to the setup step. */
    data object Cancel : ExportIntent()

    /** Back : Stops the export and goes back to the editor. */
    data object Back : ExportIntent()

    /** GoHome : Goes to the home tab. */
    data object GoHome : ExportIntent()
}

/** One-time events of the export screen. */
sealed class ExportSideEffect : SideEffect() {
    /** Close : Goes back to the editor. */
    data object Close : ExportSideEffect()

    /** GoHome : Navigates to the home tab. */
    data object GoHome : ExportSideEffect()
}

/**
 * ViewModel of the export screen.
 *
 * The prototype has no real encoder. A timer simulates the export.
 */
class ExportViewModel(
    projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
) : BaseViewModel<ExportState, ExportIntent, ExportSideEffect>(
    initialState = ExportState(project = projectRepository.project.value)
) {
    private var exportJob: Job? = null

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: ExportIntent) {
        when (intent) {
            is ExportIntent.SelectResolution -> reduce { copy(resolution = intent.resolution) }
            ExportIntent.ToggleOptions -> reduce { copy(showOptions = !showOptions) }
            ExportIntent.Start -> start()
            ExportIntent.Cancel -> {
                exportJob?.cancel()
                reduce { copy(phase = ExportPhase.SETUP, progress = 0) }
            }
            ExportIntent.Back -> {
                exportJob?.cancel()
                postSideEffect { ExportSideEffect.Close }
            }
            ExportIntent.GoHome -> postSideEffect { ExportSideEffect.GoHome }
        }
    }

    // =====================================================================
    // Export
    // =====================================================================

    private fun start() {
        exportJob?.cancel()
        reduce { copy(phase = ExportPhase.RUNNING, progress = 0) }
        exportJob = viewModelScope.launch {
            while (currentState.progress < 100 - PROGRESS_STEP) {
                delay(TICK_MILLIS)
                reduce { copy(progress = progress + PROGRESS_STEP) }
            }
            delay(TICK_MILLIS)
            val project = currentState.project
            myVideoRepository.addCompletedVideo(
                VideoSummary(
                    name = project.name,
                    duration = project.totalDuration,
                    thumbnailHue = project.clips.firstOrNull()?.thumbnailHue ?: 300,
                )
            )
            reduce { copy(phase = ExportPhase.DONE, progress = 100) }
        }
    }

    private companion object {
        /** Update interval of the simulated export. */
        const val TICK_MILLIS = 50L

        /** Progress step in percent. */
        const val PROGRESS_STEP = 2
    }
}
