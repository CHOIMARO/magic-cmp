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
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.addText
import com.company.core.domain.model.editor.compactTextLayers
import com.company.core.domain.model.editor.duplicateClip
import com.company.core.domain.model.editor.duplicateText
import com.company.core.domain.model.editor.moveText
import com.company.core.domain.model.editor.moveTextTime
import com.company.core.domain.model.editor.overlapsOtherText
import com.company.core.domain.model.editor.removeClip
import com.company.core.domain.model.editor.removeText
import com.company.core.domain.model.editor.setClipSpeed
import com.company.core.domain.model.editor.splitAt
import com.company.core.domain.model.editor.trimClipEnd
import com.company.core.domain.model.editor.trimClipStart
import com.company.core.domain.model.editor.trimTextEnd
import com.company.core.domain.model.editor.trimTextStart
import com.company.core.domain.model.editor.updateText
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.EditorSettingsRepository
import com.company.core.domain.repository.MediaLibraryRepository
import com.company.core.domain.usecase.editor.GenerateCaptionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Bottom sheet of the editor. */
enum class EditorSheet {
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

/** Edge of a clip or a text overlay that the user drags. */
enum class TrimEdge {
    /** Start of the item. */
    START,

    /** End of the item. */
    END,
}

/** Part of the timeline that the user drags. */
sealed interface TimelineDragTarget {
    /** ClipEdge(clipId, edge) : Trims one edge of a clip. The stage shows the frame at the edge. */
    data class ClipEdge(val clipId: String, val edge: TrimEdge) : TimelineDragTarget

    /** TextEdge(textId, edge) : Changes when a text overlay starts or ends. */
    data class TextEdge(val textId: String, val edge: TrimEdge) : TimelineDragTarget

    /** TextMove(textId) : Moves a text overlay in time and to another row. Its length stays. */
    data class TextMove(val textId: String) : TimelineDragTarget
}

/**
 * Text that the user types in the text sheet.
 *
 * @property id ID of the overlay to change, or null for a new overlay.
 * @property text Current input.
 * @property style Selected style.
 * @property font Selected font, or null for the font of [style].
 */
data class TextDraft(val id: String?, val text: String, val style: TextStyleType, val font: TextFontType? = null)

/**
 * State of the editor screen.
 *
 * @property project Project that the user edits.
 * @property canUndo True when the undo button can restore an earlier state.
 * @property time Position of the playhead in seconds.
 * @property playing True while the preview plays.
 * @property selectedClipId ID of the selected clip, or null. Only one clip or one text overlay is selected at a time.
 * @property selectedTextId ID of the text overlay selected on the timeline, or null.
 * @property activeDrag Part of the timeline that the user drags now, or null.
 * @property dragShift Timeline seconds that the timeline moves to the right during a timeline drag.
 *   The playhead cannot go before 0, so this shift moves the timeline for the rest. It is 0 at other times.
 * @property dragBlocked True while the moved text overlaps another text in its row. The text cannot stay there,
 *   so it goes back when the drag ends.
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
    val selectedTextId: String? = null,
    val activeDrag: TimelineDragTarget? = null,
    val dragShift: Double = 0.0,
    val dragBlocked: Boolean = false,
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

    /** Text overlay selected on the timeline, or null. */
    val selectedText: TextOverlay?
        get() = project.texts.find { it.id == selectedTextId }
}

/** User actions on the editor screen. */
sealed class EditorIntent : Intent() {
    // ----- Playback -----

    /** TogglePlay : Starts or pauses the preview. */
    data object TogglePlay : EditorIntent()

    /** Pause : Pauses the preview. */
    data object Pause : EditorIntent()

    /** Scrub(time) : Moves the playhead. Ignored while the preview plays or while a timeline drag runs. */
    data class Scrub(val time: Double) : EditorIntent()

    /** PlaybackProgress(time) : Moves the playhead to the position of the playing preview. Ignored when the preview does not play. */
    data class PlaybackProgress(val time: Double) : EditorIntent()

    /** PlaybackEnded : Stops the preview at the end of the project. */
    data object PlaybackEnded : EditorIntent()

    // ----- Clips -----

    /** SelectClip(clipId) : Selects the clip, or clears the selection if the clip is selected. */
    data class SelectClip(val clipId: String) : EditorIntent()

    /** ClearSelection : Clears the clip and text selection. */
    data object ClearSelection : EditorIntent()

    /**
     * StartTimelineDrag(target, snapSeconds) : Starts a drag of a clip edge, a text edge, or a text overlay on the
     * timeline, and keeps an undo step. A text drag also selects the text. A dragged edge snaps to the playhead, and
     * a text start also snaps to the start or the end of another text, within [snapSeconds].
     */
    data class StartTimelineDrag(val target: TimelineDragTarget, val snapSeconds: Double) : EditorIntent()

    /**
     * MoveTimelineDrag(delta, rowDelta) : The finger moved [delta] timeline seconds and [rowDelta] text rows from the
     * drag start. A positive [rowDelta] is downward. Only a text move uses [rowDelta]. The dragged part follows the
     * finger. During a clip trim, the other edge of the clip stays in place on the screen.
     */
    data class MoveTimelineDrag(val delta: Double, val rowDelta: Int = 0) : EditorIntent()

    /**
     * ScrollTimelineDrag(seconds) : Scrolls the timeline by [seconds] during a timeline drag, because the finger is at
     * the edge of the timeline. The dragged part moves with the timeline. A negative value scrolls toward the project start.
     */
    data class ScrollTimelineDrag(val seconds: Double) : EditorIntent()

    /**
     * EndTimelineDrag : Ends the timeline drag. The timeline moves back from [EditorState.dragShift].
     * A moved text that overlaps another text goes back to its place before the drag.
     */
    data object EndTimelineDrag : EditorIntent()

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

    /** SelectText(textId) : Selects the text overlay on the timeline, or clears the selection if it is selected. */
    data class SelectText(val textId: String) : EditorIntent()

    /** DuplicateText : Puts a copy of the selected text overlay right after it in time. */
    data object DuplicateText : EditorIntent()

    /** RemoveText : Removes the selected text overlay. */
    data object RemoveText : EditorIntent()

    /** StartTextDrag : Keeps an undo step before the first text move. */
    data object StartTextDrag : EditorIntent()

    /** MoveText(textId, x, y) : Moves a text overlay to a percent position. */
    data class MoveText(val textId: String, val x: Double, val y: Double) : EditorIntent()

    /** ChangeDraftText(text) : Changes the input of the text sheet. */
    data class ChangeDraftText(val text: String) : EditorIntent()

    /** ChangeDraftStyle(style) : Changes the style in the text sheet. */
    data class ChangeDraftStyle(val style: TextStyleType) : EditorIntent()

    /** ChangeDraftFont(font) : Changes the font in the text sheet. */
    data class ChangeDraftFont(val font: TextFontType) : EditorIntent()

    /** SaveText : Adds or changes the overlay from the text sheet. */
    data object SaveText : EditorIntent()

    /** DeleteText : Removes the overlay of the text sheet. */
    data object DeleteText : EditorIntent()

    // ----- Sheets -----

    /** OpenSheet(sheet) : Opens a tool sheet and pauses the preview. */
    data class OpenSheet(val sheet: EditorSheet) : EditorIntent()

    /** CloseSheet : Closes the sheet and stops the caption generation. */
    data object CloseSheet : EditorIntent()

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

    /** Snapped : The dragged part snapped to the playhead or to another text. The screen gives a short vibration. */
    data object Snapped : EditorSideEffect()
}

/**
 * ViewModel of the editor screen.
 *
 * The project and the undo history live in [EditorProjectRepository], so the picker
 * and the export screen see the same project. This ViewModel keeps only the screen state.
 *
 * The preview player of the screen moves the playhead. It sends [EditorIntent.PlaybackProgress]
 * while it plays and [EditorIntent.PlaybackEnded] at the end.
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
    private var captionJob: Job? = null

    /** Dragged item when the current timeline drag started. */
    private var dragOrigin: DragOrigin? = null

    /** Playhead position when the current timeline drag started. */
    private var dragStartTime = 0.0

    /** Finger movement of the current timeline drag, in timeline seconds. */
    private var dragFingerDelta = 0.0

    /** Timeline seconds that the edge scrolling moved during the current timeline drag. */
    private var dragEdgeScroll = 0.0

    /** Text rows that the finger moved during the current timeline drag. A positive value is downward. */
    private var dragRowDelta = 0

    /** Largest distance in seconds that snaps during the current timeline drag. */
    private var dragSnapSeconds = 0.0

    /** True while the dragged part is snapped. A new snap gives [EditorSideEffect.Snapped]. */
    private var dragSnapped = false

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
            is EditorIntent.Scrub -> if (!currentState.playing && currentState.activeDrag == null) {
                reduce { copy(time = intent.time.coerceIn(0.0, totalDuration)) }
            }
            is EditorIntent.PlaybackProgress -> if (currentState.playing) {
                reduce { copy(time = intent.time.coerceIn(0.0, totalDuration)) }
            }
            EditorIntent.PlaybackEnded -> reduce { copy(time = totalDuration, playing = false) }

            is EditorIntent.SelectClip -> {
                pause()
                reduce { copy(selectedClipId = if (selectedClipId == intent.clipId) null else intent.clipId, selectedTextId = null) }
            }
            EditorIntent.ClearSelection -> reduce { copy(selectedClipId = null, selectedTextId = null) }
            is EditorIntent.StartTimelineDrag -> startDrag(intent.target, intent.snapSeconds)
            is EditorIntent.MoveTimelineDrag -> {
                dragFingerDelta = intent.delta
                dragRowDelta = intent.rowDelta
                applyDrag()
            }
            is EditorIntent.ScrollTimelineDrag -> if (currentState.activeDrag != null) {
                dragEdgeScroll += intent.seconds
                applyDrag()
            }
            EditorIntent.EndTimelineDrag -> endDrag()
            EditorIntent.SplitClip -> splitClip()
            EditorIntent.DuplicateClip -> duplicateClip()
            EditorIntent.RemoveClip -> removeClip()
            EditorIntent.AddClips -> {
                pause()
                postSideEffect { EditorSideEffect.OpenGalleryToAppend }
            }

            is EditorIntent.OpenTextEditor -> openTextEditor(intent.textId)
            is EditorIntent.SelectText -> {
                pause()
                reduce { copy(selectedTextId = if (selectedTextId == intent.textId) null else intent.textId, selectedClipId = null) }
            }
            EditorIntent.DuplicateText -> duplicateText()
            EditorIntent.RemoveText -> removeText()
            EditorIntent.StartTextDrag -> {
                pause()
                projectRepository.saveCheckpoint()
            }
            is EditorIntent.MoveText -> projectRepository.update { it.moveText(intent.textId, intent.x, intent.y) }
            is EditorIntent.ChangeDraftText -> reduce { copy(textDraft = textDraft?.copy(text = intent.text)) }
            is EditorIntent.ChangeDraftStyle -> reduce { copy(textDraft = textDraft?.copy(style = intent.style)) }
            is EditorIntent.ChangeDraftFont -> reduce { copy(textDraft = textDraft?.copy(font = intent.font)) }
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

    /** Starts the preview. At the end of the project, the preview starts again from the start. */
    private fun play() {
        val total = currentState.totalDuration
        if (total <= 0.0) return
        val start = if (currentState.time >= total - END_MARGIN) 0.0 else currentState.time
        reduce { copy(playing = true, selectedClipId = null, selectedTextId = null, time = start) }
    }

    private fun pause() {
        if (currentState.playing) reduce { copy(playing = false) }
    }

    // =====================================================================
    // Timeline drag
    // =====================================================================

    private fun startDrag(target: TimelineDragTarget, snapSeconds: Double) {
        val project = currentState.project
        val origin = when (target) {
            is TimelineDragTarget.ClipEdge -> project.clips.find { it.id == target.clipId }
                ?.let { DragOrigin(start = it.inPoint, end = it.outPoint, layer = 0) }
            is TimelineDragTarget.TextEdge -> project.texts.find { it.id == target.textId }
                ?.let { DragOrigin(start = it.start, end = it.end, layer = it.layer) }
            is TimelineDragTarget.TextMove -> project.texts.find { it.id == target.textId }
                ?.let { DragOrigin(start = it.start, end = it.end, layer = it.layer) }
        } ?: return
        pause()
        projectRepository.saveCheckpoint()
        dragOrigin = origin
        dragStartTime = currentState.time
        dragFingerDelta = 0.0
        dragEdgeScroll = 0.0
        dragRowDelta = 0
        dragSnapSeconds = snapSeconds
        // 이미 붙어 있는 곳에서 시작하면 처음 움직일 때 다시 진동하지 않는다.
        dragSnapped = snapOffset(project.snapEdges(target, origin, delta = 0.0), snapSeconds) != null
        reduce {
            when (target) {
                is TimelineDragTarget.ClipEdge -> copy(activeDrag = target, dragBlocked = false)
                is TimelineDragTarget.TextEdge ->
                    copy(activeDrag = target, dragBlocked = false, selectedTextId = target.textId, selectedClipId = null)
                is TimelineDragTarget.TextMove ->
                    copy(activeDrag = target, dragBlocked = false, selectedTextId = target.textId, selectedClipId = null)
            }
        }
    }

    /**
     * Moves the dragged part by the finger movement plus the edge scrolling, then moves the playhead.
     *
     * The dragged edge snaps to the playhead, and a text start also snaps to the start or the end of another text.
     * The edge scrolling moves the playhead, so the dragged part stays under the finger. The clips are laid out from
     * the project start, so a clip start trim moves the clip end on the screen. Thus a clip start trim also moves the
     * playhead by the trimmed length, and the clip end stays in place. Near the project start, [EditorState.dragShift]
     * moves the timeline for the part that the playhead cannot move.
     */
    private fun applyDrag() {
        val target = currentState.activeDrag ?: return
        val origin = dragOrigin ?: return
        val before = projectRepository.project.value
        var requested = dragFingerDelta + dragEdgeScroll
        val snap = snapOffset(before.snapEdges(target, origin, requested), dragSnapSeconds)
        if (snap != null) {
            requested += snap
            if (!dragSnapped) postSideEffect { EditorSideEffect.Snapped }
        }
        dragSnapped = snap != null
        val layer = if (target is TimelineDragTarget.TextMove) before.dragLayer(target.textId, origin) else null
        projectRepository.update { project -> project.dragTo(target, origin, requested, layer) }
        val project = projectRepository.project.value
        // 도메인은 클립과 글자를 허용 범위 안에 둔다. 실제로 움직인 길이를 기준으로 계산한다.
        val moved = project.draggedDistance(target, origin) ?: return
        // 원본 끝, 최소 길이, 프로젝트 끝에 닿으면 가장자리 스크롤도 더 가지 않는다.
        // 붙기로 더해진 거리는 손가락 이동으로 본다.
        val scrollLimit = moved - dragFingerDelta - (snap ?: 0.0)
        dragEdgeScroll = when {
            dragEdgeScroll > 0 -> dragEdgeScroll.coerceAtMost(scrollLimit).coerceAtLeast(0.0)
            dragEdgeScroll < 0 -> dragEdgeScroll.coerceAtLeast(scrollLimit).coerceAtMost(0.0)
            else -> 0.0
        }
        val keepsClipEnd = target is TimelineDragTarget.ClipEdge && target.edge == TrimEdge.START
        val time = dragStartTime + dragEdgeScroll - if (keepsClipEnd) moved else 0.0
        val blocked = target is TimelineDragTarget.TextMove && project.overlapsOtherText(target.textId)
        // 재생 위치와 프로젝트를 한 번에 바꿔야 화면에서 클립 끝이 흔들리지 않는다.
        reduce {
            copy(
                project = project,
                time = time.coerceIn(0.0, project.totalDuration),
                dragShift = (-time).coerceAtLeast(0.0),
                dragBlocked = blocked,
            )
        }
    }

    private fun endDrag() {
        val target = currentState.activeDrag
        val blocked = currentState.dragBlocked
        dragOrigin = null
        dragSnapped = false
        if (target is TimelineDragTarget.TextMove) {
            if (blocked) {
                // 다른 글자와 겹치는 곳에는 놓지 않는다. 끌기를 시작할 때 저장한 단계로 되돌린다.
                projectRepository.undo()
            } else {
                projectRepository.update { it.compactTextLayers() }
            }
        }
        reduce { copy(project = projectRepository.project.value, activeDrag = null, dragShift = 0.0, dragBlocked = false) }
    }

    /**
     * Edges of the dragged part and the times that they snap to. See [timelineSnapEdges].
     *
     * @param target Dragged part.
     * @param origin Dragged item when the drag started.
     * @param delta Requested movement in timeline seconds.
     */
    private fun EditorProject.snapEdges(target: TimelineDragTarget, origin: DragOrigin, delta: Double): List<SnapEdge> =
        // 가장자리 스크롤만큼 재생 위치가 움직이므로, 재생선이 가리키는 시간은 시작 시간에 스크롤을 더한 값이다.
        timelineSnapEdges(target, origin.start, origin.end, delta, playhead = dragStartTime + dragEdgeScroll)

    /** Row that the moved text goes to. It can be one row after the last row of the other texts, which is a new row. */
    private fun EditorProject.dragLayer(textId: String, origin: DragOrigin): Int {
        val lastOtherLayer = texts.filter { it.id != textId }.maxOfOrNull { it.layer } ?: -1
        return (origin.layer + dragRowDelta).coerceIn(0, lastOtherLayer + 1)
    }

    /**
     * Moves the dragged part [delta] timeline seconds from [origin].
     *
     * @param target Dragged part.
     * @param origin Dragged item when the drag started.
     * @param delta Requested movement in timeline seconds.
     * @param layer New row of a moved text, or null to keep the row.
     */
    private fun EditorProject.dragTo(target: TimelineDragTarget, origin: DragOrigin, delta: Double, layer: Int?): EditorProject =
        when (target) {
            is TimelineDragTarget.ClipEdge -> {
                // 클립 가장자리는 원본 시간으로 움직이므로 재생 속도를 곱한다.
                val speed = clips.find { it.id == target.clipId }?.speed ?: 1.0
                when (target.edge) {
                    TrimEdge.START -> trimClipStart(target.clipId, origin.start + delta * speed)
                    TrimEdge.END -> trimClipEnd(target.clipId, origin.end + delta * speed)
                }
            }
            is TimelineDragTarget.TextEdge -> when (target.edge) {
                TrimEdge.START -> trimTextStart(target.textId, origin.start + delta)
                TrimEdge.END -> trimTextEnd(target.textId, origin.end + delta)
            }
            is TimelineDragTarget.TextMove -> moveTextTime(target.textId, origin.start + delta, layer)
        }

    /**
     * Timeline seconds that the dragged part moved from [origin].
     *
     * @return The distance, or null when the dragged item does not exist.
     */
    private fun EditorProject.draggedDistance(target: TimelineDragTarget, origin: DragOrigin): Double? =
        when (target) {
            is TimelineDragTarget.ClipEdge -> clips.find { it.id == target.clipId }?.let { clip ->
                when (target.edge) {
                    TrimEdge.START -> (clip.inPoint - origin.start) / clip.speed
                    TrimEdge.END -> (clip.outPoint - origin.end) / clip.speed
                }
            }
            is TimelineDragTarget.TextEdge -> texts.find { it.id == target.textId }?.let { text ->
                when (target.edge) {
                    TrimEdge.START -> text.start - origin.start
                    TrimEdge.END -> text.end - origin.end
                }
            }
            is TimelineDragTarget.TextMove -> texts.find { it.id == target.textId }?.let { it.start - origin.start }
        }

    /**
     * Dragged item when a timeline drag started.
     *
     * @property start Start of the item. For a clip, it is the source in-point. For a text, it is a timeline time.
     * @property end End of the item. For a clip, it is the source out-point. For a text, it is a timeline time.
     * @property layer Row of a text overlay. It is 0 for a clip.
     */
    private data class DragOrigin(val start: Double, val end: Double, val layer: Int)

    // =====================================================================
    // Clip editing
    // =====================================================================

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
        reduce { copy(selectedClipId = null, selectedTextId = null) }
        toast("되돌렸어요")
    }

    // =====================================================================
    // Texts
    // =====================================================================

    private fun openTextEditor(textId: String?) {
        pause()
        val overlay = currentState.project.texts.find { it.id == textId }
        val draft = overlay?.let { TextDraft(id = it.id, text = it.text, style = it.style, font = it.font) }
            ?: TextDraft(id = null, text = "", style = TextStyleType.BOLD)
        reduce { copy(sheet = EditorSheet.TEXT, textDraft = draft) }
    }

    private fun saveText() {
        val draft = currentState.textDraft ?: return
        val text = draft.text.trim().ifEmpty { "글자" }
        val time = currentState.time
        projectRepository.commit { project ->
            if (draft.id != null) {
                project.updateText(draft.id, text, draft.style, draft.font)
            } else {
                project.addText(text, draft.style, time, draft.font)
            }
        }
        reduce { copy(sheet = null, textDraft = null) }
    }

    private fun deleteText() {
        val textId = currentState.textDraft?.id ?: return
        projectRepository.commit { it.removeText(textId) }
        reduce { copy(sheet = null, textDraft = null, selectedTextId = selectedTextId.takeIf { it != textId }) }
        toast("삭제했어요 · 되돌리기 가능")
    }

    private fun duplicateText() {
        val textId = currentState.selectedTextId ?: return
        val (project, copyId) = currentState.project.duplicateText(textId) ?: return
        projectRepository.commit { project }
        reduce { copy(selectedTextId = copyId) }
        toast("복제했어요")
    }

    private fun removeText() {
        val textId = currentState.selectedTextId ?: return
        projectRepository.commit { it.removeText(textId) }
        reduce { copy(selectedTextId = null) }
        toast("삭제했어요 · 되돌리기 가능")
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
            val captions = generateCaptions(currentState.project.clips)
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
        /** A playhead this close to the end, in seconds, counts as the end. */
        const val END_MARGIN = 0.05

        /** Update interval of the caption progress. */
        const val CAPTION_TICK_MILLIS = 45L

        /** Progress step of the caption generation in percent. */
        const val CAPTION_STEP = 4
    }
}
