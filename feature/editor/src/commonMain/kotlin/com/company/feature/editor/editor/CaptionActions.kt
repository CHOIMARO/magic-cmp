package com.company.feature.editor.editor

import com.company.domain.model.editor.CaptionSplit
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.captionCountFrom
import com.company.domain.model.editor.captionPositionFor
import com.company.domain.model.editor.captionsBefore
import com.company.domain.model.editor.captionsFromScript
import com.company.domain.model.editor.removeCaption
import com.company.domain.model.editor.scriptLines
import com.company.domain.model.editor.scriptStart
import com.company.domain.model.editor.speechRangesFrom
import com.company.domain.model.editor.splitCaption
import com.company.domain.usecase.editor.GenerateCaptionsUseCase
import com.company.feature.editor.resources.*
import com.company.ui.editor.look
import com.company.ui.text.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Update interval of the caption progress. */
private const val CAPTION_TICK_MILLIS = 45L

/** Progress step of the caption generation in percent. */
private const val CAPTION_STEP = 4

/** A playhead at this time or earlier, in seconds, counts as the start of the video. */
private const val PLAYHEAD_START = 1e-6

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

    /** True while the dragged caption is at the height of the caption style. A new snap gives a vibration. */
    private var captionSnapped = false

    /** True when the sheet makes captions from the playhead and the playhead is after the start. */
    private val fromPlayhead: Boolean
        get() = host.state.captionFromPlayhead && host.state.time > PLAYHEAD_START

    /**
     * Applies the caption style to all captions. The same style again changes nothing.
     *
     * @param style New style.
     */
    fun pickStyle(style: CaptionStyleType) {
        if (host.state.project.captionStyle != style) host.projects.commit { it.copy(captionStyle = style) }
    }

    /**
     * Makes captions from the speech, from the playhead when the sheet asks for it. Without speech after that place,
     * a message tells it. When the new captions replace captions, a popup asks first.
     */
    fun generate() {
        val state = host.state
        if (state.captionProgress != null) return
        val head = fromPlayhead
        val from = if (head) state.time else 0.0
        if (state.project.clips.speechRangesFrom(from).isEmpty()) {
            host.toast(uiText(if (head) Res.string.toast_no_speech_after_playhead else Res.string.toast_no_speech))
            return
        }
        val count = state.project.captionCountFrom(from)
        if (count > 0) {
            host.reduce {
                copy(
                    dialog = EditorDialog(
                        uiText(if (head) Res.string.dialog_replace_captions_after_title else Res.string.dialog_replace_captions_title),
                        uiText(if (head) Res.string.dialog_replace_captions_speech_after else Res.string.dialog_replace_captions_speech, count),
                        uiText(Res.string.dialog_replace),
                        DialogAction.ReplaceWithSpeechCaptions(from),
                    ),
                )
            }
        } else {
            startGeneration(from)
        }
    }

    /**
     * Shows a progress first, then makes the captions from [from]. The captions before [from] stay. At the end, the
     * playhead goes to the first new caption. The prototype has no real speech recognition.
     *
     * @param from Time on the timeline where the new captions start.
     */
    fun startGeneration(from: Double) {
        captionJob?.cancel()
        host.reduce { copy(captionProgress = 0) }
        captionJob = host.scope.launch {
            while ((host.state.captionProgress ?: 0) < 100 - CAPTION_STEP) {
                delay(CAPTION_TICK_MILLIS)
                host.reduce { copy(captionProgress = (captionProgress ?: 0) + CAPTION_STEP) }
            }
            delay(CAPTION_TICK_MILLIS)
            val project = host.state.project
            val added = generateCaptions(project.clips, from)
            host.projects.commit { it.copy(captions = it.captionsBefore(from) + added) }
            val head = added.firstOrNull()?.start ?: from
            host.reduce { copy(captionProgress = null, sheet = null, selectedTrackItem = null, time = head) }
            host.toast(uiText(Res.string.toast_captions_inserted, added.size))
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
        host.toast(uiText(Res.string.toast_captions_removed))
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
            host.toast(uiText(Res.string.toast_add_media_first))
            return
        }
        host.reduce { copy(captionScript = lastScript) }
    }

    /** Goes back to the caption sheet. The script stays for the next time. */
    fun closeScript() {
        lastScript = host.state.captionScript ?: lastScript
        host.reduce { copy(captionScript = null) }
    }

    /**
     * Puts the script lines into the clips, from the playhead when the sheet asks for it. A popup asks first when
     * lines are left over (D08).
     */
    fun insertScript() {
        val state = host.state
        val lines = scriptLines(state.captionScript ?: return)
        if (lines.isEmpty() || state.project.clips.isEmpty()) return
        val head = fromPlayhead
        val start = state.project.scriptStart(if (head) state.time else 0.0)
        val clipCount = state.project.clips.size - start.clipIndex
        if (clipCount <= 0) {
            host.toast(uiText(Res.string.toast_no_clip_after_playhead))
            return
        }
        if (lines.size > clipCount) {
            host.reduce {
                copy(
                    dialog = EditorDialog(
                        uiText(Res.string.dialog_script_over_title),
                        uiText(if (head) Res.string.dialog_script_over_after_message else Res.string.dialog_script_over_message, clipCount, lines.size - clipCount),
                        uiText(Res.string.insert),
                        DialogAction.InsertScript(checkExisting = true),
                    ),
                )
            }
        } else {
            putScript(checkExisting = true)
        }
    }

    /**
     * Puts the script lines into the clips and closes the sheet. The first line starts at the playhead when the sheet
     * asks for it, and each next line goes to the next clip. Lines after the last clip are not used. The captions
     * before the first line stay, and the playhead goes to the first line.
     *
     * @param checkExisting True to ask first when the new captions replace captions.
     */
    fun putScript(checkExisting: Boolean) {
        val state = host.state
        val head = fromPlayhead
        val start = state.project.scriptStart(if (head) state.time else 0.0)
        val count = state.project.captionCountFrom(start.time)
        if (checkExisting && count > 0) {
            host.reduce {
                copy(
                    dialog = EditorDialog(
                        uiText(if (head) Res.string.dialog_replace_captions_after_title else Res.string.dialog_replace_captions_title),
                        uiText(if (head) Res.string.dialog_replace_captions_script_after else Res.string.dialog_replace_captions_script, count),
                        uiText(Res.string.dialog_replace),
                        DialogAction.InsertScript(checkExisting = false),
                    ),
                )
            }
            return
        }
        val lines = scriptLines(state.captionScript ?: return).take(state.project.clips.size - start.clipIndex)
        host.projects.commit {
            it.copy(captions = it.captionsBefore(start.time) + it.captionsFromScript(lines, start.clipIndex, start.offset))
        }
        lastScript = ""
        host.reduce { copy(sheet = null, captionScript = null, selectedTrackItem = null, time = start.time) }
        host.toast(uiText(Res.string.toast_captions_inserted, lines.size))
    }

    // =====================================================================
    // Caption height
    // =====================================================================

    /** Starts a caption drag on the stage. The whole drag is one undo step. Ignored while the preview plays. */
    fun startMove() {
        val state = host.state
        if (state.playing || state.fullScreen != null) return
        host.projects.saveCheckpoint()
        captionSnapped = state.project.captionPosition == null
        host.reduce { copy(captionMoving = true) }
    }

    /**
     * Moves all captions during a drag. Near the height of the caption style, the captions snap to it and the phone
     * vibrates once.
     *
     * @param position Height from the bottom of the screen that the finger gives, in percent.
     */
    fun move(position: Double) {
        val state = host.state
        if (!state.captionMoving) return
        val stylePosition = state.project.captionStyle.look().bottomPercent.toDouble()
        val newPosition = captionPositionFor(position, stylePosition)
        if (newPosition != state.project.captionPosition) host.projects.update { it.copy(captionPosition = newPosition) }
        val snapped = newPosition == null
        if (snapped && !captionSnapped) host.send(EditorSideEffect.Snapped)
        captionSnapped = snapped
    }

    /** Ends a caption drag. */
    fun endMove() {
        if (host.state.captionMoving) host.reduce { copy(captionMoving = false) }
    }

    /** Moves all captions back to the height of the caption style. */
    fun resetPosition() {
        if (host.state.project.captionPosition == null) return
        host.projects.commit { it.copy(captionPosition = null) }
        host.toast(uiText(Res.string.toast_caption_position_reset))
    }

    // =====================================================================
    // Caption track
    // =====================================================================

    /** Splits the selected caption at the playhead. */
    fun splitSelected() {
        val caption = host.state.selectedCaption ?: return
        when (val result = host.state.project.splitCaption(caption.id, host.state.time)) {
            is CaptionSplit.Done -> {
                host.projects.commit { result.project }
                host.toast(uiText(Res.string.toast_split_at_line))
            }
            CaptionSplit.OutsideCaption -> host.toast(uiText(Res.string.toast_move_line_into_caption))
            CaptionSplit.OneWord -> host.toast(uiText(Res.string.toast_one_word))
        }
    }
}
