package com.company.feature.editor.editor

import com.company.domain.model.editor.CopiedElement
import com.company.domain.model.editor.PasteTarget
import com.company.domain.model.editor.captionsInClip
import com.company.domain.model.editor.copyElement
import com.company.domain.model.editor.paste
import com.company.domain.model.editor.pasteOptions
import com.company.feature.editor.resources.*
import com.company.ui.text.uiText

/**
 * Copy and paste of texts and captions (F12).
 *
 * The clipboard holds one element of this project. The "완료" button clears it, as in the prototype.
 *
 * @param host Editor state and project.
 */
internal class ClipboardActions(private val host: EditorHost) {
    /** Copies the selected text or caption with its values of this moment. */
    fun copySelected() {
        val state = host.state
        val text = state.selectedText
        val caption = state.selectedCaption
        val clipboard = when {
            text != null -> state.project.copyElement(CopiedElement.Text(text), text.start, text.length)
            caption != null -> state.project.copyElement(CopiedElement.Caption(caption.text, caption.decorations), caption.start, caption.length)
            else -> null
        } ?: return
        host.reduce { copy(clipboard = clipboard) }
        host.toast(uiText(Res.string.toast_copied))
    }

    /** Opens the paste sheet. */
    fun openPaste() {
        if (host.state.clipboard == null) return
        host.pause()
        host.reduce { copy(sheet = EditorSheet.PASTE) }
    }

    /**
     * Pastes into the clips of a target. A paste of a caption into one clip with captions asks first.
     *
     * @param target Where to paste.
     */
    fun paste(target: PasteTarget) {
        val state = host.state
        val clipboard = state.clipboard ?: return
        val indexes = state.project.pasteOptions(clipboard, state.time)?.indexesOf(target).orEmpty()
        if (indexes.isEmpty()) {
            host.toast(uiText(if (target == PasteTarget.NEXT) Res.string.toast_no_next_clip else Res.string.toast_no_clip))
            return
        }
        val single = indexes.singleOrNull()
        if (clipboard.element is CopiedElement.Caption && single != null && state.project.captionsInClip(single).isNotEmpty()) {
            host.reduce {
                copy(dialog = EditorDialog(uiText(Res.string.dialog_paste_caption_title), uiText(Res.string.dialog_paste_caption_message), uiText(Res.string.dialog_replace), DialogAction.PasteReplacingCaption(target)))
            }
            return
        }
        pasteNow(target, replaceCaptions = false)
    }

    /**
     * Pastes into the clips of a target without a question.
     *
     * A paste into one clip selects the new element and moves the playhead to its start. The next "다음 구간" paste
     * uses the clip after the last pasted clip.
     *
     * @param target Where to paste.
     * @param replaceCaptions True to remove the captions of the target clips. False skips a clip with captions.
     */
    fun pasteNow(target: PasteTarget, replaceCaptions: Boolean) {
        val state = host.state
        val clipboard = state.clipboard ?: return
        val indexes = state.project.pasteOptions(clipboard, state.time)?.indexesOf(target).orEmpty()
        val result = state.project.paste(clipboard, indexes, replaceCaptions)
        val last = result.pasted.lastOrNull()
        if (last == null) {
            host.toast(
                when {
                    result.shortClips > 0 && indexes.size > 1 -> uiText(Res.string.toast_skipped_short, result.shortClips)
                    result.shortClips > 0 -> uiText(Res.string.toast_clip_too_short)
                    else -> uiText(Res.string.toast_skipped_caption_clips)
                },
            )
            return
        }
        host.projects.commit { result.project }
        val isText = clipboard.element is CopiedElement.Text
        host.reduce {
            val base = copy(sheet = null, clipboard = clipboard.copy(lastClipId = result.project.clips[last.clipIndex].id))
            if (target == PasteTarget.ALL) {
                base
            } else {
                base.copy(
                    selectedTextId = if (isText) last.id else null,
                    selectedTrackItem = if (isText) null else TrackSelection.Caption(last.id),
                    selectedClipId = null,
                    time = last.start,
                )
            }
        }
        val extras = listOfNotNull(
            uiText(Res.string.toast_skipped_short, result.shortClips).takeIf { result.shortClips > 0 },
            uiText(Res.string.toast_skipped_captions, result.clipsWithCaptions).takeIf { result.clipsWithCaptions > 0 },
        )
        host.toast(
            when (target) {
                PasteTarget.NEXT -> uiText(Res.string.toast_pasted_next)
                PasteTarget.CURRENT -> uiText(Res.string.toast_pasted_current)
                // 건너뛴 구간 안내는 " · "로 이어 붙인다.
                PasteTarget.ALL -> extras.fold(uiText(Res.string.toast_pasted_all, result.pasted.size)) { text, extra -> uiText(Res.string.joined_with_dot, text, extra) }
            },
        )
    }
}
