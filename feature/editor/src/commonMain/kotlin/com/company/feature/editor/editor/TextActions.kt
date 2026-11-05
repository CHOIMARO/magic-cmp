package com.company.feature.editor.editor

import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextExit
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextLoop
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.ThumbnailTemplate
import com.company.domain.model.editor.addThumbnail
import com.company.domain.model.editor.addText
import com.company.domain.model.editor.afterTextChange
import com.company.domain.model.editor.decorate
import com.company.domain.model.editor.drawnFont
import com.company.domain.model.editor.placedCaptions
import com.company.domain.model.editor.removeCaption
import com.company.domain.model.editor.removeText
import com.company.domain.model.editor.resetTextAnimation
import com.company.domain.model.editor.setTextAnimation
import com.company.domain.model.editor.setTextAnimationLength
import com.company.domain.model.editor.trimWith
import com.company.domain.model.editor.updateCaption
import com.company.domain.model.editor.updateText
import com.company.domain.model.editor.withBackground
import com.company.domain.model.editor.withBackgroundBorder
import com.company.domain.model.editor.withBackgroundShape
import com.company.domain.model.editor.withFont
import com.company.domain.model.editor.withTemplate
import com.company.domain.repository.FontRepository
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.empty_text
import com.company.feature.editor.resources.toast_animation_reset
import com.company.feature.editor.resources.toast_deleted_undoable
import com.company.feature.editor.resources.toast_enter_caption
import com.company.feature.editor.resources.toast_font_failed
import com.company.feature.editor.resources.toast_thumbnail_added
import com.company.ui.editor.nameResource
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
        // 새 글자는 템플릿·꾸미기 없이 흰 프리텐다드 3단계로 시작한다 (9.3).
        val draft = overlay?.let {
            TextDraft(
                id = it.id,
                text = it.text,
                style = it.style,
                font = it.font,
                fontFromTemplate = it.fontFromTemplate,
                sizeStep = it.sizeStep,
                decorations = it.decorations,
            )
        } ?: TextDraft(id = null, text = "")
        host.reduce { copy(sheet = EditorSheet.TEXT, textDraft = draft, colorPicker = null) }
    }

    /**
     * Opens the text sheet for one caption. The sheet shows only the decoration tab.
     *
     * @param captionId ID of the caption.
     */
    fun openCaptionEditor(captionId: String) {
        host.pause()
        val caption = host.state.project.placedCaptions.find { it.id == captionId } ?: return
        val draft = TextDraft(id = null, text = caption.text, decorations = caption.decorations, captionId = caption.id)
        host.reduce { copy(sheet = EditorSheet.TEXT, textDraft = draft, selectedTrackItem = TrackSelection.Caption(caption.id), colorPicker = null) }
    }

    /**
     * Changes the input. The decorations move with the text (see [withText]).
     *
     * @param text New input.
     * @param selectionStart Start of the selected characters.
     * @param selectionEnd End of the selected characters.
     * @param composing True while the keyboard composes the changed characters.
     */
    fun changeText(text: String, selectionStart: Int, selectionEnd: Int, composing: Boolean) {
        host.reduce {
            val draft = textDraft ?: return@reduce this
            copy(
                textDraft = draft.withText(text, composing).copy(
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
            host.reduce { copy(textDraft = textDraft?.let { it.withContent(it.content.withFont(font)) }) }
            return
        }
        host.reduce { copy(fontDownloading = font) }
        host.scope.launch {
            try {
                fontRepository.download(font)
                host.reduce { copy(textDraft = textDraft?.let { it.withContent(it.content.withFont(font)) }) }
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
     * Applies a template, or removes it when [style] is null (9.3, D61). The decorations go away. A template with a
     * font that is not on the device downloads the font. The text uses Pretendard until the font is ready.
     *
     * @param style New template, or null for no template.
     */
    fun changeStyle(style: TextStyleType?) {
        host.reduce {
            copy(textDraft = textDraft?.let { it.withContent(it.content.withTemplate(style)).copy(pendingDecoration = it.pendingDecoration.takeIf { style == null }) })
        }
        host.state.textDraft?.content?.drawnFont?.let { downloadFonts(setOf(it), quiet = false) }
    }

    /**
     * Adds all lines of a thumbnail with their texts, places, and accents, as the design does. The lines start at 0
     * seconds, or at the start of the saved text of the sheet, which they replace. Then the sheet closes. It is one
     * undo step.
     *
     * @param template Thumbnail to add.
     */
    fun addThumbnail(template: ThumbnailTemplate) {
        val draft = host.state.textDraft ?: return
        if (draft.isCaption) return
        val current = draft.id?.let { id -> host.state.project.texts.find { it.id == id } }
        host.projects.commit { it.addThumbnail(template, start = current?.start ?: 0.0, replaceTextId = current?.id) }
        host.reduce { copy(sheet = null, textDraft = null, colorPicker = null, selectedTextId = null) }
        downloadFonts(template.lines.mapTo(mutableSetOf()) { it.style.font }, quiet = false)
        host.toast(uiText(Res.string.toast_thumbnail_added, uiText(template.nameResource)))
    }

    /**
     * Opens a tab of the text sheet. The template tab first downloads the template fonts that are not on the device,
     * so the cards show the right fonts.
     *
     * @param tab Tab to open.
     */
    fun selectTab(tab: TextSheetTab) {
        host.reduce { copy(textDraft = textDraft?.copy(tab = tab)) }
        if (tab == TextSheetTab.TEMPLATE) downloadFonts(TextStyleType.entries.mapTo(mutableSetOf()) { it.font }, quiet = true)
    }

    /**
     * Downloads the fonts that the device does not have, in the background. A text uses Pretendard until its font is
     * ready.
     *
     * @param fonts Fonts that texts use.
     * @param quiet True to show no toast when a download fails.
     */
    fun downloadFonts(fonts: Set<TextFontType>, quiet: Boolean) {
        fonts.filter { it.downloadable && !fontRepository.downloadedFonts.value.containsKey(it) }.forEach { font ->
            host.scope.launch {
                try {
                    fontRepository.download(font)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (!quiet) host.toast(uiText(Res.string.toast_font_failed))
                }
            }
        }
    }

    /**
     * Changes one decoration of the selected characters, or of all characters, or of the characters to type (see
     * [decorated]).
     *
     * @param change New value of one property.
     */
    fun decorate(change: DecorationChange) {
        host.reduce { copy(textDraft = textDraft?.decorated(change)) }
    }

    // =====================================================================
    // Color picker
    // =====================================================================

    /**
     * Opens the color picker at the current color of the selected characters.
     *
     * @param target Text color or background color.
     */
    fun openColorPicker(target: ColorTarget) {
        val draft = host.state.textDraft ?: return
        val values = draft.decorationValues { if (target == ColorTarget.TEXT) it.color else it.background }
        val start = values.singleOrNull() ?: if (target == ColorTarget.TEXT) WHITE else DecorationPalette.first()
        host.reduce { copy(colorPicker = ColorPicker(target, start)) }
    }

    /**
     * Changes the color of the open picker. The draft shows the color at once.
     *
     * @param rgb New color as 0xRRGGBB.
     */
    fun changePickerColor(rgb: Int) {
        val picker = host.state.colorPicker ?: return
        if (picker.changed && picker.rgb == rgb) return
        decorate(if (picker.target == ColorTarget.TEXT) DecorationChange.Color(rgb) else DecorationChange.Background(rgb))
        host.reduce { copy(colorPicker = picker.copy(rgb = rgb, changed = true)) }
    }

    /** Closes the color picker. A changed color goes first in the recent colors. */
    fun closeColorPicker() {
        val picker = host.state.colorPicker ?: return
        host.reduce {
            copy(
                colorPicker = null,
                recentColors = if (picker.changed) (listOf(picker.rgb) + recentColors.filter { it != picker.rgb }).take(RECENT_COLOR_COUNT) else recentColors,
            )
        }
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
            host.reduce { copy(sheet = null, textDraft = null, colorPicker = null) }
            return
        }
        // 빈 칸으로 저장하면 '글자'로 넣고 미리 건 꾸미기를 입힌다 (D76). 아니면 앞뒤 빈칸을 지운 만큼 꾸미기도 당긴다.
        if (text.isEmpty()) {
            val decoration = draft.blankDecoration
            host.scope.launch {
                val placeholder = host.strings.resolve(uiText(Res.string.empty_text))
                commitText(draft, placeholder, decoration.fullRange(placeholder.length))
            }
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
        val content = draft.content.copy(text = text, decorations = decorations)
        val time = host.state.time
        host.projects.commit { project ->
            if (draft.id != null) project.updateText(draft.id, content) else project.addText(content, time).first
        }
        host.reduce { copy(sheet = null, textDraft = null, colorPicker = null) }
    }

    /** Removes the text overlay or the caption of the sheet, then closes the sheet. */
    fun delete() {
        val draft = host.state.textDraft ?: return
        val captionId = draft.captionId
        val textId = draft.id
        when {
            captionId != null -> {
                host.projects.commit { it.removeCaption(captionId) }
                host.reduce { copy(sheet = null, textDraft = null, colorPicker = null, selectedTrackItem = null) }
            }
            textId != null -> {
                host.projects.commit { it.removeText(textId) }
                host.reduce { copy(sheet = null, textDraft = null, colorPicker = null, selectedTextId = selectedTextId.takeIf { it != textId }) }
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
     * Sets the loop of the selected text. The stage plays it while the loop tab is open.
     *
     * @param loop New loop.
     */
    fun pickLoop(loop: TextLoop) {
        val text = host.state.selectedText ?: return
        if ((text.loop ?: TextLoop.NONE) != loop) host.projects.commit { it.setTextAnimation(text.id, loop = loop) }
    }

    /**
     * Sets the length of the animation of the open tab. The first change of a quick series keeps an undo step.
     *
     * @param millis New length in milliseconds.
     */
    fun changeAnimationLength(millis: Int) {
        val text = host.state.selectedText ?: return
        // 반복 애니메이션에는 길이가 없다.
        if (host.state.animationTab == AnimationTab.LOOP) return
        val fresh = lastLengthChange?.let { it.elapsedNow().inWholeMilliseconds > SLIDER_STEP_MILLIS } ?: true
        lastLengthChange = TimeSource.Monotonic.markNow()
        if (fresh) host.projects.saveCheckpoint()
        host.projects.update {
            when (host.state.animationTab) {
                AnimationTab.ENTRANCE -> it.setTextAnimationLength(text.id, entranceMillis = millis)
                AnimationTab.EXIT -> it.setTextAnimationLength(text.id, exitMillis = millis)
                AnimationTab.LOOP -> it
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

/** White as 0xRRGGBB. The color picker starts with it for a text without a color decoration. */
private const val WHITE = 0xFFFFFF

/**
 * Copy with a new input. The decorations move with the text (D76).
 *
 * The first characters in an empty input get [TextDraft.pendingDecoration]. When the user removes all characters, the
 * decoration of the first character becomes the pending decoration.
 *
 * @param text New input.
 * @param composing True while the keyboard composes the changed characters.
 */
internal fun TextDraft.withText(text: String, composing: Boolean): TextDraft = when {
    text.isEmpty() -> copy(text = text, decorations = emptyList(), pendingDecoration = if (this.text.isEmpty()) pendingDecoration else blankDecoration)
    this.text.isEmpty() -> copy(text = text, decorations = pendingDecoration.fullRange(text.length), pendingDecoration = null)
    else -> copy(text = text, decorations = decorations.afterTextChange(this.text, text, composing), pendingDecoration = null)
}

/**
 * Copy with one decoration change of the selected characters, or of all characters. A text with a template loses the
 * template first (9.3). An empty input changes [TextDraft.pendingDecoration] with the same rules (D76).
 *
 * @param change New value of one property.
 */
internal fun TextDraft.decorated(change: DecorationChange): TextDraft {
    if (text.isNotEmpty()) {
        val range = decorationRange
        return withContent(content.decorate(range.first, range.last + 1) { it.with(change) })
    }
    // 빈 입력은 한 칸짜리 문구를 꼾며 보고 그 칸의 꼾미기를 미리 건 꼾미기로 쓴다. 템플릿 규칙(9.3)이 글자를 꼾일 때와 같아진다.
    val probe = content.copy(text = " ", decorations = pendingDecoration.fullRange(1)).decorate(0, 1) { it.with(change) }
    return withContent(probe.copy(decorations = emptyList())).copy(pendingDecoration = probe.decorations.firstOrNull()?.decoration)
}

/** Applies one decoration change. */
private fun Decoration.with(change: DecorationChange): Decoration = when (change) {
    is DecorationChange.Color -> copy(color = change.rgb)
    is DecorationChange.Bold -> copy(bold = change.on)
    is DecorationChange.Outline -> copy(outline = change.on)
    is DecorationChange.Shadow -> copy(shadow = change.on)
    is DecorationChange.Background -> withBackground(change.rgb)
    is DecorationChange.BackgroundShape -> withBackgroundShape(change.shape)
    is DecorationChange.BackgroundBorder -> withBackgroundBorder(change.on)
}
