package com.company.feature.editor.export

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.ExportOverlay
import com.company.core.domain.model.editor.ExportRequest
import com.company.core.domain.model.editor.ExportStatus
import com.company.core.domain.model.editor.FrameArea
import com.company.core.domain.model.editor.GalleryAccessDeniedException
import com.company.core.domain.model.editor.VideoSummary
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.EditorSettingsRepository
import com.company.core.domain.repository.MyVideoRepository
import com.company.core.domain.repository.VideoExportRepository
import com.company.ui.editor.LayoutCell
import com.company.ui.editor.spec
import kotlin.coroutines.cancellation.CancellationException
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
 * @property width Width of the 9:16 output frame in pixels.
 * @property height Height of the 9:16 output frame in pixels.
 */
enum class ExportResolution(
    val label: String,
    val note: String,
    val megabytesPerSecond: Double,
    val width: Int,
    val height: Int,
) {
    /** Small file. */
    HD("720p", "가볍게", 0.5, 720, 1280),

    /** Recommended quality. */
    FULL_HD("1080p", "추천", 1.1, 1080, 1920),

    /** Best quality. */
    UHD("4K", "최고 화질", 4.2, 2160, 3840),
}

/**
 * Overlay images that the screen must draw before the encoder starts.
 *
 * Only the screen can draw them, because they use the same composables as the editor stage.
 *
 * @property project Project to draw.
 * @property width Width of the images in pixels.
 * @property height Height of the images in pixels.
 * @property watermark True to draw the watermark.
 */
data class OverlayRequest(val project: EditorProject, val width: Int, val height: Int, val watermark: Boolean)

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
 * @property overlayRequest Overlay images that the screen must draw now, or null.
 */
data class ExportState(
    val project: EditorProject = EditorProject(name = ""),
    val resolution: ExportResolution = ExportResolution.FULL_HD,
    val phase: ExportPhase = ExportPhase.SETUP,
    val progress: Int = 0,
    val showOptions: Boolean = false,
    val overlayRequest: OverlayRequest? = null,
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

    /** OverlaysProgress(fraction) : Shows how much of the overlay images are ready, from 0 to 1. */
    data class OverlaysProgress(val fraction: Float) : ExportIntent()

    /** OverlaysReady(overlays) : Starts the encoder with the overlay images that the screen drew. */
    class OverlaysReady(val overlays: List<ExportOverlay>) : ExportIntent()

    /** Cancel : Stops the export and goes back to the setup step. */
    data object Cancel : ExportIntent()

    /** Back : Stops the export and goes back to the editor. */
    data object Back : ExportIntent()

    /** GoHome : Goes to the home tab. */
    data object GoHome : ExportIntent()
}

/** One-time events of the export screen. */
sealed class ExportSideEffect : SideEffect() {
    /** ShowToast(message) : Shows a short message. */
    data class ShowToast(val message: String) : ExportSideEffect()

    /** Close : Goes back to the editor. */
    data object Close : ExportSideEffect()

    /** GoHome : Navigates to the home tab. */
    data object GoHome : ExportSideEffect()
}

/**
 * ViewModel of the export screen.
 *
 * The export has two steps. First the screen draws the overlay images (texts, captions, watermark,
 * and layout background). Then [VideoExportRepository] encodes the video and saves it to the gallery.
 * A project with sample clips has no source files, so a timer simulates its export.
 */
class ExportViewModel(
    projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
    private val videoExportRepository: VideoExportRepository,
    private val settingsRepository: EditorSettingsRepository,
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
            is ExportIntent.OverlaysProgress -> if (currentState.overlayRequest != null) {
                reduce { copy(progress = (intent.fraction * OVERLAY_PERCENT).roundToInt()) }
            }
            is ExportIntent.OverlaysReady -> if (currentState.overlayRequest != null) encode(intent.overlays)
            ExportIntent.Cancel -> {
                exportJob?.cancel()
                reduce { copy(phase = ExportPhase.SETUP, progress = 0, overlayRequest = null) }
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

    /** Starts the export. A project with source files first asks the screen for the overlay images. */
    private fun start() {
        exportJob?.cancel()
        val project = currentState.project
        if (project.clips.isEmpty()) return
        if (project.clips.any { it.sourceUri == null }) {
            simulateExport()
            return
        }
        val resolution = currentState.resolution
        reduce {
            copy(
                phase = ExportPhase.RUNNING,
                progress = 0,
                overlayRequest = OverlayRequest(
                    project = project,
                    width = resolution.width,
                    height = resolution.height,
                    watermark = settingsRepository.settings.value.watermark,
                ),
            )
        }
    }

    /**
     * Encodes the video with the overlay images and saves it to the gallery.
     *
     * @param overlays Overlay images that the screen drew.
     */
    private fun encode(overlays: List<ExportOverlay>) {
        val project = currentState.project
        val resolution = currentState.resolution
        val request = ExportRequest(
            name = project.name,
            clips = project.clips,
            filter = project.filter,
            width = resolution.width,
            height = resolution.height,
            videoArea = project.layout.spec.cells.first().toFrameArea(),
            overlays = overlays,
        )
        reduce { copy(overlayRequest = null, progress = OVERLAY_PERCENT) }
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            try {
                videoExportRepository.export(request).collect { status ->
                    when (status) {
                        is ExportStatus.Progress -> reduce {
                            copy(progress = OVERLAY_PERCENT + status.percent * (100 - OVERLAY_PERCENT) / 100)
                        }
                        is ExportStatus.Saved -> finish()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GalleryAccessDeniedException) {
                fail("사진 보관함에 저장하려면 접근을 허용해 주세요")
            } catch (e: Exception) {
                fail("저장하지 못했어요. 다시 시도해 주세요")
            }
        }
    }

    /** Simulates the export of a project with sample clips, as in the prototype. */
    private fun simulateExport() {
        reduce { copy(phase = ExportPhase.RUNNING, progress = 0) }
        exportJob = viewModelScope.launch {
            while (currentState.progress < 100 - PROGRESS_STEP) {
                delay(TICK_MILLIS)
                reduce { copy(progress = progress + PROGRESS_STEP) }
            }
            delay(TICK_MILLIS)
            finish()
        }
    }

    /** Adds the video to the completed videos and shows the done step. */
    private fun finish() {
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

    private fun fail(message: String) {
        reduce { copy(phase = ExportPhase.SETUP, progress = 0, overlayRequest = null) }
        postSideEffect { ExportSideEffect.ShowToast(message) }
    }

    private companion object {
        /** Part of the progress, in percent, for the overlay images. The encoder is the rest. */
        const val OVERLAY_PERCENT = 10

        /** Update interval of the simulated export. */
        const val TICK_MILLIS = 50L

        /** Progress step in percent. */
        const val PROGRESS_STEP = 2
    }
}

/** Converts a layout cell in percent to a frame area in fractions. */
private fun LayoutCell.toFrameArea(): FrameArea = FrameArea(left = left / 100f, top = top / 100f, width = width / 100f, height = height / 100f)
