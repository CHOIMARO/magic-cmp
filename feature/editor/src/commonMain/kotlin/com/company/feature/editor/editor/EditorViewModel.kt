package com.company.feature.editor.editor

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.MY_VOICE_ID
import com.company.domain.model.editor.MyVoiceStatus
import com.company.domain.model.editor.compactTextLayers
import com.company.domain.model.editor.cutTextAtEnd
import com.company.domain.model.editor.duplicateClip
import com.company.domain.model.editor.duplicateText
import com.company.domain.model.editor.finishAudioMove
import com.company.domain.model.editor.itemsOf
import com.company.domain.model.editor.moveText
import com.company.domain.model.editor.overlapsOtherText
import com.company.domain.model.editor.placedCaptions
import com.company.domain.model.editor.removeClip
import com.company.domain.model.editor.removeText
import com.company.domain.model.editor.setClipSpeed
import com.company.domain.model.editor.splitAt
import com.company.domain.model.editor.transformText
import com.company.domain.repository.DubbingRepository
import com.company.domain.repository.EditorProjectRepository
import com.company.domain.repository.EditorSettingsRepository
import com.company.domain.repository.FontRepository
import com.company.domain.repository.MediaLibraryRepository
import com.company.domain.repository.MyVoiceRepository
import com.company.domain.usecase.editor.GenerateCaptionsUseCase
import com.company.domain.usecase.editor.UpdateDraftUseCase
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.toast_deleted_undoable
import com.company.feature.editor.resources.toast_duplicated
import com.company.feature.editor.resources.toast_favorite_added
import com.company.feature.editor.resources.toast_favorite_removed
import com.company.feature.editor.resources.toast_layout_clip_hint
import com.company.feature.editor.resources.toast_layout_reset
import com.company.feature.editor.resources.toast_move_line_into_clip
import com.company.feature.editor.resources.toast_split_at_line
import com.company.feature.editor.resources.toast_undone
import com.company.ui.text.StringResolver
import com.company.ui.text.UiText
import com.company.ui.text.uiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Part of the editor that the action classes use. It gives the state, the project, and the one-time events.
 */
internal interface EditorHost {
    /** Current screen state. */
    val state: EditorState

    /** Project and undo history. */
    val projects: EditorProjectRepository

    /** Scope for work that ends with the screen. */
    val scope: CoroutineScope

    /** Reads default strings, for example the text of a new caption. */
    val strings: StringResolver

    /**
     * Changes the screen state.
     *
     * @param reducer Makes the new state from the current state.
     */
    fun reduce(reducer: EditorState.() -> EditorState)

    /**
     * Shows a short message.
     *
     * @param message Text of the message.
     */
    fun toast(message: UiText)

    /**
     * Sends a one-time event to the screen.
     *
     * @param effect The event.
     */
    fun send(effect: EditorSideEffect)

    /** Pauses the preview. */
    fun pause()
}

/**
 * ViewModel of the editor screen.
 *
 * The project and the undo history live in [EditorProjectRepository], so the picker
 * and the export screen see the same project. This ViewModel keeps only the screen state.
 *
 * The preview player of the screen moves the playhead. It sends [EditorIntent.PlaybackProgress]
 * while it plays and [EditorIntent.PlaybackEnded] at the end.
 *
 * The sheets of the editor have their own action classes: texts and animations, captions, sound, transitions, and
 * copy and paste.
 */
class EditorViewModel(
    private val projectRepository: EditorProjectRepository,
    private val settingsRepository: EditorSettingsRepository,
    mediaLibraryRepository: MediaLibraryRepository,
    generateCaptions: GenerateCaptionsUseCase,
    fontRepository: FontRepository,
    dubbingRepository: DubbingRepository,
    private val myVoiceRepository: MyVoiceRepository,
    private val updateDraft: UpdateDraftUseCase,
    private val stringResolver: StringResolver,
) : BaseViewModel<EditorState, EditorIntent, EditorSideEffect>(
    initialState = EditorState(
        project = projectRepository.project.value,
        canUndo = projectRepository.canUndo.value,
        musicTracks = mediaLibraryRepository.getMusicTracks(),
        voices = dubbingRepository.getVoices(),
        soundEffects = mediaLibraryRepository.getSoundEffects(),
        myVoice = myVoiceRepository.status.value,
    )
) {
    private val host = object : EditorHost {
        override val state: EditorState
            get() = currentState
        override val projects: EditorProjectRepository
            get() = projectRepository
        override val scope: CoroutineScope
            get() = viewModelScope
        override val strings: StringResolver
            get() = stringResolver

        override fun reduce(reducer: EditorState.() -> EditorState) = this@EditorViewModel.reduce(reducer)

        override fun toast(message: UiText) = this@EditorViewModel.toast(message)

        override fun send(effect: EditorSideEffect) {
            postSideEffect { effect }
        }

        override fun pause() = this@EditorViewModel.pause()
    }

    private val texts = TextActions(host, fontRepository)
    private val captions = CaptionActions(host, generateCaptions)
    private val audio = AudioActions(host, dubbingRepository)
    private val transitions = TransitionActions(host)
    private val clipboard = ClipboardActions(host)

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

    /** Timer that hides the controls of the full-screen preview. */
    private var fullScreenControlsJob: Job? = null

    init {
        viewModelScope.launch {
            projectRepository.project.collect { project ->
                reduce { copy(project = project, time = time.coerceIn(0.0, project.totalDuration)) }
            }
        }
        viewModelScope.launch { projectRepository.canUndo.collect { reduce { copy(canUndo = it) } } }
        viewModelScope.launch {
            settingsRepository.settings.collect { reduce { copy(watermark = it.watermark, favoriteTextStyles = it.favoriteTextStyles) } }
        }
        viewModelScope.launch {
            myVoiceRepository.status.collect { status ->
                reduce {
                    val draft = dubbingDraft?.let {
                        when {
                            // 더빙 시트에서 내 목소리를 만들었으면, 다 된 목소리를 바로 고른다.
                            status == MyVoiceStatus.Ready && myVoice != MyVoiceStatus.Ready -> it.copy(voiceId = MY_VOICE_ID)
                            // 내 목소리를 지우면 그 목소리를 고른 초안은 기본 목소리로 바꾼다.
                            status != MyVoiceStatus.Ready && it.voiceId == MY_VOICE_ID -> it.copy(voiceId = voices.first().id)
                            else -> it
                        }
                    }
                    copy(myVoice = status, dubbingDraft = draft)
                }
            }
        }
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
            EditorIntent.TogglePlay -> {
                if (currentState.playing) pause() else play()
                showFullScreenControls(autoHide = true)
            }
            EditorIntent.Pause -> pause()
            is EditorIntent.Scrub -> if (!currentState.playing && currentState.activeDrag == null) {
                reduce { copy(time = intent.time.coerceIn(0.0, totalDuration), scrubbing = true) }
            }
            EditorIntent.ScrubEnded -> reduce { copy(scrubbing = false) }
            is EditorIntent.PlaybackProgress -> if (currentState.playing) {
                reduce { copy(time = intent.time.coerceIn(0.0, totalDuration)) }
            }
            EditorIntent.PlaybackEnded -> {
                reduce { copy(time = totalDuration, playing = false) }
                showFullScreenControls(autoHide = false)
            }

            EditorIntent.OpenFullScreen -> openFullScreen()
            EditorIntent.CloseFullScreen -> closeFullScreen()
            EditorIntent.ToggleFullScreenControls -> if (currentState.fullScreen?.controlsVisible == true) {
                hideFullScreenControls()
            } else {
                showFullScreenControls(autoHide = true)
            }
            EditorIntent.ShowFullScreenControls -> showFullScreenControls(autoHide = true)
            is EditorIntent.StartFullScreenSeek -> startFullScreenSeek(intent.time)
            is EditorIntent.MoveFullScreenSeek -> if (currentState.fullScreen?.seeking == true) {
                reduce { copy(time = intent.time.coerceIn(0.0, totalDuration)) }
            }
            EditorIntent.EndFullScreenSeek -> endFullScreenSeek()

            is EditorIntent.SelectClip -> {
                pause()
                reduce { copy(selectedClipId = if (selectedClipId == intent.clipId) null else intent.clipId, selectedTextId = null, selectedTrackItem = null) }
            }
            is EditorIntent.SelectTrackItem -> {
                pause()
                reduce {
                    copy(
                        selectedTrackItem = if (selectedTrackItem == intent.item) null else intent.item,
                        selectedClipId = null,
                        selectedTextId = null,
                    )
                }
            }
            EditorIntent.ClearSelection ->
                // [완료]를 누르면 복사·붙여넣기도 끝난다.
                reduce { copy(selectedClipId = null, selectedTextId = null, selectedTrackItem = null, clipboard = null) }
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

            is EditorIntent.SelectText -> {
                pause()
                reduce { copy(selectedTextId = if (selectedTextId == intent.textId) null else intent.textId, selectedClipId = null, selectedTrackItem = null) }
            }
            EditorIntent.DuplicateText -> duplicateText()
            EditorIntent.RemoveText -> removeText()
            EditorIntent.StartTextDrag -> {
                pause()
                projectRepository.saveCheckpoint()
            }
            is EditorIntent.MoveText -> projectRepository.update { it.moveText(intent.textId, intent.x, intent.y) }
            EditorIntent.StartTextTransform -> {
                pause()
                projectRepository.saveCheckpoint()
            }
            is EditorIntent.TransformText ->
                projectRepository.update { it.transformText(intent.textId, intent.scale, intent.rotation) }

            is EditorIntent.OpenTextEditor -> texts.openTextEditor(intent.textId)
            is EditorIntent.ChangeDraftText -> texts.changeText(intent.text, intent.selectionStart, intent.selectionEnd)
            is EditorIntent.SelectTextTab -> reduce { copy(textDraft = textDraft?.copy(tab = intent.tab)) }
            is EditorIntent.ChangeDraftStyle -> reduce { copy(textDraft = textDraft?.copy(style = intent.style)) }
            is EditorIntent.ToggleFavoriteTextStyle -> toggleFavoriteTextStyle(intent.style)
            is EditorIntent.ChangeDraftFont -> texts.changeFont(intent.font)
            is EditorIntent.ChangeDraftSize -> reduce { copy(textDraft = textDraft?.copy(sizeStep = intent.step)) }
            is EditorIntent.DecorateDraft -> texts.decorate(intent.change)
            is EditorIntent.PickCustomColor -> texts.pickCustomColor(intent.rgb)
            EditorIntent.ResetDraftDecorations -> reduce { copy(textDraft = textDraft?.copy(decorations = emptyList())) }
            EditorIntent.SaveText -> texts.save()
            EditorIntent.DeleteText -> texts.delete()

            EditorIntent.OpenAnimationSheet -> texts.openAnimationSheet()
            is EditorIntent.SelectAnimationTab -> reduce { copy(animationTab = intent.tab) }
            is EditorIntent.PickEntrance -> texts.pickEntrance(intent.entrance)
            is EditorIntent.PickExit -> texts.pickExit(intent.exit)
            is EditorIntent.PickLoop -> texts.pickLoop(intent.loop)
            is EditorIntent.ChangeAnimationLength -> texts.changeAnimationLength(intent.millis)
            EditorIntent.ResetAnimation -> texts.resetAnimation()

            is EditorIntent.OpenSheet -> openSheet(intent.sheet)
            EditorIntent.CloseSheet -> closeSheet()
            is EditorIntent.PickLayout -> pickLayout(intent.layout)
            EditorIntent.ResetLayout -> if (currentState.project.layout != LayoutType.FULL) {
                projectRepository.commit { it.copy(layout = LayoutType.FULL) }
                toast(uiText(Res.string.toast_layout_reset))
            }
            is EditorIntent.PickSpeed -> currentState.selectedClip?.let { clip ->
                if (clip.speed != intent.speed) projectRepository.commit { it.setClipSpeed(clip.id, intent.speed) }
            }

            is EditorIntent.PickCaptionStyle -> captions.pickStyle(intent.style)
            EditorIntent.GenerateCaptions -> captions.generate()
            EditorIntent.ToggleCaptionFromPlayhead -> if (currentState.captionProgress == null) {
                reduce { copy(captionFromPlayhead = !captionFromPlayhead) }
            }
            EditorIntent.RemoveCaptions -> captions.removeAll()
            EditorIntent.OpenCaptionScript -> captions.openScript()
            EditorIntent.CloseCaptionScript -> captions.closeScript()
            is EditorIntent.ChangeCaptionScript -> reduce { copy(captionScript = intent.script) }
            EditorIntent.InsertCaptionScript -> captions.insertScript()
            EditorIntent.StartCaptionMove -> captions.startMove()
            is EditorIntent.MoveCaption -> captions.move(intent.position)
            EditorIntent.EndCaptionMove -> captions.endMove()
            EditorIntent.ResetCaptionPosition -> captions.resetPosition()
            is EditorIntent.OpenCaptionEditor -> texts.openCaptionEditor(intent.captionId)
            EditorIntent.SplitCaption -> captions.splitSelected()

            is EditorIntent.PickMusicMood -> reduce { copy(musicDraft = musicDraft?.copy(mood = intent.mood)) }
            is EditorIntent.TapMusicTrack -> audio.tapMusic(intent.musicId)
            is EditorIntent.MoveMusicRange -> audio.moveMusicRange(intent.origin, intent.delta, intent.moveStart, intent.moveEnd)
            is EditorIntent.ChangeMusicVolume -> reduce { copy(musicDraft = musicDraft?.copy(volume = intent.volume.coerceIn(0, 100))) }
            EditorIntent.ToggleMusicFade -> reduce { copy(musicDraft = musicDraft?.copy(fade = !musicDraft.fade)) }
            EditorIntent.ApplyMusic -> audio.applyMusic()

            is EditorIntent.OpenDubbing -> audio.openDubbing(intent.captionId, intent.editId)
            is EditorIntent.ChangeDubbingText -> audio.changeDubbingText(intent.text)
            is EditorIntent.PickDubbingVoice -> reduce { copy(dubbingDraft = dubbingDraft?.copy(voiceId = intent.voiceId)) }
            is EditorIntent.PickDubbingSpeed -> reduce { copy(dubbingDraft = dubbingDraft?.copy(speed = intent.speed)) }
            EditorIntent.InsertDubbing -> audio.insertDubbing()
            EditorIntent.FitDubbing -> audio.fitDubbing()
            EditorIntent.OpenMyVoice -> {
                pause()
                postSideEffect { EditorSideEffect.OpenMyVoice }
            }

            is EditorIntent.PickSoundEffectCategory -> reduce { copy(soundEffectCategory = intent.category) }
            is EditorIntent.InsertSoundEffect -> audio.insertSoundEffect(intent.effectId)
            is EditorIntent.OpenVolume -> {
                pause()
                reduce { copy(sheet = EditorSheet.VOLUME, volumeTarget = intent.target) }
            }
            is EditorIntent.ChangeVolume -> audio.changeVolume(intent.volume)
            EditorIntent.DuplicateTrackItem -> audio.duplicateSelected()
            EditorIntent.RemoveTrackItem -> removeTrackItem()

            is EditorIntent.OpenTransition -> transitions.open(intent.slot)
            EditorIntent.OpenNearestTransition -> transitions.openNearest()
            is EditorIntent.PickTransition -> transitions.pick(intent.type)
            is EditorIntent.PickPushDirection -> transitions.pickDirection(intent.direction)
            EditorIntent.ApplyTransitionToAll -> transitions.applyToAll()

            EditorIntent.CopySelected -> clipboard.copySelected()
            EditorIntent.OpenPaste -> clipboard.openPaste()
            is EditorIntent.Paste -> clipboard.paste(intent.target)

            EditorIntent.ConfirmDialog -> confirmDialog()
            EditorIntent.DismissDialog -> reduce { copy(dialog = null) }

            EditorIntent.Undo -> undo()
            EditorIntent.Export -> if (currentState.project.clips.isNotEmpty()) {
                pause()
                updateDraft()
                postSideEffect { EditorSideEffect.OpenExport }
            }
            EditorIntent.Back -> {
                pause()
                reduce { copy(leaveDialog = true) }
            }
            EditorIntent.ConfirmLeave -> {
                reduce { copy(leaveDialog = false) }
                updateDraft()
                postSideEffect { EditorSideEffect.Close }
            }
            EditorIntent.DismissLeave -> reduce { copy(leaveDialog = false) }
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
        reduce { copy(playing = true, selectedClipId = null, selectedTextId = null, selectedTrackItem = null, time = start) }
    }

    private fun pause() {
        if (currentState.playing) reduce { copy(playing = false) }
    }

    // =====================================================================
    // Full-screen preview
    // =====================================================================

    /** Shows the full-screen preview and plays it. The controls show first and hide after a short time. */
    private fun openFullScreen() {
        if (currentState.project.clips.isEmpty() || currentState.fullScreen != null) return
        pause()
        closeSheet()
        reduce { copy(fullScreen = FullScreenState(), activeDrag = null) }
        play()
        showFullScreenControls(autoHide = true)
    }

    private fun closeFullScreen() {
        fullScreenControlsJob?.cancel()
        pause()
        reduce { copy(fullScreen = null) }
    }

    /**
     * Shows the controls of the full-screen preview. Nothing happens when the full-screen preview is closed.
     *
     * @param autoHide True to hide the controls after [FULL_SCREEN_CONTROLS_MILLIS] without a touch. A seek keeps
     *   them visible until it ends.
     */
    private fun showFullScreenControls(autoHide: Boolean) {
        val fullScreen = currentState.fullScreen ?: return
        fullScreenControlsJob?.cancel()
        if (!fullScreen.controlsVisible) reduce { copy(fullScreen = fullScreen.copy(controlsVisible = true)) }
        if (!autoHide) return
        fullScreenControlsJob = viewModelScope.launch {
            delay(FULL_SCREEN_CONTROLS_MILLIS)
            val current = currentState.fullScreen ?: return@launch
            if (!current.seeking) reduce { copy(fullScreen = current.copy(controlsVisible = false)) }
        }
    }

    private fun hideFullScreenControls() {
        val fullScreen = currentState.fullScreen ?: return
        fullScreenControlsJob?.cancel()
        reduce { copy(fullScreen = fullScreen.copy(controlsVisible = false)) }
    }

    /** Pauses the preview while the finger is on the seek bar. The preview plays again after the seek if it played before. */
    private fun startFullScreenSeek(seekTime: Double) {
        val fullScreen = currentState.fullScreen ?: return
        fullScreenControlsJob?.cancel()
        val resume = currentState.playing
        pause()
        reduce {
            copy(
                time = seekTime.coerceIn(0.0, totalDuration),
                fullScreen = fullScreen.copy(controlsVisible = true, seeking = true, resumeAfterSeek = resume),
            )
        }
    }

    private fun endFullScreenSeek() {
        val fullScreen = currentState.fullScreen ?: return
        if (!fullScreen.seeking) return
        reduce { copy(fullScreen = fullScreen.copy(seeking = false, resumeAfterSeek = false)) }
        if (fullScreen.resumeAfterSeek && currentState.time < currentState.totalDuration - END_MARGIN) play()
        showFullScreenControls(autoHide = true)
    }

    // =====================================================================
    // Timeline drag
    // =====================================================================

    private fun startDrag(target: TimelineDragTarget, snapSeconds: Double) {
        if (currentState.project.dragOrigin(target) == null) return
        pause()
        projectRepository.saveCheckpoint()
        // 타임라인은 글자를 프로젝트 끝까지만 보여 준다. 보이는 막대에서 끌기를 시작하도록 먼저 자른다.
        val textId = when (target) {
            is TimelineDragTarget.TextEdge -> target.textId
            is TimelineDragTarget.TextMove -> target.textId
            else -> null
        }
        if (textId != null) projectRepository.update { it.cutTextAtEnd(textId) }
        val project = projectRepository.project.value
        val origin = project.dragOrigin(target) ?: return
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
                is TimelineDragTarget.ClipEdge, is TimelineDragTarget.AudioMove -> copy(activeDrag = target, dragBlocked = false)
                is TimelineDragTarget.TextEdge ->
                    copy(activeDrag = target, dragBlocked = false, selectedTextId = target.textId, selectedClipId = null, selectedTrackItem = null)
                is TimelineDragTarget.TextMove ->
                    copy(activeDrag = target, dragBlocked = false, selectedTextId = target.textId, selectedClipId = null, selectedTrackItem = null)
                is TimelineDragTarget.CaptionEdge -> copy(
                    activeDrag = target,
                    dragBlocked = false,
                    selectedTrackItem = TrackSelection.Caption(target.captionId),
                    selectedClipId = null,
                    selectedTextId = null,
                )
            }
        }
    }

    /**
     * Moves the dragged part by the finger movement plus the edge scrolling, then moves the playhead.
     *
     * The dragged edge snaps to the playhead and to other items. The edge scrolling moves the playhead, so the dragged
     * part stays under the finger. The clips are laid out from the project start, so a clip start trim moves the clip
     * end on the screen. Thus a clip start trim also moves the playhead by the trimmed length, and the clip end stays
     * in place. Near the project start, [EditorState.dragShift] moves the timeline for the part that the playhead
     * cannot move.
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
        projectRepository.update { project -> project.dragTo(target, origin.start, origin.end, requested, layer) }
        val project = projectRepository.project.value
        // 도메인은 항목을 허용 범위 안에 둔다. 실제로 움직인 길이를 기준으로 계산한다.
        val moved = project.draggedDistance(target, origin.start, origin.end) ?: return
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
        when (target) {
            is TimelineDragTarget.TextMove -> if (blocked) {
                // 다른 글자와 겹치는 곳에는 놓지 않는다. 끌기를 시작할 때 저장한 단계로 되돌린다.
                projectRepository.undo()
            } else {
                projectRepository.update { it.compactTextLayers() }
            }
            // 다른 더빙·효과음과 겹치면 빈 줄로 옮긴다.
            is TimelineDragTarget.AudioMove -> projectRepository.update { it.finishAudioMove(target.track, target.id) }
            else -> Unit
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
     * Dragged item when a timeline drag started.
     *
     * @property start Start of the item. For a clip, it is the source in-point. For other items, it is a timeline time.
     * @property end End of the item. For a clip, it is the source out-point. For other items, it is a timeline time.
     * @property layer Row of a text overlay or an audio item. It is 0 for a clip and a caption.
     */
    private data class DragOrigin(val start: Double, val end: Double, val layer: Int)

    /** Place of the dragged item now, or null when the item is not in the project. */
    private fun EditorProject.dragOrigin(target: TimelineDragTarget): DragOrigin? = when (target) {
        is TimelineDragTarget.ClipEdge -> clips.find { it.id == target.clipId }
            ?.let { DragOrigin(start = it.inPoint, end = it.outPoint, layer = 0) }
        is TimelineDragTarget.TextEdge -> texts.find { it.id == target.textId }
            ?.let { DragOrigin(start = it.start, end = it.end, layer = it.layer) }
        is TimelineDragTarget.TextMove -> texts.find { it.id == target.textId }
            ?.let { DragOrigin(start = it.start, end = it.end, layer = it.layer) }
        is TimelineDragTarget.CaptionEdge -> placedCaptions.find { it.id == target.captionId }
            ?.let { DragOrigin(start = it.start, end = it.end, layer = 0) }
        is TimelineDragTarget.AudioMove -> itemsOf(target.track).find { it.id == target.id }
            ?.let { DragOrigin(start = it.start, end = it.end, layer = it.layer) }
    }

    // =====================================================================
    // Clip editing
    // =====================================================================

    private fun splitClip() {
        val split = currentState.project.splitAt(currentState.time)
        if (split == null) {
            toast(uiText(Res.string.toast_move_line_into_clip))
            return
        }
        projectRepository.commit { split }
        reduce { copy(selectedClipId = null) }
        toast(uiText(Res.string.toast_split_at_line))
    }

    private fun duplicateClip() {
        val clipId = currentState.selectedClipId ?: return
        val (project, copyId) = currentState.project.duplicateClip(clipId) ?: return
        projectRepository.commit { project }
        reduce { copy(selectedClipId = copyId) }
        toast(uiText(Res.string.toast_duplicated))
    }

    private fun removeClip() {
        val clipId = currentState.selectedClipId ?: return
        projectRepository.commit { it.removeClip(clipId) }
        reduce { copy(selectedClipId = null, time = 0.0) }
        toast(uiText(Res.string.toast_deleted_undoable))
    }

    private fun pickLayout(layout: LayoutType) {
        val project = currentState.project
        if (project.layout == layout) return
        projectRepository.commit { it.copy(layout = layout) }
        if (layout.clipCount > project.clips.size) toast(uiText(Res.string.toast_layout_clip_hint, layout.clipCount))
    }

    /**
     * Adds the text template to the favorites at the front, or removes it, and tells the user. The favorites are a
     * user setting, not a part of the project.
     */
    private fun toggleFavoriteTextStyle(style: TextStyleType) {
        val added = style !in currentState.favoriteTextStyles
        settingsRepository.update { settings ->
            val others = settings.favoriteTextStyles - style
            settings.copy(favoriteTextStyles = if (added) listOf(style) + others else others)
        }
        toast(uiText(if (added) Res.string.toast_favorite_added else Res.string.toast_favorite_removed))
    }

    private fun undo() {
        if (!projectRepository.undo()) return
        pause()
        reduce { copy(selectedClipId = null, selectedTextId = null, selectedTrackItem = null) }
        toast(uiText(Res.string.toast_undone))
    }

    // =====================================================================
    // Texts and track items
    // =====================================================================

    private fun duplicateText() {
        val textId = currentState.selectedTextId ?: return
        val (project, copyId) = currentState.project.duplicateText(textId) ?: return
        projectRepository.commit { project }
        reduce { copy(selectedTextId = copyId) }
        toast(uiText(Res.string.toast_duplicated))
    }

    private fun removeText() {
        val textId = currentState.selectedTextId ?: return
        projectRepository.commit { it.removeText(textId) }
        reduce { copy(selectedTextId = null) }
        toast(uiText(Res.string.toast_deleted_undoable))
    }

    private fun removeTrackItem() {
        val item = currentState.selectedTrackItem ?: return
        when (item) {
            is TrackSelection.Caption -> captions.remove(item.id)
            is TrackSelection.Dubbing, is TrackSelection.SoundEffect -> audio.removeSelected()
        }
        reduce { copy(selectedTrackItem = null) }
        toast(uiText(Res.string.toast_deleted_undoable))
    }

    // =====================================================================
    // Sheets and popups
    // =====================================================================

    private fun openSheet(sheet: EditorSheet) {
        when (sheet) {
            EditorSheet.TEXT -> texts.openTextEditor(textId = null)
            EditorSheet.MUSIC -> audio.openMusic()
            EditorSheet.DUBBING -> audio.openDubbing(captionId = null, editId = null)
            EditorSheet.TRANSITION -> transitions.openNearest()
            EditorSheet.PASTE -> clipboard.openPaste()
            EditorSheet.ANIMATION -> texts.openAnimationSheet()
            else -> {
                pause()
                // 자막 시트는 열 때마다 영상 처음부터 만드는 상태로 시작한다.
                reduce { copy(sheet = sheet, captionScript = null, captionFromPlayhead = false) }
            }
        }
    }

    private fun closeSheet() {
        captions.stop()
        audio.stop()
        reduce {
            copy(
                sheet = null,
                textDraft = null,
                captionProgress = null,
                captionScript = null,
                musicDraft = null,
                dubbingDraft = null,
                volumeTarget = null,
                transitionSlot = null,
            )
        }
    }

    private fun confirmDialog() {
        val dialog = currentState.dialog ?: return
        reduce { copy(dialog = null) }
        when (val action = dialog.action) {
            is DialogAction.ReplaceWithSpeechCaptions -> captions.startGeneration(action.from)
            is DialogAction.InsertScript -> captions.putScript(action.checkExisting)
            is DialogAction.PasteReplacingCaption -> clipboard.pasteNow(action.target, replaceCaptions = true)
        }
    }

    private fun toast(message: UiText) {
        postSideEffect { EditorSideEffect.ShowToast(message) }
    }

    private companion object {
        /** A playhead this close to the end, in seconds, counts as the end. */
        const val END_MARGIN = 0.05

        /** Time without a touch before the controls of the full-screen preview hide. */
        const val FULL_SCREEN_CONTROLS_MILLIS = 1_500L
    }
}
