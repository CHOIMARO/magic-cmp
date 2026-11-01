package com.company.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.domain.model.editor.BackgroundShape
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.DEFAULT_SIZE_STEP
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextLoop
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.perCharacter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * Shadow behind text. A blur of 0 gives a hard shadow with sharp edges.
 *
 * @property color Color of the shadow.
 * @property offsetY Vertical offset in dp.
 * @property blur Blur radius in dp.
 * @property offsetX Horizontal offset in dp.
 */
@Immutable
data class LookShadow(val color: Color, val offsetY: Float, val blur: Float, val offsetX: Float = 0f)

/**
 * Band of color on the inner edge of the letters.
 *
 * @property color Color of the band. Its alpha is the strength of the band.
 * @property widthEm Width of the band, as a part of the font size.
 * @property bottomOnly True to draw the band only on the bottom edges of the letters.
 */
@Immutable
data class LookEdge(val color: Color, val widthEm: Float, val bottomOnly: Boolean = false)

/**
 * Vertical gradient of the letters. Each line gets the full gradient, from the top to the bottom of its letters.
 *
 * @property stops Colors at positions from 0 (top of the letters) to 1 (bottom of the letters).
 * @property edge Band on the inner edge of the letters, or null.
 */
@Immutable
data class LookFill(val stops: List<Pair<Float, Color>>, val edge: LookEdge? = null)

/**
 * Soft light around the outline.
 *
 * @property color Color of the light. Its alpha is the strength of the light.
 * @property blurEm Blur radius, as a part of the font size.
 */
@Immutable
data class LookGlow(val color: Color, val blurEm: Float)

/** Shape of a [LookSticker]. */
enum class StickerShape {
    /** STAR : A star with five points, a fill, and an outline. */
    STAR,

    /** SPARK : A thin star with four points and a fill. */
    SPARK,
}

/**
 * Small shape next to the letters of a thumbnail look. It moves and grows with the text box.
 *
 * The sizes are parts of the font size, so the sticker grows with the size step.
 *
 * @property shape Shape of the sticker.
 * @property anchorX Horizontal anchor, as a part of the text box width. 0 is the left edge, and 1 is the right edge.
 * @property offsetXEm Horizontal distance from the anchor.
 * @property offsetYEm Vertical distance from the middle of the text box. A positive value is lower.
 * @property sizeEm Width and height of the sticker.
 * @property fill Fill color.
 * @property stroke Outline color, or null for no outline.
 * @property strokeRatio Width of the outline, as a part of the sticker size.
 */
@Immutable
data class LookSticker(
    val shape: StickerShape,
    val anchorX: Float,
    val offsetXEm: Float,
    val offsetYEm: Float,
    val sizeEm: Float,
    val fill: Color,
    val stroke: Color? = null,
    val strokeRatio: Float = 0f,
)

/**
 * Visual style of overlay text or caption text.
 *
 * Sizes are in dp and sp at scale 1. A preview uses a smaller scale.
 *
 * @property color Text color. For captions it is the color of the unread words.
 * @property highlightColor Color of the words that the speaker already said. Only captions use it.
 * @property background Background of the text box.
 * @property fontSize Font size in sp.
 * @property fontWeight Font weight.
 * @property font Font of the text.
 * @property shadows Shadows. The first shadow is on top.
 * @property outline True to draw an outline around the letters.
 * @property outlineColor Color of the outline.
 * @property outlineWidth Width of the outline stroke in dp at font scale 1.
 * @property paddingHorizontal Horizontal padding of the text box.
 * @property paddingVertical Vertical padding of the text box.
 * @property cornerRadius Corner radius of the text box in dp.
 * @property letterSpacingEm Letter spacing in em.
 * @property rotation Rotation of the text box in degrees. A negative value turns it counterclockwise.
 * @property textAlign Alignment of the lines in the text box.
 * @property boxWidthRatio Widest text box, as a part of the stage width.
 * @property bottomPercent Distance of a caption from the bottom, in percent of the screen height.
 * @property maxLines Largest number of lines. Longer texts get smaller.
 * @property maxCharsPerLine Largest number of characters on one line, without spaces.
 * @property highlight How a caption shows the current word.
 * @property revealByWord True when caption words come on screen one at a time.
 * @property animation Entrance and exit of the text.
 * @property widthFactor Factor of the average letter width. Bold and outline decorations make the letters wider,
 *   so fewer letters fit on a line.
 * @property outlineEm Width of the outline outside the letters, as a part of the font size. 0 uses [outlineWidth].
 * @property extrude Offset of a solid 3D shadow behind the outline, as parts of the font size, or null.
 * @property extrudeColor Color of the 3D shadow.
 * @property glow Soft light around the outline, or null.
 * @property fill Gradient of the letters, or null for [color].
 * @property firstWordFill Gradient of the first word, or null for [fill].
 * @property skewX Slant of the letters. A negative value moves the tops of the letters to the right.
 * @property scaleY Vertical stretch of the text box. 1 keeps the letters as they are.
 * @property stickers Small shapes next to the letters. They draw behind the letters.
 * @property lastWordColor Color of the last word of a text with two or more words, or null for [color].
 */
@Immutable
data class TextLook(
    val color: Color = Color.White,
    val highlightColor: Color = Color.White,
    val background: Color = Color.Transparent,
    val fontSize: Float,
    val fontWeight: FontWeight,
    val font: TextFontType = TextFontType.PRETENDARD,
    val shadows: List<LookShadow> = emptyList(),
    val outline: Boolean = false,
    val outlineColor: Color = Color.Black,
    val outlineWidth: Float = 3f,
    val paddingHorizontal: Float = 0f,
    val paddingVertical: Float = 0f,
    val cornerRadius: Float = 8f,
    val letterSpacingEm: Float = 0f,
    val rotation: Float = 0f,
    val textAlign: TextAlign = TextAlign.Center,
    val boxWidthRatio: Float = 0.84f,
    val bottomPercent: Float = 17f,
    val maxLines: Int = 2,
    val maxCharsPerLine: Int = Int.MAX_VALUE,
    val highlight: CaptionHighlight = CaptionHighlight.NONE,
    val revealByWord: Boolean = false,
    val animation: TextAnimation = TextAnimation(TextEntrance.FADE, entranceMillis = 150, exitMillis = 150),
    val widthFactor: Float = 1f,
    val outlineEm: Float = 0f,
    val extrude: Offset? = null,
    val extrudeColor: Color = Color.Black,
    val glow: LookGlow? = null,
    val fill: LookFill? = null,
    val firstWordFill: LookFill? = null,
    val skewX: Float = 0f,
    val scaleY: Float = 1f,
    val stickers: List<LookSticker> = emptyList(),
    val lastWordColor: Color? = null,
) {
    /** Copy with the sizes multiplied by [scale]. Previews use it. */
    internal fun scaled(scale: Float): TextLook = if (scale == 1f) this else copy(
        fontSize = fontSize * scale,
        paddingHorizontal = paddingHorizontal * scale,
        paddingVertical = paddingVertical * scale,
    )

    /** Copy with [maxCharsPerLine] calculated from the sizes at scale 1. */
    internal fun withLineLimit(): TextLook = copy(maxCharsPerLine = charactersPerLine())
}

private val DefaultTextShadow = LookShadow(Color.Black.copy(alpha = 0.5f), offsetY = 2f, blur = 12f)
private val NeonText = Color(0xFFFFDBFF)
private val NeonGlowNear = Color(0xFFE660FF)
private val NeonGlowFar = Color(0xFFD852F5)
private val MarkerYellow = Color(0xFFFDDC5B)
private val PopYellow = Color(0xFFFBD35F)
private val ImpactYellow = Color(0xFFFFE14D)

/** Font size of a text without a template: 80 px on the 1080 canvas (D60). */
private const val PLAIN_FONT_SIZE = 80 * DP_PER_SPEC_PX

// Thumbnail looks (D64). Each one is the largest line of a thumbnail in the design. The design gives the sizes in
// pixels of the 1080 canvas: the font size, the outline width (half of it shows outside the letters), and the shadows.

/** Stars of the yellow bubble look. They sit outside the left and the right edges of the text. */
private val BubbleStars = listOf(
    LookSticker(StickerShape.STAR, anchorX = 0f, offsetXEm = -0.42f, offsetYEm = 0.06f, sizeEm = 0.44f, fill = Color(0xFFFFE45C), stroke = Color(0xFF2E2A3A), strokeRatio = 0.05f),
    LookSticker(StickerShape.STAR, anchorX = 1f, offsetXEm = 0.3f, offsetYEm = -0.17f, sizeEm = 0.29f, fill = Color(0xFFFFE45C), stroke = Color(0xFF2E2A3A), strokeRatio = 0.05f),
)

/** Sparkles of the pixel handwriting look. They sit on the top and the right part of the text. */
private val PixelSparks = listOf(
    LookSticker(StickerShape.SPARK, anchorX = 0.67f, offsetXEm = 0f, offsetYEm = -0.47f, sizeEm = 0.27f, fill = Color(0xFFFFF3A0)),
    LookSticker(StickerShape.SPARK, anchorX = 0.97f, offsetXEm = 0f, offsetYEm = 0.14f, sizeEm = 0.23f, fill = Color(0xFFFFF3A0)),
)

/** Animation of a thumbnail look: no entrance, so the title shows from the first frame. */
private val ThumbnailAnimation = TextAnimation(TextEntrance.NONE, entranceMillis = 0, exitMillis = TITLE_EXIT_MILLIS)

/** Animation of a text without a template: no entrance, and a fade exit (D60). */
private val PlainAnimation = TextAnimation(TextEntrance.NONE, entranceMillis = 0, exitMillis = TITLE_EXIT_MILLIS)

/** Most steps of a 3D shadow. One step is about one pixel. */
private const val MAX_EXTRUDE_STEPS = 24

/** Fade-out length of a title, from the spec. */
private const val TITLE_EXIT_MILLIS = 200

/** Fade-out length of a caption, from the spec. */
private const val CAPTION_EXIT_MILLIS = 150

/** Size factors of the five text size steps (F01). Step 3 is the size of the style. */
val SizeStepFactors: List<Float> = listOf(0.6f, 0.8f, 1f, 1.25f, 1.5f)

/** Extra letter width of bold decorations, as a part of the letter width. */
private const val BOLD_EXTRA_WIDTH = 0.04f

/** Extra letter width of outline decorations, as a part of the letter width. */
private const val OUTLINE_EXTRA_WIDTH = 0.06f

/** Factor of the letter width for the decorations of a text. */
internal fun List<DecorationRange>.widthFactor(): Float =
    1f + (if (any { it.decoration.bold }) BOLD_EXTRA_WIDTH else 0f) + (if (any { it.decoration.outline }) OUTLINE_EXTRA_WIDTH else 0f)

/**
 * Look of a text overlay with a template, or without a template when the receiver is null.
 *
 * The entrance, the exit, and the line rules come from the "Magic Text Templates" spec. A text without a template is
 * white Pretendard at 80 px on the 1080 canvas, with no shadow, no entrance, and a fade exit of 200 ms (D60).
 *
 * @param scale Size factor. 1 is the editor stage. Previews use smaller values.
 * @param font Font that replaces the font of the template, or null to keep the font of the template.
 * @param sizeStep Size step from 1 to 5. It multiplies the font size of the template.
 * @param decorations Decorations of the text. Bold and outline decorations change the line breaks.
 */
fun TextStyleType?.look(
    scale: Float = 1f,
    font: TextFontType? = null,
    sizeStep: Int = DEFAULT_SIZE_STEP,
    decorations: List<DecorationRange> = emptyList(),
): TextLook {
    val base = TextLook(
        fontSize = 24f,
        fontWeight = FontWeight.ExtraBold,
        font = this?.font ?: TextFontType.PRETENDARD,
        shadows = listOf(DefaultTextShadow),
        animation = TextAnimation(TextEntrance.POP, entranceMillis = 350, exitMillis = TITLE_EXIT_MILLIS),
    )
    // 썸네일은 큰 제목이라 그림자와 등장 애니메이션 없이 시작한다. 크기와 두께는 1080 캨버스 px를 무대 dp로 바꾼 값이다.
    val thumbnail = base.copy(fontWeight = FontWeight.Normal, shadows = emptyList(), animation = ThumbnailAnimation)
    val look = when (this) {
        null -> base.copy(fontSize = PLAIN_FONT_SIZE, shadows = emptyList(), animation = PlainAnimation)
        TextStyleType.NEON -> base.copy(
            color = NeonText,
            shadows = listOf(LookShadow(NeonGlowNear, 0f, 6f), LookShadow(NeonGlowFar, 0f, 16f)),
            animation = TextAnimation(TextEntrance.FLICKER, entranceMillis = 700, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.MARKER -> base.copy(
            color = Color(0xFF111111), background = MarkerYellow, fontSize = 19f,
            paddingVertical = 2f, paddingHorizontal = 8f, cornerRadius = 2f, shadows = emptyList(),
            animation = TextAnimation(TextEntrance.MARKER_SWEEP, entranceMillis = 550, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.IMPACT -> base.copy(
            fontSize = 30f, color = ImpactYellow,
            outline = true, outlineWidth = 3.7f, shadows = listOf(LookShadow(Color.Black, offsetY = 2.5f, blur = 0f)),
            rotation = -4f,
            animation = TextAnimation(TextEntrance.STAMP, entranceMillis = 380, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.MINIMAL -> base.copy(
            fontWeight = FontWeight.Medium, fontSize = 12f, letterSpacingEm = 0.115f,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.5f), offsetY = 0.5f, blur = 5f)),
            textAlign = TextAlign.Start, boxWidthRatio = 0.7f,
            animation = TextAnimation(TextEntrance.FADE_UP, entranceMillis = 700, exitMillis = TITLE_EXIT_MILLIS),
        )
        // '서울' 줄: 168px, 흰 테두리 16px, 딱딱한 그림자 10·12px.
        TextStyleType.THUMB_FIRE -> thumbnail.copy(
            fontSize = 168 * DP_PER_SPEC_PX, color = Color(0xFFF5A8C8),
            outline = true, outlineEm = 8f / 168, outlineColor = Color.White,
            extrude = Offset(10f / 168, 12f / 168), extrudeColor = Color(0xFF5B4250),
        )
        // '월 260' 줄: 220px, 어두운 테두리 12px, 흰 빛, 별 두 개.
        TextStyleType.THUMB_BUBBLE -> thumbnail.copy(
            fontSize = 220 * DP_PER_SPEC_PX, color = Color(0xFFFFF27A),
            outline = true, outlineEm = 6f / 220, outlineColor = Color(0xFF2E2A3A),
            glow = LookGlow(Color.White, blurEm = 0.03f), boxWidthRatio = 1.075f, stickers = BubbleStars,
        )
        // '배운 것' 줄: 310px, 검은 테두리 16px, 자간 4px, 3도 회전, 세로 1.5배.
        TextStyleType.THUMB_HAND -> thumbnail.copy(
            fontSize = 310 * DP_PER_SPEC_PX, color = Color(0xFFFFF24D),
            outline = true, outlineEm = 8f / 310, outlineColor = Color(0xFF111111),
            letterSpacingEm = 4f / 310, rotation = 3f, scaleY = 1.5f, boxWidthRatio = 1.011f,
        )
        // '(청약 4번 당첨된 방법)' 줄: 108px, 굵기 300, 자간 -5px, 아래 1·흐림 8 그림자.
        TextStyleType.THUMB_THIN -> thumbnail.copy(
            fontSize = 108 * DP_PER_SPEC_PX, fontWeight = FontWeight.Light, letterSpacingEm = -5f / 108,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.25f), offsetY = 1 * DP_PER_SPEC_PX, blur = 8 * DP_PER_SPEC_PX)),
            boxWidthRatio = 1.305f,
        )
        // '일주일에 3번' 줄: 118px, 어두운 테두리 8px, 아래 4·흐림 12 그림자, 끝 단어 노랑.
        TextStyleType.THUMB_BOLD -> thumbnail.copy(
            fontSize = 118 * DP_PER_SPEC_PX,
            outline = true, outlineEm = 4f / 118, outlineColor = Color(0xFF262626),
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.35f), offsetY = 4 * DP_PER_SPEC_PX, blur = 12 * DP_PER_SPEC_PX)),
            lastWordColor = Color(0xFFFFF09C),
        )
        // '멋있다고 느끼는' 줄: 96px, 굵기 700, 남색 테두리 7px, 반짝이 두 개.
        TextStyleType.THUMB_PIXEL -> thumbnail.copy(
            fontSize = 96 * DP_PER_SPEC_PX, fontWeight = FontWeight.Bold, color = Color(0xFFFFF6C4),
            outline = true, outlineEm = 3.5f / 96, outlineColor = Color(0xFF1F2A48), stickers = PixelSparks,
        )
    }
    val stepFactor = SizeStepFactors[sizeStep.coerceIn(1, SizeStepFactors.size) - 1]
    return look.copy(font = font ?: look.font, fontSize = look.fontSize * stepFactor, widthFactor = decorations.widthFactor())
        .withLineLimit()
        .scaled(scale)
}

/**
 * Look of the text overlay: its style, font, size step, decorations, and the animations that the user picked.
 *
 * @param scale Size factor. 1 is the editor stage.
 */
fun TextOverlay.look(scale: Float = 1f): TextLook {
    val look = style.look(scale = scale, font = font, sizeStep = sizeStep, decorations = decorations)
    return look.copy(animation = look.animation.withChoices(entrance, entranceMillis, exit, exitMillis, length, loop))
}

/**
 * Look of the captions.
 *
 * The entrance, the exit, the highlight, and the line rules come from the "Magic Text Templates" spec.
 *
 * @param scale Size factor. 1 is the editor stage. Previews use smaller values.
 * @param decorations Decorations of one caption. Bold and outline decorations change the line breaks.
 */
fun CaptionStyleType.look(scale: Float = 1f, decorations: List<DecorationRange> = emptyList()): TextLook {
    val base = TextLook(
        fontSize = 17f,
        fontWeight = FontWeight.ExtraBold,
        outline = true,
        shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.6f), offsetY = 2f, blur = 8f)),
        cornerRadius = 6f,
        animation = TextAnimation(TextEntrance.FADE, entranceMillis = 150, exitMillis = CAPTION_EXIT_MILLIS),
    )
    val look = when (this) {
        CaptionStyleType.BASIC -> base
        CaptionStyleType.POP -> base.copy(highlightColor = PopYellow, highlight = CaptionHighlight.CURRENT)
        CaptionStyleType.BOX -> base.copy(
            fontWeight = FontWeight.SemiBold, fontSize = 15f, background = Color.Black.copy(alpha = 0.72f),
            paddingVertical = 4f, paddingHorizontal = 9f, outline = false, shadows = emptyList(),
        )
        CaptionStyleType.MINIMAL -> base.copy(
            fontWeight = FontWeight.Medium, fontSize = 14f, outline = false,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.7f), offsetY = 1f, blur = 6f)), bottomPercent = 9f,
        )
        CaptionStyleType.SERIF -> base.copy(
            font = TextFontType.GOWUN_BATANG, fontWeight = FontWeight.Bold, fontSize = 16f, outline = false,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.6f), offsetY = 2f, blur = 10f)),
        )
        CaptionStyleType.BIG -> base.copy(
            fontSize = 25f, fontWeight = FontWeight.Black, highlightColor = MagicColors.Accent, bottomPercent = 40f,
            maxLines = 3, highlight = CaptionHighlight.CURRENT, revealByWord = true,
            animation = TextAnimation(TextEntrance.POP, entranceMillis = 200, exitMillis = CAPTION_EXIT_MILLIS),
        )
    }
    return look.copy(widthFactor = decorations.widthFactor()).withLineLimit().scaled(scale)
}

/** Soft shadow of the shadow decoration: 6 down, 28 blur, 45% black on the 1080 canvas. */
private val DecorationShadow = LookShadow(Color.Black.copy(alpha = 0.45f), offsetY = 1.47f, blur = 6.85f)

/** Width of the outline decoration, as a part of the font size. The stroke is centered on the edge. */
private const val DECORATION_OUTLINE_RATIO = 0.16f

/** Width of the false bold stroke, as a part of the font size. Fonts without a bold weight use it. */
private const val FALSE_BOLD_RATIO = 0.03f

/** Vertical padding of a decoration background, in dp at font scale 1. */
private const val DECORATION_BACKGROUND_PADDING_V = 2.45f

/** Horizontal padding of a decoration background, in dp at font scale 1. */
private const val DECORATION_BACKGROUND_PADDING_H = 6.85f

/** Corner radius of a rounded decoration background, in dp at font scale 1. It is 14 px on the 1080 canvas. */
private const val DECORATION_BACKGROUND_RADIUS = 3.4f

/** Width of the box border of a decoration background: 6 px on the 1080 canvas (D62). */
private const val DECORATION_BORDER_WIDTH = 6 * DP_PER_SPEC_PX

/** Free space between the background boxes of two lines, in dp. */
private const val DECORATION_LINE_GAP = 2.4f

/** Line height of the text, as a part of the font size. */
private const val LINE_HEIGHT_EM = 1.25f

/** Height of a background box without its padding, as a part of the font size. */
private const val DECORATION_BOX_HEIGHT_EM = 1.1f

/**
 * Text color on a decoration background. It is white or black, whichever has the higher contrast ratio (WCAG).
 *
 * @param background Background color as 0xRRGGBB.
 */
fun autoTextColor(background: Int): Color {
    val luminance = Color(0xFF000000.toInt() or background).luminance()
    val onWhite = 1.05f / (luminance + 0.05f)
    val onBlack = (luminance + 0.05f) / 0.05f
    return if (onWhite >= onBlack) Color.White else Color(0xFF111111)
}

/** Converts a 0xRRGGBB value to an opaque color. */
fun rgbColor(rgb: Int): Color = Color(0xFF000000.toInt() or rgb)

/**
 * One character of the drawn text.
 *
 * @property source Index of the character in the source text.
 * @property word Index of the word, or -1 for a space between words.
 * @property decoration Decoration of the character, or null.
 */
private class DrawnChar(val source: Int, val word: Int, val decoration: Decoration?)

/**
 * Text after the line breaks of a [TextLook], with the source position of each character.
 *
 * @property lines Characters of each line.
 * @property fontScale Size factor of the text, from 0.7 to 1.
 */
private class DrawnText(val lines: List<List<DrawnChar>>, val fontScale: Float)

/** Breaks the text into lines with the rule of [look], and keeps the decoration of each character. */
private fun drawnText(text: String, decorations: List<DecorationRange>, look: TextLook): DrawnText {
    val words = Regex("\\S+").findAll(text).toList()
    val breaks = look.breakLines(words.map { it.value })
    val perChar = decorations.perCharacter(text.length)
    var wordIndex = 0
    val lines = breaks.lines.map { line ->
        buildList {
            line.forEachIndexed { indexInLine, _ ->
                val word = words[wordIndex]
                word.range.forEach { add(DrawnChar(it, wordIndex, perChar[it])) }
                // 단어 사이 빈칸은 원문 빈칸의 꾸미기를 따른다. 그래서 이어진 배경이 빈칸에서 끊기지 않는다.
                if (indexInLine < line.lastIndex) {
                    val space = word.range.last + 1
                    add(DrawnChar(space, -1, if (space < text.length && text[space].isWhitespace()) perChar[space] else null))
                }
                wordIndex++
            }
        }
    }
    return DrawnText(lines, breaks.fontScale)
}

/** Layer of the drawn text. The layers draw from the first to the last. */
private enum class TextLayer {
    /** FAR_SHADOW : One of the far shadows of the style. */
    FAR_SHADOW,

    /** DECORATION_SHADOW : The soft shadow of the shadow decoration. */
    DECORATION_SHADOW,

    /** STYLE_OUTLINE : The outline of the style. */
    STYLE_OUTLINE,

    /** DECORATION_OUTLINE : The black outline of the outline decoration. */
    DECORATION_OUTLINE,

    /** FALSE_BOLD : A thin stroke in the text color, for bold letters of a font without a bold weight. */
    FALSE_BOLD,

    /** MAIN : The letters with their colors and the near shadow of the style. */
    MAIN,

    /** FILL_ALL : The gradient fill of all words. */
    FILL_ALL,

    /** FILL_FIRST : The gradient fill of the first word. */
    FILL_FIRST,

    /** FILL_REST : The gradient fill of the words after the first word. */
    FILL_REST,
}

/**
 * Text drawn with a [TextLook]: background box, outline, shadows, and the decorations of character ranges.
 *
 * The words break into lines with the overflow rule of the look. Each layer draws the same text,
 * so the layers line up.
 *
 * @param text Text to draw.
 * @param decorations Decorations of character ranges in [text].
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param frame Animation state of the text.
 * @param wordColor Color of the word at an index, or null for the text color.
 * @param wordHidden True for a word at an index that is not on screen yet. The word keeps its space.
 */
@Composable
private fun LookText(
    text: String,
    decorations: List<DecorationRange>,
    look: TextLook,
    modifier: Modifier = Modifier,
    frame: TextFrame = TextFrame.Static,
    wordColor: (Int) -> Color? = { null },
    wordHidden: (Int) -> Boolean = { false },
) {
    val fontFamily = look.font.fontFamily()
    val boldWeight = look.font.boldWeight
    val density = LocalDensity.current
    val drawn = remember(text, decorations, look) { drawnText(text, decorations, look) }
    val fit = drawn.fontScale
    val fontSizePx = with(density) { (look.fontSize * fit).sp.toPx() }
    // 배경 박스가 있으면 줄 사이를 벌려 줄마다 박스가 따로 깔리게 한다. 글자는 줄 가운데에 둔다.
    val boxes = drawn.lines.any { line -> line.any { it.decoration?.background != null } }
    val borders = boxes && drawn.lines.any { line -> line.any { it.decoration?.background != null && it.decoration.backgroundBorder } }
    val lineGap = DECORATION_BACKGROUND_PADDING_V * fit + (if (borders) DECORATION_BORDER_WIDTH else 0f) + DECORATION_LINE_GAP / 2
    val style = TextStyle(
        color = look.color,
        fontSize = (look.fontSize * fit).sp,
        fontWeight = look.fontWeight,
        fontFamily = fontFamily,
        // 파일에 없는 굵기를 흉내 내지 않는다. 굵기가 하나뿐인 글꼴은 그 굵기로 그린다.
        fontSynthesis = FontSynthesis.None,
        letterSpacing = look.letterSpacingEm.em,
        lineHeight = if (boxes) (LINE_HEIGHT_EM + lineGap * 2 / (look.fontSize * fit)).em else LINE_HEIGHT_EM.em,
        lineHeightStyle = if (boxes) LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None) else null,
        textAlign = look.textAlign,
    )
    // 마지막 단어 강조는 단어가 두 개 이상일 때만 준다.
    val lastWord = drawn.lines.maxOfOrNull { line -> line.maxOfOrNull { it.word } ?: -1 } ?: -1
    val lastWordColor = look.lastWordColor?.takeIf { lastWord >= 1 }
    val colorOfWord: (Int) -> Color? = { index -> wordColor(index) ?: lastWordColor?.takeIf { index == lastWord } }
    val outlineStrokePx = if (look.outlineEm > 0f) look.outlineEm * fontSizePx * 2 else with(density) { (look.outlineWidth * fit).dp.toPx() }
    // 불꽃 반복은 스타일의 그림자를 위로 번지는 주황빛 그림자로 바꾼다.
    val shadows = if (frame.loop == TextLoop.FIRE) fireShadows(frame.loopTime, look.fontSize * fit) else look.shadows
    val outlineLayout = remember { LayoutRef() }
    val hasDecorations = decorations.isNotEmpty()
    val decorationShadow = remember(density) { DecorationShadow.toShadow(density) }

    /** Text of one layer. Each character gets the style of the layer and of its decoration. */
    fun layer(kind: TextLayer): AnnotatedString = buildAnnotatedString {
        drawn.lines.forEachIndexed { lineIndex, line ->
            if (lineIndex > 0) append('\n')
            line.forEach { char ->
                val decoration = char.decoration
                val hidden = char.word >= 0 && wordHidden(char.word)
                val onBackground = decoration?.background != null
                val span = when {
                    hidden -> SpanStyle(color = Color.Transparent, shadow = Shadow.None)
                    else -> when (kind) {
                        TextLayer.FAR_SHADOW -> if (onBackground) SpanStyle(color = Color.Transparent, shadow = Shadow.None) else null
                        TextLayer.DECORATION_SHADOW ->
                            SpanStyle(color = Color.Transparent, shadow = if (decoration?.shadow == true) decorationShadow else Shadow.None)
                        TextLayer.STYLE_OUTLINE -> if (onBackground) SpanStyle(color = Color.Transparent) else null
                        TextLayer.DECORATION_OUTLINE -> SpanStyle(color = if (decoration?.outline == true) Color.Black else Color.Transparent)
                        TextLayer.FALSE_BOLD -> SpanStyle(
                            color = if (decoration?.bold == true && boldWeight == null) mainColor(char, decoration, look, colorOfWord) else Color.Transparent,
                        )
                        TextLayer.MAIN -> SpanStyle(
                            color = mainColor(char, decoration, look, colorOfWord),
                            fontWeight = if (decoration?.bold == true && boldWeight != null) boldWeight else null,
                            shadow = if (onBackground) Shadow.None else null,
                        )
                        TextLayer.FILL_ALL, TextLayer.FILL_FIRST, TextLayer.FILL_REST -> {
                            val inGroup = when (kind) {
                                TextLayer.FILL_FIRST -> char.word == 0
                                TextLayer.FILL_REST -> char.word > 0
                                else -> true
                            }
                            // 꾸미기 색이나 배경이 있는 글자는 그라데이션을 덮지 않고 꾸미기 색을 보여 준다.
                            // 칠할 글자에는 색을 두지 않는다. 그래야 나중에 다른 색으로 다시 그릴 수 있다.
                            if (inGroup && decoration?.color == null && !onBackground) {
                                SpanStyle(fontWeight = if (decoration?.bold == true && boldWeight != null) boldWeight else null)
                            } else {
                                SpanStyle(color = Color.Transparent)
                            }
                        }
                    }
                }
                val letter = if (char.word < 0) ' ' else text[char.source]
                if (span != null) withStyle(span) { append(letter) } else append(letter)
            }
        }
    }

    val background = look.background
    val cornerRadius = look.cornerRadius.dp
    val paddingH = (look.paddingHorizontal * fit).dp
    val paddingV = (look.paddingVertical * fit).dp
    var mainLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Box(
        modifier = modifier
            .graphicsLayer {
                val shake = if (frame.loop == TextLoop.SHAKE) shakeAt(frame.loopTime, fontSizePx) else ShakeOffset.None
                alpha = frame.alpha
                scaleX = frame.scale
                scaleY = frame.scale * look.scaleY
                translationX = shake.x
                translationY = frame.offsetY.dp.toPx() + shake.y
                rotationZ = look.rotation + shake.rotation
            }
            .drawWithContent {
                val layout = mainLayout
                // 타이핑과 물결치기는 글자 자리를 잘라 따로 그린다. 모든 층(그림자·테두리·입체)이 글자와 함께 움직인다.
                val pieces = if (layout != null && (frame.charReveal < 1f || frame.loop == TextLoop.WAVE)) {
                    textPieces(drawn, layout, frame, fontSizePx, Offset(paddingH.toPx(), paddingV.toPx()), size)
                } else {
                    null
                }
                // 기울임은 그리기 단계에서 준다. iOS(Skia)의 글자 배치는 TextGeometricTransform을 그리지 않는다.
                withTransform({ if (look.skewX != 0f) transform(skewMatrix(look.skewX, size.height / 2)) }) {
                    drawStickers(look.stickers, fontSizePx)
                    if (frame.textReveal >= 1f) {
                        drawLookBackground(background, cornerRadius.toPx(), frame.backgroundReveal)
                        this@drawWithContent.drawPieces(pieces)
                    } else {
                        // 스펙처럼 위아래와 왼쪽은 넉넉히 남겨서, 그림자와 테두리가 잘리지 않게 한다.
                        clipRect(
                            left = -size.width * 0.1f,
                            top = -size.height * 0.3f,
                            right = size.width * frame.textReveal,
                            bottom = size.height * 1.3f,
                        ) {
                            drawLookBackground(background, cornerRadius.toPx(), frame.backgroundReveal)
                            this@drawWithContent.drawPieces(pieces)
                        }
                    }
                }
            }
            .padding(horizontal = paddingH, vertical = paddingV),
    ) {
        // 꾸미기 배경은 글자 뒤 모든 층보다 아래에 그린다.
        if (drawn.lines.any { line -> line.any { it.decoration?.background != null } }) {
            Box(
                modifier = Modifier.matchParentSize().drawBehind {
                    val layout = mainLayout ?: return@drawBehind
                    drawDecorationBackgrounds(drawn, layout, fit, fontSizePx)
                },
            )
        }
        // 아래 레이어부터 그린다: 먼 그림자 → 꾸미기 그림자 → 빛·입체 그림자 → 테두리 → 본문(가까운 그림자 포함)
        // → 그라데이션.
        shadows.drop(1).reversed().forEach { shadow ->
            Text(text = layer(TextLayer.FAR_SHADOW), style = style.copy(shadow = shadow.toShadow(density)), softWrap = false)
        }
        if (hasDecorations && decorations.any { it.decoration.shadow }) {
            Text(text = layer(TextLayer.DECORATION_SHADOW), style = style, softWrap = false)
        }
        if (look.outline) {
            Text(
                text = layer(TextLayer.STYLE_OUTLINE),
                style = style.copy(color = look.outlineColor, drawStyle = Stroke(width = outlineStrokePx, join = StrokeJoin.Round)),
                softWrap = false,
                onTextLayout = { outlineLayout.value = it },
                modifier = if (look.glow == null && look.extrude == null) Modifier else Modifier.drawWithContent {
                    outlineLayout.value?.let { drawUnderOutline(it, look, fontSizePx, outlineStrokePx) }
                    drawContent()
                },
            )
        }
        if (hasDecorations && decorations.any { it.decoration.outline }) {
            Text(
                text = layer(TextLayer.DECORATION_OUTLINE),
                style = style.copy(drawStyle = Stroke(width = fontSizePx * DECORATION_OUTLINE_RATIO, join = StrokeJoin.Round)),
                softWrap = false,
            )
        }
        if (hasDecorations && boldWeight == null && decorations.any { it.decoration.bold }) {
            Text(
                text = layer(TextLayer.FALSE_BOLD),
                style = style.copy(drawStyle = Stroke(width = fontSizePx * FALSE_BOLD_RATIO, join = StrokeJoin.Round)),
                softWrap = false,
            )
        }
        Text(
            text = layer(TextLayer.MAIN),
            style = style.copy(shadow = shadows.firstOrNull()?.toShadow(density)),
            softWrap = false,
            onTextLayout = { mainLayout = it },
        )
        look.fill?.let { fill ->
            val firstWordFill = look.firstWordFill
            if (firstWordFill == null) {
                FillText(layer(TextLayer.FILL_ALL), style, fill, look.font, fontSizePx)
            } else {
                FillText(layer(TextLayer.FILL_FIRST), style, firstWordFill, look.font, fontSizePx)
                FillText(layer(TextLayer.FILL_REST), style, fill, look.font, fontSizePx)
            }
        }
    }
}

// =====================================================================
// Loops and typing (F09)
// =====================================================================

/** Height of the wave loop, as a part of the font size. */
private const val WAVE_HEIGHT_EM = 0.14f

/** Speed of the wave loop, in radians per second. One wave takes about 0.9 seconds. */
private const val WAVE_SPEED = 7f

/** Phase step of the wave loop from one character to the next, in radians. */
private const val WAVE_STEP = 0.6f

/** Largest move of the shake loop, as a part of the font size. */
private const val SHAKE_EM = 0.06f

/** Largest turn of the shake loop, in degrees. */
private const val SHAKE_DEGREES = 1.8f

/** Font size that the fire glow sizes of the prototype use, in dp. */
private const val FIRE_REFERENCE_SIZE = 26f

/**
 * Move and turn of the shake loop at one moment.
 *
 * @property x Horizontal move in pixels.
 * @property y Vertical move in pixels.
 * @property rotation Turn in degrees.
 */
private class ShakeOffset(val x: Float, val y: Float, val rotation: Float) {
    companion object {
        val None = ShakeOffset(0f, 0f, 0f)
    }
}

/** Shake of the whole text box at [time] seconds. Several fast sine waves make an uneven shake. */
private fun shakeAt(time: Float, fontSizePx: Float): ShakeOffset {
    val a = fontSizePx * SHAKE_EM
    return ShakeOffset(
        x = sin(time * 61f) * a + sin(time * 23f) * a * 0.6f,
        y = cos(time * 53f) * a,
        rotation = sin(time * 41f) * SHAKE_DEGREES,
    )
}

/**
 * Shadows of the fire loop at [time] seconds: a light core and orange glows that go up and flicker.
 *
 * @param fontSize Font size of the text in dp.
 */
private fun fireShadows(time: Float, fontSize: Float): List<LookShadow> {
    val glow = 0.7f + 0.3f * abs(sin(time * 11f) * sin(time * 6.7f + 1f))
    val unit = fontSize / FIRE_REFERENCE_SIZE
    return listOf(
        LookShadow(Color(0xE6FFFADC), offsetY = 0f, blur = 2f * unit),
        LookShadow(Color(0xFFFFD84D), offsetY = -1.5f * unit, blur = 5f * glow * unit),
        LookShadow(Color(0xFFFF9A1F), offsetY = -3f * unit, blur = 10f * glow * unit),
        LookShadow(Color(0xFFFF4A17), offsetY = -5f * unit, blur = 17f * glow * unit),
        LookShadow(Color(0x99FF2800), offsetY = -8f * unit, blur = 26f * glow * unit),
    )
}

/**
 * Part of the text box that draws on its own.
 *
 * @property rect Area of the part in the text box.
 * @property dy Vertical move of the part in pixels.
 */
private class TextPiece(val rect: Rect, val dy: Float)

/**
 * Splits the text box into parts for the typing entrance and the wave loop.
 *
 * Typing gives one part for each line, up to the last character that shows. The wave gives one part for each
 * character that shows, with its own height. The first and the last part of a line go far outside the box, so the
 * shadows and the outlines at the edges stay.
 *
 * @param drawn Text after the line breaks.
 * @param layout Layout of the letters.
 * @param frame Animation state.
 * @param fontSizePx Font size in pixels.
 * @param origin Position of the letters in the text box.
 * @param boxSize Size of the text box.
 */
private fun textPieces(drawn: DrawnText, layout: TextLayoutResult, frame: TextFrame, fontSizePx: Float, origin: Offset, boxSize: Size): List<TextPiece> {
    val total = drawn.lines.sumOf { it.size }
    val shown = floor(frame.charReveal * total + 1e-4f).toInt()
    val wave = frame.loop == TextLoop.WAVE
    val amplitude = fontSizePx * WAVE_HEIGHT_EM
    val far = boxSize.width + boxSize.height + fontSizePx * 4
    val pieces = mutableListOf<TextPiece>()
    var offset = 0
    var index = 0
    drawn.lines.forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) offset++
        if (lineIndex < layout.lineCount) {
            val top = if (lineIndex == 0) -far else origin.y + layout.getLineTop(lineIndex)
            val bottom = if (lineIndex == drawn.lines.lastIndex) far else origin.y + layout.getLineBottom(lineIndex)
            val visible = (shown - index).coerceIn(0, line.size)
            fun box(char: Int) = layout.getBoundingBox(offset + char)
            if (visible > 0 && !wave) {
                val right = if (visible == line.size) far else origin.x + box(visible - 1).right
                pieces += TextPiece(Rect(-far, top, right, bottom), 0f)
            } else if (visible > 0) {
                for (char in 0 until visible) {
                    val left = if (char == 0) -far else origin.x + box(char).left
                    val right = when (char) {
                        line.lastIndex -> far
                        visible - 1 -> origin.x + box(char).right
                        else -> origin.x + box(char + 1).left
                    }
                    val dy = sin(frame.loopTime * WAVE_SPEED - (index + char) * WAVE_STEP) * amplitude
                    pieces += TextPiece(Rect(left, top, right, bottom), dy)
                }
            }
        }
        index += line.size
        offset += line.size
    }
    return pieces
}

/** Draws the content, or only [pieces] when they are not null. */
private fun ContentDrawScope.drawPieces(pieces: List<TextPiece>?) {
    if (pieces == null) {
        drawContent()
        return
    }
    pieces.forEach { piece ->
        translate(top = piece.dy) {
            clipRect(piece.rect.left, piece.rect.top, piece.rect.right, piece.rect.bottom) { this@drawPieces.drawContent() }
        }
    }
}

/** Draws the background box of a look. [reveal] is the part that shows from the left edge, from 0 to 1. */
private fun DrawScope.drawLookBackground(background: Color, cornerRadiusPx: Float, reveal: Float) {
    if (background.alpha <= 0f) return
    clipRect(right = size.width * reveal) {
        drawRoundRect(background, cornerRadius = CornerRadius(cornerRadiusPx))
    }
}

/**
 * Matrix that slants the drawing horizontally around a row.
 *
 * @param skewX Slant. A negative value moves the parts above [pivotY] to the right.
 * @param pivotY Row that does not move.
 */
private fun skewMatrix(skewX: Float, pivotY: Float): Matrix = Matrix().apply {
    values[Matrix.SkewX] = skewX
    values[Matrix.TranslateX] = -skewX * pivotY
}

/** Last text layout of a layer. The drawing of the layer reads the lines from it. */
private class LayoutRef {
    var value: TextLayoutResult? = null
}

/** Number of times that the glow draws. More passes make the light stronger. */
private const val GLOW_PASSES = 2

/**
 * Draws the glow and the 3D shadow of [look] under the outline.
 *
 * @param layout Layout of the outline layer.
 * @param look Visual style.
 * @param fontSizePx Font size in pixels.
 * @param strokePx Width of the outline stroke in pixels.
 */
private fun DrawScope.drawUnderOutline(layout: TextLayoutResult, look: TextLook, fontSizePx: Float, strokePx: Float) {
    val stroke = Stroke(width = strokePx, join = StrokeJoin.Round)
    look.glow?.let { glow ->
        val shadow = Shadow(color = glow.color, offset = Offset.Zero, blurRadius = glow.blurEm * fontSizePx)
        // 테두리 색 획은 위의 테두리 층에 가려지고, 번진 빛만 남는다.
        repeat(GLOW_PASSES) { drawText(layout, color = look.outlineColor, drawStyle = stroke, shadow = shadow) }
    }
    look.extrude?.let { extrude ->
        val dx = extrude.x * fontSizePx
        val dy = extrude.y * fontSizePx
        // 1픽셀 간격으로 글자를 겹쳐 그려서 끊김 없는 입체 그림자를 만든다.
        val steps = ceil(max(abs(dx), abs(dy))).toInt().coerceIn(1, MAX_EXTRUDE_STEPS)
        for (step in steps downTo 1) {
            val at = Offset(dx * step / steps, dy * step / steps)
            drawText(layout, color = look.extrudeColor, topLeft = at, drawStyle = Fill)
            drawText(layout, color = look.extrudeColor, topLeft = at, drawStyle = stroke)
        }
    }
}

/**
 * Letters of one fill group with a vertical gradient on each line, and the inner edge band of the fill.
 *
 * @param text Text of the group. The letters outside the group are transparent.
 * @param style Text style of the layers.
 * @param fill Gradient and edge band.
 * @param font Font of the text. Its letter bounds place the gradient.
 * @param fontSizePx Font size in pixels.
 */
@Composable
private fun FillText(text: AnnotatedString, style: TextStyle, fill: LookFill, font: TextFontType, fontSizePx: Float) {
    val layoutRef = remember { LayoutRef() }
    Text(
        text = text,
        style = style.copy(color = Color.White),
        softWrap = false,
        onTextLayout = { layoutRef.value = it },
        modifier = Modifier.drawWithContent {
            val layout = layoutRef.value ?: run {
                drawContent()
                return@drawWithContent
            }
            // 기울인 글자와 줄 밖으로 나온 획도 칠하도록 그리는 영역을 넓힌다.
            val bounds = Rect(-fontSizePx, -fontSizePx, size.width + fontSizePx, size.height + fontSizePx)
            drawContext.canvas.saveLayer(bounds, Paint())
            drawContent()
            drawLineGradients(layout, fill, font, fontSizePx, bounds)
            drawContext.canvas.restore()
            fill.edge?.let { drawInnerEdge(layout, it, fontSizePx, bounds) }
        },
    )
}

/** Colors the letters of the current layer with the gradient. Each line gets the full gradient. */
private fun DrawScope.drawLineGradients(layout: TextLayoutResult, fill: LookFill, font: TextFontType, fontSizePx: Float, bounds: Rect) {
    val (topEm, bottomEm) = font.letterBoundsEm
    val stops = fill.stops.toTypedArray()
    val last = layout.lineCount - 1
    for (line in 0..last) {
        val baseline = layout.getLineBaseline(line)
        val top = if (line == 0) bounds.top else layout.getLineTop(line)
        val bottom = if (line == last) bounds.bottom else layout.getLineBottom(line)
        drawRect(
            brush = Brush.verticalGradient(*stops, startY = baseline - topEm * fontSizePx, endY = baseline + bottomEm * fontSizePx),
            topLeft = Offset(bounds.left, top),
            size = Size(bounds.width, bottom - top),
            blendMode = BlendMode.SrcIn,
        )
    }
}

/** Draws the band of [edge] on the inner edge of the letters. */
private fun DrawScope.drawInnerEdge(layout: TextLayoutResult, edge: LookEdge, fontSizePx: Float, bounds: Rect) {
    val width = edge.widthEm * fontSizePx
    drawContext.canvas.saveLayer(bounds, Paint())
    if (edge.bottomOnly) {
        // 글자에서 위로 옮긴 글자를 빼면 아래 가장자리만 남는다.
        drawText(layout, color = edge.color)
        drawText(layout, color = Color.Black, topLeft = Offset(0f, -width), blendMode = BlendMode.DstOut)
    } else {
        // 가장자리 획에서 글자 안쪽 부분만 남긴다.
        drawText(layout, color = edge.color, drawStyle = Stroke(width = width * 2, join = StrokeJoin.Round))
        drawText(layout, color = Color.Black, drawStyle = Fill, blendMode = BlendMode.DstIn)
    }
    drawContext.canvas.restore()
}

/** Color of a letter in the main layer. A background decoration decides it first, then a color decoration. */
private fun mainColor(char: DrawnChar, decoration: Decoration?, look: TextLook, wordColor: (Int) -> Color?): Color {
    decoration?.background?.let { return autoTextColor(it) }
    decoration?.color?.let { return rgbColor(it) }
    return (if (char.word >= 0) wordColor(char.word) else null) ?: look.color
}

/**
 * Background box of a decoration: the parts that decide if two neighbor letters share one box.
 *
 * @property color Background color as 0xRRGGBB.
 * @property shape Shape of the box.
 * @property border True to draw the box border.
 */
private data class DecorationBox(val color: Int, val shape: BackgroundShape, val border: Boolean)

/** Background box of the decoration, or null when it has no background. */
private val Decoration?.box: DecorationBox?
    get() = this?.background?.let { DecorationBox(it, backgroundShape, backgroundBorder) }

/**
 * Draws the backgrounds of the background decorations. Neighbor letters with the same background, shape, and border
 * share one box, and each line has its own boxes. A box border is a line on the inner edge of the box, in the
 * automatic text color (D62).
 *
 * @param drawn Text after the line breaks.
 * @param layout Layout of the letters.
 * @param fit Size factor of the text from the overflow rule.
 * @param fontSizePx Font size in pixels.
 */
private fun DrawScope.drawDecorationBackgrounds(drawn: DrawnText, layout: TextLayoutResult, fit: Float, fontSizePx: Float) {
    val paddingV = (DECORATION_BACKGROUND_PADDING_V * fit).dp.toPx()
    val paddingH = (DECORATION_BACKGROUND_PADDING_H * fit).dp.toPx()
    val rounded = CornerRadius((DECORATION_BACKGROUND_RADIUS * fit).dp.toPx())
    val borderWidth = DECORATION_BORDER_WIDTH.dp.toPx()
    val halfHeight = fontSizePx * DECORATION_BOX_HEIGHT_EM / 2
    var offset = 0
    drawn.lines.forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) offset++
        val lineNumber = layout.getLineForOffset(offset.coerceAtMost(maxOf(0, layout.layoutInput.text.length - 1)))
        val middle = (layout.getLineTop(lineNumber) + layout.getLineBottom(lineNumber)) / 2
        var runStart = -1
        line.forEachIndexed { index, char ->
            val box = char.decoration.box
            if (box != null && runStart < 0) runStart = index
            if (box != null && line.getOrNull(index + 1)?.decoration.box != box) {
                // 테두리는 여백 바깥에 더해서, 테두리를 켜도 글자와 테두리 사이 여백이 줄지 않는다.
                val edge = if (box.border) borderWidth else 0f
                val rect = Rect(
                    left = layout.getBoundingBox(offset + runStart).left - paddingH - edge,
                    top = middle - halfHeight - paddingV - edge,
                    right = layout.getBoundingBox(offset + index).right + paddingH + edge,
                    bottom = middle + halfHeight + paddingV + edge,
                )
                val radius = if (box.shape == BackgroundShape.SQUARE) CornerRadius.Zero else rounded
                drawRoundRect(color = rgbColor(box.color), topLeft = rect.topLeft, size = rect.size, cornerRadius = radius)
                if (box.border) {
                    val inset = borderWidth / 2
                    drawRoundRect(
                        color = autoTextColor(box.color),
                        topLeft = Offset(rect.left + inset, rect.top + inset),
                        size = Size(rect.width - borderWidth, rect.height - borderWidth),
                        cornerRadius = CornerRadius((radius.x - inset).coerceAtLeast(0f)),
                        style = Stroke(width = borderWidth),
                    )
                }
                runStart = -1
            }
        }
        offset += line.size
    }
}

/** Points of the five-point star in a 100 × 100 box, from the design. */
private val StarPoints = floatArrayOf(50f, 6f, 61f, 38f, 96f, 38f, 68f, 59f, 79f, 93f, 50f, 72f, 21f, 93f, 32f, 59f, 4f, 38f, 39f, 38f)

/** Points of the four-point sparkle in a 20 × 20 box, from the design. */
private val SparkPoints = floatArrayOf(10f, 0f, 12f, 8f, 20f, 10f, 12f, 12f, 10f, 20f, 8f, 12f, 0f, 10f, 8f, 8f)

/**
 * Draws the stickers of a look in the text box.
 *
 * @param stickers Stickers to draw.
 * @param fontSizePx Font size in pixels. The sticker sizes and places are parts of it.
 */
private fun DrawScope.drawStickers(stickers: List<LookSticker>, fontSizePx: Float) {
    stickers.forEach { sticker ->
        val stickerSize = sticker.sizeEm * fontSizePx
        val center = Offset(
            x = size.width * sticker.anchorX + sticker.offsetXEm * fontSizePx,
            y = size.height / 2 + sticker.offsetYEm * fontSizePx,
        )
        val (points, box) = when (sticker.shape) {
            StickerShape.STAR -> StarPoints to 100f
            StickerShape.SPARK -> SparkPoints to 20f
        }
        val unit = stickerSize / box
        val path = Path().apply {
            for (i in points.indices step 2) {
                val x = center.x + (points[i] - box / 2) * unit
                val y = center.y + (points[i + 1] - box / 2) * unit
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(path, sticker.fill)
        sticker.stroke?.let { drawPath(path, it, style = Stroke(width = stickerSize * sticker.strokeRatio, join = StrokeJoin.Round)) }
    }
}

/**
 * Overlay text.
 *
 * @param text Text to draw.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param frame Animation state of the text.
 * @param decorations Decorations of character ranges in [text].
 */
@Composable
fun OverlayText(
    text: String,
    look: TextLook,
    modifier: Modifier = Modifier,
    frame: TextFrame = TextFrame.Static,
    decorations: List<DecorationRange> = emptyList(),
) {
    LookText(text = text, decorations = decorations, look = look, modifier = modifier, frame = frame)
}

/**
 * Caption text. The look decides how the current word shows: with the highlight color, and for
 * some looks, with the next words hidden.
 *
 * @param text Text of the caption.
 * @param currentWord Index of the word that the speaker says now.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param frame Animation state of the caption.
 * @param decorations Decorations of character ranges in [text]. A color decoration is stronger than the highlight.
 */
@Composable
fun CaptionText(
    text: String,
    currentWord: Int,
    look: TextLook,
    modifier: Modifier = Modifier,
    frame: TextFrame = TextFrame.Static,
    decorations: List<DecorationRange> = emptyList(),
) {
    LookText(
        text = text,
        decorations = decorations,
        look = look,
        modifier = modifier,
        frame = frame,
        wordColor = { index -> if (look.highlight == CaptionHighlight.CURRENT && index == currentWord) look.highlightColor else null },
        wordHidden = { index -> look.revealByWord && index > currentWord },
    )
}

/**
 * Caption text from words.
 *
 * @param words Words of the caption.
 * @param currentWord Index of the word that the speaker says now.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param frame Animation state of the caption.
 */
@Composable
fun CaptionText(
    words: List<String>,
    currentWord: Int,
    look: TextLook,
    modifier: Modifier = Modifier,
    frame: TextFrame = TextFrame.Static,
) {
    CaptionText(text = words.joinToString(" "), currentWord = currentWord, look = look, modifier = modifier, frame = frame)
}

/** Bold weight of the font, or null when the font has only one weight. Bold letters then use a false bold stroke. */
internal val TextFontType.boldWeight: FontWeight?
    get() = when (this) {
        TextFontType.PRETENDARD -> FontWeight.Black
        TextFontType.GOWUN_BATANG, TextFontType.GAEGU, TextFontType.DONGLE, TextFontType.NEUJOH -> FontWeight.Bold
        TextFontType.NANUM_PEN, TextFontType.BLACK_HAN_SANS, TextFontType.DO_HYEON, TextFontType.JUA,
        TextFontType.GUGI, TextFontType.SONG_MYUNG, TextFontType.KKUBULIM, TextFontType.MYOEUN_HEULLIM,
        TextFontType.GELLYROLL, TextFontType.MONA12, TextFontType.OK_DANDAN, TextFontType.STRONG_ARMY,
        TextFontType.KUKDETOPOKKI, TextFontType.ESAMANRU, TextFontType.OK_GUNG, TextFontType.DOKDO -> null
    }

/**
 * Top of the letters above the baseline and bottom of the letters below the baseline, as parts of the font size.
 * A gradient fill spans this range on each line. The values come from the measured Hangul letters of each font.
 */
internal val TextFontType.letterBoundsEm: Pair<Float, Float>
    get() = when (this) {
        TextFontType.PRETENDARD -> 0.79f to 0.08f
        TextFontType.GOWUN_BATANG -> 0.88f to 0.10f
        TextFontType.NANUM_PEN -> 0.77f to 0.16f
        TextFontType.BLACK_HAN_SANS -> 0.78f to 0.01f
        TextFontType.DO_HYEON -> 0.76f to 0.02f
        TextFontType.JUA -> 0.81f to 0.05f
        TextFontType.KKUBULIM, TextFontType.MYOEUN_HEULLIM -> 0.76f to 0.16f
        TextFontType.GELLYROLL -> 0.73f to 0.13f
        TextFontType.MONA12 -> 0.83f to 0.08f
        TextFontType.NEUJOH -> 0.80f to 0.09f
        TextFontType.OK_DANDAN -> 0.74f to 0.15f
        TextFontType.STRONG_ARMY -> 0.82f to 0.05f
        TextFontType.KUKDETOPOKKI, TextFontType.ESAMANRU -> 0.77f to 0.15f
        TextFontType.OK_GUNG -> 0.79f to 0.14f
        TextFontType.GAEGU -> 0.76f to 0.24f
        TextFontType.DONGLE -> 0.52f to 0.39f
        TextFontType.GUGI -> 0.83f to 0.08f
        TextFontType.SONG_MYUNG -> 0.80f to 0.07f
        // 독도는 재지 않았다. 그라데이션을 쓰는 템플릿이 없어서 비슷한 손글씨 글꼴 값을 쓴다.
        TextFontType.DOKDO -> 0.76f to 0.16f
    }

/** Splits a text into words at spaces. */
fun String.splitWords(): List<String> = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

private fun LookShadow.toShadow(density: Density): Shadow = with(density) {
    Shadow(color = color, offset = Offset(offsetX.dp.toPx(), offsetY.dp.toPx()), blurRadius = blur.dp.toPx())
}
