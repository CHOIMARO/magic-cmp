package com.company.feature.editor.editor

import com.company.core.domain.model.editor.Decoration
import com.company.core.domain.model.editor.DecorationRange
import com.company.core.domain.model.editor.TextContent
import com.company.core.domain.model.editor.TextEntrance
import com.company.core.domain.model.editor.TextExit
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.addText
import com.company.core.domain.model.editor.afterTextChange
import com.company.core.domain.model.editor.decorate
import com.company.core.domain.model.editor.placedCaptions
import com.company.core.domain.model.editor.removeCaption
import com.company.core.domain.model.editor.removeText
import com.company.core.domain.model.editor.resetTextAnimation
import com.company.core.domain.model.editor.setTextAnimation
import com.company.core.domain.model.editor.setTextAnimationLength
import com.company.core.domain.model.editor.trimWith
import com.company.core.domain.model.editor.updateCaption
import com.company.core.domain.model.editor.updateText
import com.company.core.domain.repository.FontRepository
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.empty_text
import com.company.feature.editor.resources.toast_animation_reset
import com.company.feature.editor.resources.toast_deleted_undoable
import com.company.feature.editor.resources.toast_enter_caption
import com.company.feature.editor.resources.toast_font_failed
import com.company.ui.text.uiText
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Number of colors in the recent colors of the color picker. */
private const val RECENT_COLOR_COUNT = 5

/** Changes of a slider within this time, in milliseconds, are one undo step. */
internal const val SLIDER_STEP_MILLIS = 600L

/**
 * Text sheet (F01, F02, F03), caption text changes, and the animation sheet (F09).
 *
 * @param host Editor state and project.
 * @param fontRepository Downloads the fonts that the app does not include.
 */
internal class TextActions(private val host: EditorHost, private val fontRepository: FontRepository) {
    /** Time of the last change of the animation length. A quick series of changes is one undo step. */
    private var lastLengthChange: TimeMark? = null

    // =====================================================================
    // Text sheet
    // =====================================================================

    /**
     * Opens the text sheet for a text overlay.
     *
     * @param textId ID of the overlay, or null for a new overlay.
     */
    fun openTextEditor(textId: String?) {
        host.pause()
        val overlay = host.state.project.texts.find { it.id == textId }
        val draft = overlay?.let {
            TextDraft(id = it.id, text = it.text, style = it.style, font = it.font, sizeStep = it.sizeStep, decorations = it.decorations)
        } ?: TextDraft(id = null, text = "", style = TextStyleType.BOLD)
        host.reduce { copy(sheet = EditorSheet.TEXT, textDraft = draft) }
    }

    /**
     * Opens the text sheet for one caption. The sheet shows only the decoration tab.
     *
     * @param captionId ID of the caption.
     */
    fun openCaptionEditor(captionId: String) {
        host.pause()
        val caption = host.state.project.placedCaptions.find { it.id == captionId } ?: return
        val draft = TextDraft(id = null, text = caption.text, style = TextStyleType.BOLD, decorations = caption.decorations, captionId = caption.id)
        host.reduce { copy(sheet = EditorSheet.TEXT, textDraft = draft, selectedTrackItem = TrackSelection.Caption(caption.id)) }
    }

    /**
     * Changes the input. The decorations move with the text.
     *
     * @param text New input.
     * @param selectionStart Start of the selected characters.
     * @param selectionEnd End of the selected characters.
     */
    fun changeText(text: String, selectionStart: Int, selectionEnd: Int) {
        host.reduce {
            val draft = textDraft ?: return@reduce this
            copy(
                textDraft = draft.copy(
                    text = text,
                    decorations = draft.decorations.afterTextChange(draft.text, text),
                    selectionStart = minOf(selectionStart, selectionEnd).coerceIn(0, text.length),
                    selectionEnd = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length),
                ),
            )
        }
    }

    /**
     * Changes the font. A font that is not on the device downloads first. During the download, the font list shows
     * a progress mark, and the draft keeps the old font. A failed download keeps the old font.
     *
     * @param font New font.
     */
    fun changeFont(font: TextFontType) {
        if (host.state.fontDownloading != null) return
        if (!font.downloadable || fontRepository.downloadedFonts.value.containsKey(font)) {
            host.reduce { copy(textDraft = textDraft?.copy(font = font)) }
            return
        }
        host.reduce { copy(fontDownloading = font) }
        host.scope.launch {
            try {
                fontRepository.download(font)
                host.reduce { copy(textDraft = textDraft?.copy(font = font)) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                host.toast(uiText(Res.string.toast_font_failed))
            } finally {
                host.reduce { copy(fontDownloading = null) }
            }
        }
    }

    /**
     * Changes one decoration of the selected characters, or of all characters.
     *
     * @param change New value of one property.
     */
    fun decorate(change: DecorationChange) {
        host.reduce {
            val draft = textDraft ?: return@reduce this
            if (draft.text.isEmpty()) return@reduce this
            val range = draft.decorationRange
            val decorations = draft.decorations.decorate(draft.text.length, range.first, range.last + 1) { it.with(change) }
            copy(textDraft = draft.copy(decorations = decorations))
        }
    }

    /**
     * Sets a color from the color picker, and keeps the color first in the recent colors.
     *
     * @param rgb Color as 0xRRGGBB.
     */
    fun pickCustomColor(rgb: Int) {
        decorate(DecorationChange.Color(rgb))
        host.reduce { copy(recentColors = (listOf(rgb) + recentColors.filter { it != rgb }).take(RECENT_COLOR_COUNT)) }
    }

    /** Adds or changes the text overlay or the caption of the sheet, then closes the sheet. */
    fun save() {
        val draft = host.state.textDraft ?: return
        val (text, decorations) = draft.decorations.trimWith(draft.text)
        val captionId = draft.captionId
        if (captionId != null) {
            if (text.isEmpty()) {
                host.toast(uiText(Res.string.toast_enter_caption))
                return
            }
            host.projects.commit { it.updateCaption(captionId, text, decorations) }
            host.reduce { copy(sheet = null, textDraft = null) }
            return
        }
        // 빈 칸으로 저장하면 '글자'로 넣고, 앞뒤 빈칸을 지운 만큼 꾸미기도 당긴다.
        if (text.isEmpty()) {
            host.scope.launch { commitText(draft, host.strings.resolve(uiText(Res.string.empty_text)), emptyList()) }
        } else {
            commitText(draft, text, decorations)
        }
    }

    /**
     * Adds or changes the text overlay of the draft, then closes the sheet.
     *
     * @param draft Draft of the sheet.
     * @param text Text to save.
     * @param decorations Decorations of [text].
     */
    private fun commitText(draft: TextDraft, text: String, decorations: List<DecorationRange>) {
        val content = TextContent(text = text, style = draft.style, font = draft.font, sizeStep = draft.sizeStep, decorations = decorations)
        val time = host.state.time
        host.projects.commit { project ->
            if (draft.id != null) project.updateText(draft.id, content) else project.addText(content, time).first
        }
        host.reduce { copy(sheet = null, textDraft = null) }
    }

    /** Removes the text overlay or the caption of the sheet, then closes the sheet. */
    fun delete() {
        val draft = host.state.textDraft ?: return
        val captionId = draft.captionId
        val textId = draft.id
        when {
            captionId != null -> {
                host.projects.commit { it.removeCaption(captionId) }
                host.reduce { copy(sheet = null, textDraft = null, selectedTrackItem = null) }
            }
            textId != null -> {
                host.projects.commit { it.removeText(textId) }
                host.reduce { copy(sheet = null, textDraft = null, selectedTextId = selectedTextId.takeIf { it != textId }) }
            }
            else -> return
        }
        host.toast(uiText(Res.string.toast_deleted_undoable))
    }

    // =====================================================================
    // Animation sheet
    // =====================================================================

    /** Opens the animation sheet of the selected text. */
    fun openAnimationSheet() {
        if (host.state.selectedText == null) return
        host.pause()
        host.reduce { copy(sheet = EditorSheet.ANIMATION, animationTab = AnimationTab.ENTRANCE) }
    }

    /**
     * Sets the entrance of the selected text, and plays it once on the stage.
     *
     * @param entrance New entrance.
     */
    fun pickEntrance(entrance: TextEntrance) {
        val text = host.state.selectedText ?: return
        if (text.entrance != entrance) host.projects.commit { it.setTextAnimation(text.id, entrance = entrance) }
        host.reduce { copy(animationReplay = animationReplay + 1) }
    }

    /**
     * Sets the exit of the selected text, and plays it once on the stage.
     *
     * @param exit New exit.
     */
    fun pickExit(exit: TextExit) {
        val text = host.state.selectedText ?: return
        if (text.exit != exit) host.projects.commit { it.setTextAnimation(text.id, exit = exit) }
        host.reduce { copy(animationReplay = animationReplay + 1) }
    }

    /**
     * Sets the length of the animation of the open tab. The first change of a quick series keeps an undo step.
     *
     * @param millis New length in milliseconds.
     */
    fun changeAnimationLength(millis: Int) {
        val text = host.state.selectedText ?: return
        val fresh = lastLengthChange?.let { it.elapsedNow().inWholeMilliseconds > SLIDER_STEP_MILLIS } ?: true
        lastLengthChange = TimeSource.Monotonic.markNow()
        if (fresh) host.projects.saveCheckpoint()
        host.projects.update {
            when (host.state.animationTab) {
                AnimationTab.ENTRANCE -> it.setTextAnimationLength(text.id, entranceMillis = millis)
                AnimationTab.EXIT -> it.setTextAnimationLength(text.id, exitMillis = millis)
            }
        }
        host.reduce { copy(animationReplay = animationReplay + 1) }
    }

    /** The selected text uses the animations of its style again. */
    fun resetAnimation() {
        val text = host.state.selectedText ?: return
        host.projects.commit { it.resetTextAnimation(text.id) }
        host.reduce { copy(animationReplay = animationReplay + 1) }
        host.toast(uiText(Res.string.toast_animation_reset))
    }
}

/** Applies one decoration change. */
private fun Decoration.with(change: DecorationChange): Decoration = when (change) {
    is DecorationChange.Color -> copy(color = change.rgb)
    is DecorationChange.Bold -> copy(bold = change.on)
    is DecorationChange.Outline -> copy(outline = change.on)
    is DecorationChange.Shadow -> copy(shadow = change.on)
    is DecorationChange.Background -> copy(background = change.rgb)
}
