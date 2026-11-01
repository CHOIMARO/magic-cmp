package com.company.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.ThumbnailTemplate

// =====================================================================
// Looks of the thumbnail lines (the "릴스 썸네일 템플릿" of the design)
//
// The values come from THUMB_TPL of the design, in pixels of the 1080 × 1920 canvas. The outline width is the full
// stroke width: half of it shows outside the letters. Shadows, lights, and stickers grow with the font size.
// =====================================================================

/**
 * Shadow or light of a thumbnail line, in canvas pixels.
 *
 * @property x Horizontal offset.
 * @property y Vertical offset.
 * @property blur Blur radius. 0 gives a hard shadow.
 * @property color Color of the shadow.
 */
private class CanvasShadow(val x: Int, val y: Int, val blur: Int, val color: Color)

/**
 * Sticker of a thumbnail line, in canvas pixels.
 *
 * @property shape Shape of the sticker.
 * @property dx Horizontal distance of the sticker center from the center of the line.
 * @property dy Vertical distance of the sticker center from the center of the line.
 * @property width Width of the sticker.
 * @property height Height of the sticker.
 * @property fill Fill color.
 * @property stroke Outline color, or null.
 * @property strokeWidth Outline width in the units of the star shape (a 100 × 100 box).
 */
private class CanvasSticker(
    val shape: StickerShape,
    val dx: Int,
    val dy: Int,
    val width: Int,
    val height: Int = width,
    val fill: Color = Color.White,
    val stroke: Color? = null,
    val strokeWidth: Float = 0f,
)

/**
 * Look of one thumbnail line, in canvas pixels.
 *
 * @property size Font size.
 * @property color Text color.
 * @property weight Font weight.
 * @property outline Full width of the outline stroke, or 0 for no outline.
 * @property outlineColor Color of the outline.
 * @property shadows Text shadows, the first on top.
 * @property glow Light around the outlined letters, or null.
 * @property background Color of the box behind the text, or null.
 * @property padV Vertical padding of the box.
 * @property padH Horizontal padding of the box.
 * @property radius Corner radius of the box.
 * @property lineHeight Line height, as a part of the font size.
 * @property letterSpacing Letter spacing.
 * @property rotation Angle in degrees. A positive value turns the text clockwise.
 * @property scaleY Vertical stretch.
 * @property italic True for slanted letters.
 * @property stickers Stickers behind the letters.
 */
private class ThumbSpec(
    val size: Int,
    val color: Color,
    val weight: Int = 400,
    val outline: Int = 0,
    val outlineColor: Color = Color.Black,
    val shadows: List<CanvasShadow> = emptyList(),
    val glow: CanvasShadow? = null,
    val background: Color? = null,
    val padV: Int = 0,
    val padH: Int = 0,
    val radius: Int = 0,
    val lineHeight: Float = DEFAULT_LINE_HEIGHT_EM,
    val letterSpacing: Int = 0,
    val rotation: Float = 0f,
    val scaleY: Float = 1f,
    val italic: Boolean = false,
    val stickers: List<CanvasSticker> = emptyList(),
)

private val Pink = Color(0xFFF7B3CF)
private val StarYellow = Color(0xFFFFE45C)
private val StarOutline = Color(0xFF2E2A3A)
private val Sparkle = Color(0xFFFFF3A0)

/** Pink star of the fire thumbnail. */
private fun pinkStar(dx: Int, dy: Int, width: Int) = CanvasSticker(StickerShape.STAR, dx, dy, width, fill = Pink, stroke = Pink, strokeWidth = 8f)

/** Yellow star with a dark outline of the bubble thumbnail. */
private fun yellowStar(dx: Int, dy: Int, width: Int) = CanvasSticker(StickerShape.STAR, dx, dy, width, fill = StarYellow, stroke = StarOutline, strokeWidth = 5f)

/** Sparkle of the pixel thumbnail. */
private fun sparkle(dx: Int, dy: Int, width: Int) = CanvasSticker(StickerShape.SPARK, dx, dy, width, fill = Sparkle)

private val SoftShadow = CanvasShadow(0, 1, 8, Color.Black.copy(alpha = 0.25f))
private val SmallSoftShadow = CanvasShadow(0, 1, 6, Color.Black.copy(alpha = 0.25f))
private val BoldShadow = CanvasShadow(0, 4, 12, Color.Black.copy(alpha = 0.35f))
private val FireSolidShadow = CanvasShadow(10, 12, 0, Color(0xFF5B4250))
private val BubbleLight = CanvasShadow(0, 0, 4, Color.White)

/** Looks of the thumbnail lines. The sticker places are the design places less the center of the line. */
private val ThumbSpecs: Map<TextStyleType, ThumbSpec> = mapOf(
    TextStyleType.THUMB_FIRE_1 to ThumbSpec(
        size = 96, color = Color.White, outline = 10, outlineColor = Color(0xFF4A2310),
        shadows = listOf(CanvasShadow(0, 0, 24, Color(0xFFFF5A00))),
        stickers = listOf(CanvasSticker(StickerShape.FLAME, 0, 10, 820, 220), pinkStar(-463, 95, 110), pinkStar(460, 72, 100)),
    ),
    TextStyleType.THUMB_FIRE_2 to ThumbSpec(size = 168, color = Color(0xFFF5A8C8), outline = 16, outlineColor = Color.White, shadows = listOf(FireSolidShadow)),
    TextStyleType.THUMB_FIRE_3 to ThumbSpec(
        size = 168, color = Color(0xFFF5A8C8), outline = 16, outlineColor = Color.White, shadows = listOf(FireSolidShadow),
        stickers = listOf(pinkStar(1, 164, 92)),
    ),
    TextStyleType.THUMB_FIRE_4 to ThumbSpec(
        size = 64, color = Color(0xFF1E1E1E), weight = 700, background = Color.White, padV = 18, padH = 40, radius = 16, lineHeight = 1f,
    ),
    TextStyleType.THUMB_BUBBLE_1 to ThumbSpec(
        size = 220, color = Color(0xFFFFF27A), outline = 12, outlineColor = StarOutline, glow = BubbleLight,
        stickers = listOf(yellowStar(-380, 13, 96), yellowStar(354, -38, 64)),
    ),
    TextStyleType.THUMB_BUBBLE_2 to ThumbSpec(size = 200, color = Color(0xFFFFF27A), outline = 12, outlineColor = StarOutline, glow = BubbleLight),
    TextStyleType.THUMB_BUBBLE_3 to ThumbSpec(
        size = 168, color = Color(0xFFEC5A72), outline = 14, outlineColor = Color.White, glow = CanvasShadow(0, 0, 2, Color(0xFF3B2B33)),
        stickers = listOf(yellowStar(-218, 178, 84)),
    ),
    TextStyleType.THUMB_HAND_1 to ThumbSpec(
        size = 250, color = Color(0xFFFFF24D), outline = 14, outlineColor = Color(0xFF111111), letterSpacing = 6, rotation = -4f, scaleY = 1.45f,
    ),
    TextStyleType.THUMB_HAND_2 to ThumbSpec(
        size = 300, color = Color(0xFFFFF24D), outline = 16, outlineColor = Color(0xFF111111), letterSpacing = -8, rotation = 1f, scaleY = 1.6f,
    ),
    TextStyleType.THUMB_HAND_3 to ThumbSpec(
        size = 310, color = Color(0xFFFFF24D), outline = 16, outlineColor = Color(0xFF111111), letterSpacing = 4, rotation = 3f, scaleY = 1.5f,
    ),
    TextStyleType.THUMB_THIN_1 to ThumbSpec(
        size = 44, color = Color(0xFF1D3A28), weight = 500, background = Color(0xFFE3F6E4), padV = 8, padH = 20, lineHeight = 1.1f,
        stickers = listOf(CanvasSticker(StickerShape.BURST, -174, 2, 40)),
    ),
    TextStyleType.THUMB_THIN_2 to ThumbSpec(size = 48, color = Color.White, shadows = listOf(SoftShadow)),
    TextStyleType.THUMB_THIN_3 to ThumbSpec(size = 108, color = Color.White, weight = 300, letterSpacing = -5, shadows = listOf(SoftShadow)),
    TextStyleType.THUMB_THIN_4 to ThumbSpec(size = 36, color = Color.White, weight = 500, shadows = listOf(SmallSoftShadow)),
    TextStyleType.THUMB_THIN_5 to ThumbSpec(
        size = 28, color = Color.White, italic = true, shadows = listOf(SmallSoftShadow),
        stickers = listOf(CanvasSticker(StickerShape.BAR, 272, 9, 110, 2)),
    ),
    TextStyleType.THUMB_BOLD_1 to ThumbSpec(
        size = 46, color = Color(0xFF1A1A1A), weight = 600, background = Color(0xFFFBF0A8), padV = 10, padH = 22, lineHeight = 1.1f,
    ),
    TextStyleType.THUMB_BOLD_2 to ThumbSpec(size = 118, color = Color.White, outline = 8, outlineColor = Color(0xFF262626), shadows = listOf(BoldShadow)),
    TextStyleType.THUMB_BOLD_3 to ThumbSpec(size = 118, color = Color.White, outline = 8, outlineColor = Color(0xFF262626), shadows = listOf(BoldShadow)),
    TextStyleType.THUMB_BOLD_4 to ThumbSpec(size = 118, color = Color(0xFFFFF09C), outline = 8, outlineColor = Color(0xFF262626), shadows = listOf(BoldShadow)),
    TextStyleType.THUMB_PIXEL_1 to ThumbSpec(
        size = 42, color = Color(0xFF111111), weight = 500, background = Color(0xFFA8E6EF), padV = 8, padH = 22, lineHeight = 1.1f,
    ),
    TextStyleType.THUMB_PIXEL_2 to ThumbSpec(
        size = 96, color = Color(0xFFFFF6C4), outline = 7, outlineColor = Color(0xFF1F2A48), stickers = listOf(sparkle(95, -45, 26), sparkle(263, 13, 22)),
    ),
    TextStyleType.THUMB_PIXEL_3 to ThumbSpec(
        size = 96, color = Color(0xFFFFF6C4), outline = 7, outlineColor = Color(0xFF1F2A48), stickers = listOf(sparkle(-338, -13, 30), sparkle(-161, 104, 24)),
    ),
    TextStyleType.THUMB_PIXEL_4 to ThumbSpec(
        size = 82, color = Color(0xFF49B8E8), outline = 7, outlineColor = Color(0xFF16233F), stickers = listOf(sparkle(269, 53, 28), sparkle(-14, 70, 22)),
    ),
)

/** Animation of a thumbnail line: no entrance, so the title shows from the first frame. The exit is a fade. */
private val ThumbnailAnimation = TextAnimation(TextEntrance.NONE, entranceMillis = 0, exitMillis = DEFAULT_TITLE_EXIT_MILLIS)

/** Background of the template cards of each thumbnail. */
val ThumbnailTemplate.cardBackground: Color
    get() = when (this) {
        ThumbnailTemplate.FIRE -> Color(0xFF8D939A)
        ThumbnailTemplate.BUBBLE -> Color(0xFF77806F)
        ThumbnailTemplate.HAND -> Color(0xFF8B857E)
        ThumbnailTemplate.THIN -> Color(0xFFA9A297)
        ThumbnailTemplate.BOLD -> Color(0xFF968F84)
        ThumbnailTemplate.PIXEL -> Color(0xFFD9D7D3)
    }

/**
 * Look of a thumbnail line at size step 3 and scale 1.
 *
 * The widest text box keeps the text of the design on one line. A longer text breaks into lines with the overflow rule.
 *
 * @param style A thumbnail line.
 * @param base Look that gives the values that the design does not set.
 */
internal fun thumbnailLook(style: TextStyleType, base: TextLook): TextLook {
    val spec = ThumbSpecs.getValue(style)
    val size = spec.size.toFloat()
    val fontSize = size * DP_PER_SPEC_PX
    val letterSpacingEm = spec.letterSpacing / size
    val paddingHorizontal = spec.padH * DP_PER_SPEC_PX
    val text = ThumbnailTemplate.lineOf(style)?.text.orEmpty()
    return base.copy(
        color = spec.color,
        background = spec.background ?: Color.Transparent,
        fontSize = fontSize,
        fontWeight = FontWeight(spec.weight),
        shadows = spec.shadows.map { LookShadow(it.color, offsetY = it.y / size, blur = it.blur / size, offsetX = it.x / size, em = true) },
        shadowsUnderOutline = spec.outline > 0,
        outline = spec.outline > 0,
        outlineEm = spec.outline / 2f / size,
        outlineColor = spec.outlineColor,
        glow = spec.glow?.let { LookGlow(it.color, blurEm = it.blur / size) },
        paddingVertical = spec.padV * DP_PER_SPEC_PX,
        paddingHorizontal = paddingHorizontal,
        cornerRadius = spec.radius * DP_PER_SPEC_PX,
        letterSpacingEm = letterSpacingEm,
        rotation = spec.rotation,
        scaleY = spec.scaleY,
        italic = spec.italic,
        lineHeightEm = spec.lineHeight,
        boxWidthRatio = oneLineBoxRatio(text, fontSize, style.font, letterSpacingEm, paddingHorizontal),
        stickers = spec.stickers.map {
            LookSticker(
                shape = it.shape,
                centerXEm = it.dx / size,
                centerYEm = it.dy / size,
                widthEm = it.width / size,
                heightEm = it.height / size,
                fill = it.fill,
                stroke = it.stroke,
                strokeRatio = it.strokeWidth / 100f,
            )
        },
        animation = ThumbnailAnimation,
    )
}
