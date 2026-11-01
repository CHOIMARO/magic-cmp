package com.company.domain.model.editor

// =====================================================================
// Text templates and decorations (9.3, D61, D63)
//
// A text has a template or decorations, not both. The text keeps the last one that the user picked.
// The text, the font, the size step, the place, the time, and the picked animations stay in both cases.
// =====================================================================

/** Font that draws the text: the picked font, or the font of the template, or Pretendard. */
val TextContent.drawnFont: TextFontType
    get() = font ?: style?.font ?: TextFontType.PRETENDARD

/** Font that draws the text: the picked font, or the font of the template, or Pretendard. */
val TextOverlay.drawnFont: TextFontType
    get() = font ?: style?.font ?: TextFontType.PRETENDARD

/**
 * Applies a text template, or removes it when [template] is null.
 *
 * A template removes all decorations. A font that the user picked stays. A font that a removed template left
 * (see [TextContent.fontFromTemplate]) goes away, so the new template uses its own font (D61). The size step stays (D63).
 *
 * @param template New template, or null for no template.
 */
fun TextContent.withTemplate(template: TextStyleType?): TextContent = copy(
    style = template,
    decorations = if (template != null) emptyList() else decorations,
    font = if (fontFromTemplate) null else font,
    fontFromTemplate = false,
)

/**
 * Changes the decorations of the characters from [start] to [end].
 *
 * A text with a template loses the template first, with all its effects (9.3). The text then keeps the font of the
 * template, and only the new decoration applies. A change that gives no decoration keeps the template.
 *
 * @param start Index of the first character to change.
 * @param end Index after the last character to change.
 * @param change Makes the new decoration from the old decoration. An empty decoration is removed.
 */
fun TextContent.decorate(start: Int, end: Int, change: (Decoration) -> Decoration): TextContent {
    val template = style ?: return copy(decorations = decorations.decorate(text.length, start, end, change))
    val changed = emptyList<DecorationRange>().decorate(text.length, start, end, change)
    if (changed.isEmpty()) return this
    return copy(style = null, decorations = changed, font = font ?: template.font, fontFromTemplate = font == null)
}

/**
 * Changes the font. The template and the decorations stay.
 *
 * @param font Font that the user picked.
 */
fun TextContent.withFont(font: TextFontType): TextContent = copy(font = font, fontFromTemplate = false)

/**
 * Copy with a new background color. No background also sets the box shape and the box border back to the defaults.
 *
 * @param rgb Background color as 0xRRGGBB, or null for no background.
 */
fun Decoration.withBackground(rgb: Int?): Decoration =
    if (rgb == null) copy(background = null, backgroundShape = BackgroundShape.ROUNDED, backgroundBorder = false) else copy(background = rgb)

/**
 * Copy with a new box shape. A character without a background does not change.
 *
 * @param shape New shape of the background box.
 */
fun Decoration.withBackgroundShape(shape: BackgroundShape): Decoration = if (background == null) this else copy(backgroundShape = shape)

/**
 * Copy with the box border on or off. A character without a background does not change.
 *
 * @param on True to draw the box border.
 */
fun Decoration.withBackgroundBorder(on: Boolean): Decoration = if (background == null) this else copy(backgroundBorder = on)
