package com.company.feature.editor.editor

import com.company.core.domain.model.editor.CopiedElement
import com.company.core.domain.model.editor.PasteTarget
import com.company.core.domain.model.editor.captionsInClip
import com.company.core.domain.model.editor.copyElement
import com.company.core.domain.model.editor.paste
import com.company.core.domain.model.editor.pasteOptions

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
        host.toast("복사했어요")
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
            host.toast(if (target == PasteTarget.NEXT) "붙일 다음 구간이 없어요" else "붙일 구간이 없어요")
            return
        }
        val single = indexes.singleOrNull()
        if (clipboard.element is CopiedElement.Caption && single != null && state.project.captionsInClip(single).isNotEmpty()) {
            host.reduce {
                copy(dialog = EditorDialog("이 구간 자막을 바꿀까요?", "이 구간에 있는 자막을 지우고 복사한 자막을 넣어요", "바꾸기", DialogAction.PasteReplacingCaption(target)))
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
                    result.shortClips > 0 && indexes.size > 1 -> "${result.shortClips}개 구간은 짧아서 건너뛰었어요"
                    result.shortClips > 0 -> "구간이 짧아서 붙일 수 없어요"
                    else -> "자막이 있는 구간은 건너뛰었어요"
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
            "${result.shortClips}개 구간은 짧아서 건너뛰었어요".takeIf { result.shortClips > 0 },
            "${result.clipsWithCaptions}개 구간은 자막이 있어서 건너뛰었어요".takeIf { result.clipsWithCaptions > 0 },
        ).joinToString(" · ")
        host.toast(
            when (target) {
                PasteTarget.NEXT -> "다음 구간에 붙였어요"
                PasteTarget.CURRENT -> "지금 구간에 붙였어요"
                PasteTarget.ALL -> "구간 ${result.pasted.size}개에 붙였어요" + if (extras.isNotEmpty()) " · $extras" else ""
            },
        )
    }
}
