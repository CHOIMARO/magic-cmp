package com.company.feature.editor.editor

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.MusicTrack
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.Vibe
import com.company.core.domain.model.editor.addText
import com.company.core.domain.model.editor.applyVibe
import com.company.core.domain.model.editor.duplicateClip
import com.company.core.domain.model.editor.moveText
import com.company.core.domain.model.editor.removeClip
import com.company.core.domain.model.editor.removeText
import com.company.core.domain.model.editor.setClipSpeed
import com.company.core.domain.model.editor.splitAt
import com.company.core.domain.model.editor.trimClipEnd
import com.company.core.domain.model.editor.trimClipStart
import com.company.core.domain.model.editor.updateText
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.EditorSettingsRepository
import com.company.core.domain.repository.MediaLibraryRepository
import com.company.core.domain.usecase.editor.GenerateCaptionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/** Bottom sheet of the editor. */
enum class EditorSheet {
    /** Vibe presets ("매직"). */
    VIBE,

    /** Screen layouts. */
    LAYOUT,

    /** Text input and text styles. */
    TEXT,

    /** Background music. */
    MUSIC,

    /** Color filters. */
    FILTER,

    /** Play speed of the selected clip. */
    SPEED,

    /** Automatic captions and caption styles. */
    CAPTIONS,
}

/** Edge of a clip that the user drags. */
enum class TrimEdge {
    /** Start of the clip. */
    START,

    /** End of the clip. */
    END,
}

/**
 * Text that the user types in the text sheet.
 *
 * @property id ID of the overlay to change, or null for a new overlay.
 * @property text Current input.
 * @property style Selected style.
 */
data class TextDraft(val id: String?, val text: String, val style: TextStyleType)

/**
 * State of the editor screen.
 *
 * @property project Project that the user edits.
 * @property canUndo True when the undo button can restore an earlier state.
 * @property time Position of the playhead in seconds.
 * @property playing True while the preview plays.
 * @property selectedClipId ID of the selected clip, or null.
 * @property sheet Open bottom sheet, or null.
 * @property textDraft Input of the text sheet, or null.
 * @property captionProgress Progress of the caption generation from 0 to 100, or null when it does not run.
 * @property musicTracks Music tracks that the user can pick.
 * @property watermark True to show the "magic" watermark on the stage.
 */
data class EditorState(
    val project: EditorProject = EditorProject(name = ""),
    val canUndo: Boolean = false,
    val time: Double = 0.0,
    val playing: Boolean = false,
    val selectedClipId: String? = null,
    val sheet: EditorSheet? = null,
    val textDraft: TextDraft? = null,
    val captionProgress: Int? = null,
    val musicTracks: List<MusicTrack> = emptyList(),
    val watermark: Boolean = false,
) : State() {
    /** Length of the project in seconds. */
    val totalDuration: Double
        get() = project.totalDuration

    /** Selected clip, or null. */
    val selectedClip: Clip?
        get() = project.clips.find { it.id == selectedClipId }
}

/** User actions on the editor screen. */
sealed class EditorIntent : Intent() {
    // ----- Playback -----

    /** TogglePlay : Starts or pauses the preview. */
    data object TogglePlay : EditorIntent()

    /** Pause : Pauses the preview. */
    data object Pause : EditorIntent()

    /** Scrub(time) : Moves the playhead. Ignored while the preview plays. */
    data class Scrub(val time: Double) : EditorIntent()

    // ----- Clips -----

    /** SelectClip(clipId) : Selects the clip, or clears the selection if the clip is selected. */
    data class SelectClip(val clipId: String) : EditorIntent()

    /** ClearSelection : Clears the clip selection. */
    data object ClearSelection : EditorIntent()

    /** StartTrim(clipId) : Starts a trim drag and keeps an undo step. */
    data class StartTrim(val clipId: String) : EditorIntent()

    /** Trim(clipId, edge, delta) : Moves a clip edge by [delta] timeline seconds from the drag start. */
    data class Trim(val clipId: String, val edge: TrimEdge, val delta: Double) : EditorIntent()

    /** SplitClip : Splits the clip at the playhead. */
    data object SplitClip : EditorIntent()

    /** DuplicateClip : Puts a copy of the selected clip after it. */
    data object DuplicateClip : EditorIntent()

    /** RemoveClip : Removes the selected clip. */
    data object RemoveClip : EditorIntent()

    /** AddClips : Opens the video picker to add clips. */
    data object AddClips : EditorIntent()

    // ----- Texts -----

    /** OpenTextEditor(textId) : Opens the text sheet for an overlay, or for a new overlay if null. */
    data class OpenTextEditor(val textId: String?) : EditorIntent()

    /** StartTextDrag : Keeps an undo step before the first text move. */
    data object StartTextDrag : EditorIntent()

    /** MoveText(textId, x, y) : Moves a text overlay to a percent position. */
    data class MoveText(val textId: String, val x: Double, val y: Double) : EditorIntent()

    /** ChangeDraftText(text) : Changes the input of the text sheet. */
    data class ChangeDraftText(val text: String) : EditorIntent()

    /** ChangeDraftStyle(style) : Changes the style in the text sheet. */
    data class ChangeDraftStyle(val style: TextStyleType) : EditorIntent()

    /** SaveText : Adds or changes the overlay from the text sheet. */
    data object SaveText : EditorIntent()

    /** DeleteText : Removes the overlay of the text sheet. */
    data object DeleteText : EditorIntent()

    // ----- Sheets -----

    /** OpenSheet(sheet) : Opens a tool sheet and pauses the preview. */
    data class OpenSheet(val sheet: EditorSheet) : EditorIntent()

    /** CloseSheet : Closes the sheet and stops the caption generation. */
    data object CloseSheet : EditorIntent()

    /** PickVibe(vibe) : Applies the vibe preset. */
    data class PickVibe(val vibe: Vibe) : EditorIntent()

    /** PickLayout(layout) : Applies the layout. */
    data class PickLayout(val layout: LayoutType) : EditorIntent()

    /** PickMusic(musicId) : Sets the music. Null removes the music. */
    data class PickMusic(val musicId: String?) : EditorIntent()

    /** PickFilter(filter) : Applies the filter. */
    data class PickFilter(val filter: FilterType) : EditorIntent()

    /** PickSpeed(speed) : Sets the play speed of the selected clip. */
    data class PickSpeed(val speed: Double) : EditorIntent()

    /** PickCaptionStyle(style) : Applies the caption style. */
    data class PickCaptionStyle(val style: CaptionStyleType) : EditorIntent()

    /** GenerateCaptions : Makes captions for the whole project. */
    data object GenerateCaptions : EditorIntent()

    /** RemoveCaptions : Removes all captions. */
    data object RemoveCaptions : EditorIntent()

    // ----- Project -----

    /** Undo : Restores the last undo step. */
    data object Undo : EditorIntent()

    /** Export : Opens the export screen. */
    data object Export : EditorIntent()

    /** Back : Leaves the editor. */
    data object Back : EditorIntent()
}

/** One-time events of the editor screen. */
sealed class EditorSideEffect : SideEffect() {
    /** ShowToast(message) : Shows a short message. */
    data class ShowToast(val message: String) : EditorSideEffect()

    /** OpenGalleryToAppend : Navigates to the video picker in append mode. */
    data object OpenGalleryToAppend : EditorSideEffect()

    /** OpenExport : Navigates to the export screen. */
    data object OpenExport : EditorSideEffect()

    /** Close : Leaves the editor. */
    data object Close : EditorSideEffect()
}

/**
 * ViewModel of the editor screen.
 *
 * The project and the undo history live in [EditorProjectRepository], so the picker
 * and the export screen see the same project. This ViewModel keeps only the screen state.
 */
class EditorViewModel(
    private val projectRepository: EditorProjectRepository,
    settingsRepository: EditorSettingsRepository,
    mediaLibraryRepository: MediaLibraryRepository,
    private val generateCaptions: GenerateCaptionsUseCase,
) : BaseViewModel<EditorState, EditorIntent, EditorSideEffect>(
    initialState = EditorState(
        project = projectRepository.project.value,
        canUndo = projectRepository.canUndo.value,
        musicTracks = mediaLibraryRepository.getMusicTracks(),
    )
) {
    private var playJob: Job? = null
    private var captionJob: Job? = null

    /** Clip range when the current trim drag started. */
    private var trimOrigin: Pair<Double, Double>? = null

    init {
        viewModelScope.launch {
            projectRepository.project.collect { project ->
                reduce { copy(project = project, time = time.coerceIn(0.0, project.totalDuration)) }
            }
        }
        viewModelScope.launch { projectRepository.canUndo.collect { reduce { copy(canUndo = it) } } }
        viewModelScope.launch { settingsRepository.settings.collect { reduce { copy(watermark = it.watermark) } } }
    }

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: EditorIntent) {
        when (intent) {
            EditorIntent.TogglePlay -> if (currentState.playing) pause() else play()
            EditorIntent.Pause -> pause()
            is EditorIntent.Scrub -> if (!currentState.playing) {
                reduce { copy(time = intent.time.coerceIn(0.0, totalDuration)) }
            }

            is EditorIntent.SelectClip -> {
                pause()
                reduce { copy(selectedClipId = if (selectedClipId == intent.clipId) null else intent.clipId) }
            }
            EditorIntent.ClearSelection -> reduce { copy(selectedClipId = null) }
            is EditorIntent.StartTrim -> startTrim(intent.clipId)
            is EditorIntent.Trim -> trim(intent.clipId, intent.edge, intent.delta)
            EditorIntent.SplitClip -> splitClip()
            EditorIntent.DuplicateClip -> duplicateClip()
            EditorIntent.RemoveClip -> removeClip()
            EditorIntent.AddClips -> {
                pause()
                postSideEffect { EditorSideEffect.OpenGalleryToAppend }
            }

            is EditorIntent.OpenTextEditor -> openTextEditor(intent.textId)
            EditorIntent.StartTextDrag -> {
                pause()
                projectRepository.saveCheckpoint()
            }
            is EditorIntent.MoveText -> projectRepository.update { it.moveText(intent.textId, intent.x, intent.y) }
            is EditorIntent.ChangeDraftText -> reduce { copy(textDraft = textDraft?.copy(text = intent.text)) }
            is EditorIntent.ChangeDraftStyle -> reduce { copy(textDraft = textDraft?.copy(style = intent.style)) }
            EditorIntent.SaveText -> saveText()
            EditorIntent.DeleteText -> deleteText()

            is EditorIntent.OpenSheet -> {
                if (intent.sheet == EditorSheet.TEXT) {
                    openTextEditor(textId = null)
                } else {
                    pause()
                    reduce { copy(sheet = intent.sheet) }
                }
            }
            EditorIntent.CloseSheet -> closeSheet()
            is EditorIntent.PickVibe -> if (currentState.project.vibe != intent.vibe) {
                projectRepository.commit { it.applyVibe(intent.vibe) }
            }
            is EditorIntent.PickLayout -> pickLayout(intent.layout)
            is EditorIntent.PickMusic -> if (currentState.project.musicId != intent.musicId) {
                projectRepository.commit { it.copy(musicId = intent.musicId) }
            }
            is EditorIntent.PickFilter -> if (currentState.project.filter != intent.filter) {
                projectRepository.commit { it.copy(filter = intent.filter) }
            }
            is EditorIntent.PickSpeed -> currentState.selectedClip?.let { clip ->
                projectRepository.commit { it.setClipSpeed(clip.id, intent.speed) }
            }
            is EditorIntent.PickCaptionStyle -> if (currentState.project.captionStyle != intent.style) {
                projectRepository.commit { it.copy(captionStyle = intent.style) }
            }
            EditorIntent.GenerateCaptions -> startCaptionGeneration()
            EditorIntent.RemoveCaptions -> {
                projectRepository.commit { it.copy(captions = emptyList()) }
                reduce { copy(sheet = null) }
            }

            EditorIntent.Undo -> undo()
            EditorIntent.Export -> {
                pause()
                postSideEffect { EditorSideEffect.OpenExport }
            }
            EditorIntent.Back -> {
                pause()
                postSideEffect { EditorSideEffect.Close }
            }
        }
    }

    // =====================================================================
    // Playback
    // =====================================================================

    private fun play() {
        val total = currentState.totalDuration
        if (total <= 0.0) return
        val start = if (currentState.time >= total - 0.05) 0.0 else currentState.time
        reduce { copy(playing = true, selectedClipId = null, time = start) }

        playJob?.cancel()
        playJob = viewModelScope.launch {
            var mark = TimeSource.Monotonic.markNow()
            while (isActive) {
                delay(FRAME_MILLIS)
                val elapsed = mark.elapsedNow().inWholeMicroseconds / 1_000_000.0
                mark = TimeSource.Monotonic.markNow()
                val next = currentState.time + elapsed
                if (next >= currentState.totalDuration) {
                    reduce { copy(time = totalDuration, playing = false) }
                    break
                }
                reduce { copy(time = next) }
            }
        }
    }

    private fun pause() {
        playJob?.cancel()
        playJob = null
        if (currentState.playing) reduce { copy(playing = false) }
    }

    // =====================================================================
    // Clip editing
    // =====================================================================

    private fun startTrim(clipId: String) {
        val clip = currentState.project.clips.find { it.id == clipId } ?: return
        projectRepository.saveCheckpoint()
        trimOrigin = clip.inPoint to clip.outPoint
    }

    /** Converts the timeline delta to source time with the clip speed, then trims from the drag start. */
    private fun trim(clipId: String, edge: TrimEdge, delta: Double) {
        val origin = trimOrigin ?: return
        val clip = currentState.project.clips.find { it.id == clipId } ?: return
        val sourceDelta = delta * clip.speed
        projectRepository.update { project ->
            when (edge) {
                TrimEdge.START -> project.trimClipStart(clipId, origin.first + sourceDelta)
                TrimEdge.END -> project.trimClipEnd(clipId, origin.second + sourceDelta)
            }
        }
    }

    private fun splitClip() {
        val split = currentState.project.splitAt(currentState.time)
        if (split == null) {
            toast("흰 선을 클립 안쪽으로 옮겨주세요")
            return
        }
        projectRepository.commit { split }
        reduce { copy(selectedClipId = null) }
        toast("흰 선 위치에서 나눴어요")
    }

    private fun duplicateClip() {
        val clipId = currentState.selectedClipId ?: return
        val (project, copyId) = currentState.project.duplicateClip(clipId) ?: return
        projectRepository.commit { project }
        reduce { copy(selectedClipId = copyId) }
        toast("복제했어요")
    }

    private fun removeClip() {
        val clipId = currentState.selectedClipId ?: return
        projectRepository.commit { it.removeClip(clipId) }
        reduce { copy(selectedClipId = null, time = 0.0) }
        toast("삭제했어요 · 되돌리기 가능")
    }

    private fun pickLayout(layout: LayoutType) {
        val project = currentState.project
        if (project.layout == layout) return
        projectRepository.commit { it.copy(layout = layout) }
        if (layout.clipCount > project.clips.size) toast("클립을 ${layout.clipCount}개 넣으면 더 잘 어울려요")
    }

    private fun undo() {
        if (!projectRepository.undo()) return
        pause()
        reduce { copy(selectedClipId = null) }
        toast("되돌렸어요")
    }

    // =====================================================================
    // Texts
    // =====================================================================

    private fun openTextEditor(textId: String?) {
        pause()
        val overlay = currentState.project.texts.find { it.id == textId }
        val draft = overlay?.let { TextDraft(id = it.id, text = it.text, style = it.style) }
            ?: TextDraft(id = null, text = "", style = TextStyleType.BOLD)
        reduce { copy(sheet = EditorSheet.TEXT, textDraft = draft) }
    }

    private fun saveText() {
        val draft = currentState.textDraft ?: return
        val text = draft.text.trim().ifEmpty { "글자" }
        val time = currentState.time
        projectRepository.commit { project ->
            if (draft.id != null) project.updateText(draft.id, text, draft.style) else project.addText(text, draft.style, time)
        }
        reduce { copy(sheet = null, textDraft = null) }
    }

    private fun deleteText() {
        val textId = currentState.textDraft?.id ?: return
        projectRepository.commit { it.removeText(textId) }
        reduce { copy(sheet = null, textDraft = null) }
    }

    // =====================================================================
    // Captions and sheets
    // =====================================================================

    /** Shows a progress bar first. The prototype has no real speech recognition. */
    private fun startCaptionGeneration() {
        captionJob?.cancel()
        reduce { copy(captionProgress = 0) }
        captionJob = viewModelScope.launch {
            while ((currentState.captionProgress ?: 0) < 100 - CAPTION_STEP) {
                delay(CAPTION_TICK_MILLIS)
                reduce { copy(captionProgress = (captionProgress ?: 0) + CAPTION_STEP) }
            }
            delay(CAPTION_TICK_MILLIS)
            val captions = generateCaptions(currentState.totalDuration)
            projectRepository.commit { it.copy(captions = captions) }
            reduce { copy(captionProgress = null, sheet = null) }
            toast("자막을 넣었어요")
        }
    }

    private fun closeSheet() {
        captionJob?.cancel()
        reduce { copy(sheet = null, textDraft = null, captionProgress = null) }
    }

    private fun toast(message: String) {
        postSideEffect { EditorSideEffect.ShowToast(message) }
    }

    private companion object {
        /** Update interval of the preview playback. */
        const val FRAME_MILLIS = 16L

        /** Update interval of the caption progress. */
        const val CAPTION_TICK_MILLIS = 45L

        /** Progress step of the caption generation in percent. */
        const val CAPTION_STEP = 4
    }
}
