package com.company.feature.editor.editor

import com.company.core.domain.model.editor.CaptionSplit
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.addCaptionAt
import com.company.core.domain.model.editor.captionsFromScript
import com.company.core.domain.model.editor.removeCaption
import com.company.core.domain.model.editor.scriptLines
import com.company.core.domain.model.editor.splitCaption
import com.company.core.domain.usecase.editor.GenerateCaptionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Text of a caption that the user adds at the playhead. */
private const val NEW_CAPTION_TEXT = "새 자막"

/** Update interval of the caption progress. */
private const val CAPTION_TICK_MILLIS = 45L

/** Progress step of the caption generation in percent. */
private const val CAPTION_STEP = 4

/**
 * Caption sheet: captions from speech and from a script (F04), the caption style, and the caption edits on the
 * timeline.
 *
 * @param host Editor state and project.
 * @param generateCaptions Makes captions from the speech of the video clips.
 */
internal class CaptionActions(private val host: EditorHost, private val generateCaptions: GenerateCaptionsUseCase) {
    private var captionJob: Job? = null

    /** Script of the last script field. It comes back when the user opens the field again. */
    private var lastScript = ""

    /**
     * Applies the caption style to all captions. The same style again changes nothing.
     *
     * @param style New style.
     */
    fun pickStyle(style: CaptionStyleType) {
        if (host.state.project.captionStyle != style) host.projects.commit { it.copy(captionStyle = style) }
    }

    /** Makes captions from the speech. When captions exist, a popup asks first. */
    fun generate() {
        val state = host.state
        if (state.captionProgress != null) return
        val count = state.project.captions.size
        if (count > 0) {
            host.reduce {
                copy(dialog = EditorDialog("기존 자막을 바꿀까요?", "지금 있는 자막 ${count}개를 지우고 말소리로 새로 만들어요", "바꾸기", DialogAction.ReplaceWithSpeechCaptions))
            }
        } else {
            startGeneration()
        }
    }

    /** Shows a progress first, then makes the captions. The prototype has no real speech recognition. */
    fun startGeneration() {
        captionJob?.cancel()
        host.reduce { copy(captionProgress = 0) }
        captionJob = host.scope.launch {
            while ((host.state.captionProgress ?: 0) < 100 - CAPTION_STEP) {
                delay(CAPTION_TICK_MILLIS)
                host.reduce { copy(captionProgress = (captionProgress ?: 0) + CAPTION_STEP) }
            }
            delay(CAPTION_TICK_MILLIS)
            val captions = generateCaptions(host.state.project.clips)
            host.projects.commit { it.copy(captions = captions) }
            host.reduce { copy(captionProgress = null, sheet = null, selectedTrackItem = null) }
            host.toast("자막을 넣었어요")
        }
    }

    /** Stops the caption generation and keeps the script for the next time. Closing the sheet calls it. */
    fun stop() {
        captionJob?.cancel()
        captionJob = null
        host.state.captionScript?.let { lastScript = it }
    }

    /** Removes all captions and closes the sheet. */
    fun removeAll() {
        if (host.state.captionProgress != null) return
        host.projects.commit { it.copy(captions = emptyList()) }
        host.reduce { copy(sheet = null, selectedTrackItem = null) }
        host.toast("자막을 지웠어요 · 되돌리기 가능")
    }

    /**
     * Removes one caption.
     *
     * @param captionId ID of the caption.
     */
    fun remove(captionId: String) {
        host.projects.commit { it.removeCaption(captionId) }
    }

    // =====================================================================
    // Script
    // =====================================================================

    /** Shows the script field. A project without clips has no part to put the lines into. */
    fun openScript() {
        if (host.state.project.clips.isEmpty()) {
            host.toast("먼저 영상이나 사진을 넣어 주세요")
            return
        }
        host.reduce { copy(captionScript = lastScript) }
    }

    /** Goes back to the caption sheet. The script stays for the next time. */
    fun closeScript() {
        lastScript = host.state.captionScript ?: lastScript
        host.reduce { copy(captionScript = null) }
    }

    /** Puts the script lines into the clips. A popup asks first when lines are left over (D08). */
    fun insertScript() {
        val state = host.state
        val lines = scriptLines(state.captionScript ?: return)
        val clipCount = state.project.clips.size
        if (lines.isEmpty() || clipCount == 0) return
        if (lines.size > clipCount) {
            host.reduce {
                copy(
                    dialog = EditorDialog(
                        "자막이 영상 구간보다 많아요",
                        "구간 ${clipCount}개에 맞춰 넣고 남는 ${lines.size - clipCount}줄은 잘려요",
                        "넣기",
                        DialogAction.InsertScript(checkExisting = true),
                    ),
                )
            }
        } else {
            putScript(checkExisting = true)
        }
    }

    /**
     * Puts the script lines into the clips and closes the sheet. Lines after the last clip are not used.
     *
     * @param checkExisting True to ask first when the project has captions.
     */
    fun putScript(checkExisting: Boolean) {
        val state = host.state
        val count = state.project.captions.size
        if (checkExisting && count > 0) {
            host.reduce { copy(dialog = EditorDialog("기존 자막을 바꿀까요?", "지금 있는 자막 ${count}개를 지우고 새로 넣어요", "바꾸기", DialogAction.InsertScript(checkExisting = false))) }
            return
        }
        val lines = scriptLines(state.captionScript ?: return).take(state.project.clips.size)
        host.projects.commit { it.copy(captions = it.captionsFromScript(lines)) }
        lastScript = ""
        host.reduce { copy(sheet = null, captionScript = null, selectedTrackItem = null) }
        host.toast("자막 ${lines.size}개를 넣었어요")
    }

    // =====================================================================
    // Caption track
    // =====================================================================

    /**
     * Adds a caption at the playhead, selects it, and opens it in the text sheet.
     *
     * @param openEditor Opens the text sheet for the new caption.
     */
    fun addAtPlayhead(openEditor: (String) -> Unit) {
        val (project, captionId) = host.state.project.addCaptionAt(host.state.time, NEW_CAPTION_TEXT) ?: run {
            host.toast("재생선을 자막이 없는 곳으로 옮겨 주세요")
            return
        }
        host.projects.commit { project }
        openEditor(captionId)
    }

    /** Splits the selected caption at the playhead. */
    fun splitSelected() {
        val caption = host.state.selectedCaption ?: return
        when (val result = host.state.project.splitCaption(caption.id, host.state.time)) {
            is CaptionSplit.Done -> {
                host.projects.commit { result.project }
                host.toast("흰 선 위치에서 나눴어요")
            }
            CaptionSplit.OutsideCaption -> host.toast("흰 선을 자막 안쪽으로 옮겨 주세요")
            CaptionSplit.OneWord -> host.toast("단어가 하나라 나눌 수 없어요")
        }
    }
}
