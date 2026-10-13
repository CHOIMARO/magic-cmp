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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.DEFAULT_SIZE_STEP
import com.company.core.domain.model.editor.Decoration
import com.company.core.domain.model.editor.DecorationRange
import com.company.core.domain.model.editor.TextEntrance
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.perCharacter

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
private val RetroCream = Color(0xFFFFF3D6)
private val RetroRed = Color(0xFFFF5A5F)
private val CutePurple = Color(0xFF7B4FD6)

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
 * Look of a text overlay.
 *
 * The entrance, the exit, and the line rules come from the "Magic Text Templates" spec.
 *
 * @param scale Size factor. 1 is the editor stage. Previews use smaller values.
 * @param font Font that replaces the font of the style, or null to keep the font of the style.
 * @param sizeStep Size step from 1 to 5. It multiplies the font size of the style.
 * @param decorations Decorations of the text. Bold and outline decorations change the line breaks.
 */
fun TextStyleType.look(
    scale: Float = 1f,
    font: TextFontType? = null,
    sizeStep: Int = DEFAULT_SIZE_STEP,
    decorations: List<DecorationRange> = emptyList(),
): TextLook {
    val base = TextLook(
        fontSize = 24f,
        fontWeight = FontWeight.ExtraBold,
        shadows = listOf(DefaultTextShadow),
        animation = TextAnimation(TextEntrance.POP, entranceMillis = 350, exitMillis = TITLE_EXIT_MILLIS),
    )
    val look = when (this) {
        TextStyleType.BOLD -> base
        TextStyleType.BOX -> base.copy(
            color = Color(0xFF111111), background = Color.White, fontSize = 18f,
            fontWeight = FontWeight.Bold, shadows = emptyList(),
            paddingVertical = 6f, paddingHorizontal = 11f,
            animation = TextAnimation(TextEntrance.SLIDE_UP, entranceMillis = 400, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.OUTLINE -> base.copy(shadows = emptyList(), outline = true)
        TextStyleType.SERIF -> base.copy(
            font = TextFontType.GOWUN_BATANG, fontWeight = FontWeight.Bold, fontSize = 22f,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.45f), offsetY = 2f, blur = 14f)),
            animation = TextAnimation(TextEntrance.FADE, entranceMillis = 900, exitMillis = TITLE_EXIT_MILLIS),
        )
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
        TextStyleType.TAG -> base.copy(
            color = MagicColors.OnAccent, background = MagicColors.Accent, fontSize = 14f,
            fontWeight = FontWeight.Bold, paddingVertical = 5f, paddingHorizontal = 12f,
            cornerRadius = 99f, shadows = emptyList(), letterSpacingEm = 0.03f, maxLines = 1,
            animation = TextAnimation(TextEntrance.POP, entranceMillis = 300, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.HAND -> base.copy(
            font = TextFontType.NANUM_PEN, fontWeight = FontWeight.Normal, fontSize = 35f,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.45f), offsetY = 1.5f, blur = 7f)),
            animation = TextAnimation(TextEntrance.WRITE, entranceMillis = 1000, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.IMPACT -> base.copy(
            font = TextFontType.BLACK_HAN_SANS, fontSize = 30f, color = ImpactYellow,
            outline = true, outlineWidth = 3.7f, shadows = listOf(LookShadow(Color.Black, offsetY = 2.5f, blur = 0f)),
            rotation = -4f,
            animation = TextAnimation(TextEntrance.STAMP, entranceMillis = 380, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.RETRO -> base.copy(
            font = TextFontType.DO_HYEON, fontSize = 29f, color = RetroCream,
            letterSpacingEm = 0.016f, shadows = listOf(LookShadow(RetroRed, offsetY = 2f, blur = 0f, offsetX = 2f)),
            animation = TextAnimation(TextEntrance.BOUNCE, entranceMillis = 700, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.CUTE -> base.copy(
            font = TextFontType.JUA, fontSize = 25f,
            outline = true, outlineColor = CutePurple, shadows = listOf(LookShadow(CutePurple, offsetY = 1.7f, blur = 0f)),
            animation = TextAnimation(TextEntrance.BOUNCE, entranceMillis = 650, exitMillis = TITLE_EXIT_MILLIS),
        )
        TextStyleType.MINIMAL -> base.copy(
            fontWeight = FontWeight.Medium, fontSize = 12f, letterSpacingEm = 0.115f,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.5f), offsetY = 0.5f, blur = 5f)),
            textAlign = TextAlign.Start, boxWidthRatio = 0.7f,
            animation = TextAnimation(TextEntrance.FADE_UP, entranceMillis = 700, exitMillis = TITLE_EXIT_MILLIS),
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
    return look.copy(animation = look.animation.withChoices(entrance, entranceMillis, exit, exitMillis, length))
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

/** Corner radius of a decoration background, in dp at font scale 1. */
private const val DECORATION_BACKGROUND_RADIUS = 3.4f

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
    val style = TextStyle(
        color = look.color,
        fontSize = (look.fontSize * fit).sp,
        fontWeight = look.fontWeight,
        fontFamily = fontFamily,
        // 파일에 없는 굵기를 흉내 내지 않는다. 굵기가 하나뿐인 글꼴은 그 굵기로 그린다.
        fontSynthesis = FontSynthesis.None,
        letterSpacing = look.letterSpacingEm.em,
        lineHeight = 1.25.em,
        textAlign = look.textAlign,
    )
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
                            color = if (decoration?.bold == true && boldWeight == null) mainColor(char, decoration, look, wordColor) else Color.Transparent,
                        )
                        TextLayer.MAIN -> SpanStyle(
                            color = mainColor(char, decoration, look, wordColor),
                            fontWeight = if (decoration?.bold == true && boldWeight != null) boldWeight else null,
                            shadow = if (onBackground) Shadow.None else null,
                        )
                    }
                }
                val letter = if (char.word < 0) ' ' else text[char.source]
                if (span != null) withStyle(span) { append(letter) } else append(letter)
            }
        }
    }

    val background = look.background
    val cornerRadius = look.cornerRadius.dp
    var mainLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = frame.alpha
                scaleX = frame.scale
                scaleY = frame.scale
                translationY = frame.offsetY.dp.toPx()
                rotationZ = look.rotation
            }
            .drawWithContent {
                if (frame.textReveal >= 1f) {
                    drawContent()
                } else {
                    // 스펙처럼 위아래와 왼쪽은 넉넉히 남겨서, 그림자와 테두리가 잘리지 않게 한다.
                    clipRect(
                        left = -size.width * 0.1f,
                        top = -size.height * 0.3f,
                        right = size.width * frame.textReveal,
                        bottom = size.height * 1.3f,
                    ) { this@drawWithContent.drawContent() }
                }
            }
            .drawBehind {
                if (background.alpha > 0f) {
                    clipRect(right = size.width * frame.backgroundReveal) {
                        drawRoundRect(background, cornerRadius = CornerRadius(cornerRadius.toPx()))
                    }
                }
            }
            .padding(horizontal = (look.paddingHorizontal * fit).dp, vertical = (look.paddingVertical * fit).dp),
    ) {
        // 꾸미기 배경은 글자 뒤 모든 층보다 아래에 그린다.
        if (drawn.lines.any { line -> line.any { it.decoration?.background != null } }) {
            Box(
                modifier = Modifier.matchParentSize().drawBehind {
                    val layout = mainLayout ?: return@drawBehind
                    drawDecorationBackgrounds(drawn, layout, fit)
                },
            )
        }
        // 아래 레이어부터 그린다: 먼 그림자 → 꾸미기 그림자 → 테두리 → 본문(가까운 그림자 포함).
        look.shadows.drop(1).reversed().forEach { shadow ->
            Text(text = layer(TextLayer.FAR_SHADOW), style = style.copy(shadow = shadow.toShadow(density)), softWrap = false)
        }
        if (hasDecorations && decorations.any { it.decoration.shadow }) {
            Text(text = layer(TextLayer.DECORATION_SHADOW), style = style, softWrap = false)
        }
        if (look.outline) {
            Text(
                text = layer(TextLayer.STYLE_OUTLINE),
                style = style.copy(
                    color = look.outlineColor,
                    drawStyle = Stroke(width = with(density) { (look.outlineWidth * fit).dp.toPx() }, join = StrokeJoin.Round),
                ),
                softWrap = false,
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
            style = style.copy(shadow = look.shadows.firstOrNull()?.toShadow(density)),
            softWrap = false,
            onTextLayout = { mainLayout = it },
        )
    }
}

/** Color of a letter in the main layer. A background decoration decides it first, then a color decoration. */
private fun mainColor(char: DrawnChar, decoration: Decoration?, look: TextLook, wordColor: (Int) -> Color?): Color {
    decoration?.background?.let { return autoTextColor(it) }
    decoration?.color?.let { return rgbColor(it) }
    return (if (char.word >= 0) wordColor(char.word) else null) ?: look.color
}

/**
 * Draws the backgrounds of the background decorations. Neighbor letters with the same background share one box,
 * and each line has its own boxes.
 */
private fun DrawScope.drawDecorationBackgrounds(drawn: DrawnText, layout: TextLayoutResult, fit: Float) {
    val paddingV = (DECORATION_BACKGROUND_PADDING_V * fit).dp.toPx()
    val paddingH = (DECORATION_BACKGROUND_PADDING_H * fit).dp.toPx()
    val radius = CornerRadius((DECORATION_BACKGROUND_RADIUS * fit).dp.toPx())
    var offset = 0
    drawn.lines.forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) offset++
        val lineNumber = layout.getLineForOffset(offset.coerceAtMost(maxOf(0, layout.layoutInput.text.length - 1)))
        val top = layout.getLineTop(lineNumber)
        val bottom = layout.getLineBottom(lineNumber)
        var runStart = -1
        line.forEachIndexed { index, char ->
            val background = char.decoration?.background
            val next = line.getOrNull(index + 1)?.decoration?.background
            if (background != null && runStart < 0) runStart = index
            if (background != null && next != background) {
                val left = layout.getBoundingBox(offset + runStart).left
                val right = layout.getBoundingBox(offset + index).right
                drawRoundRect(
                    color = rgbColor(background),
                    topLeft = Offset(left - paddingH, top - paddingV + (bottom - top) * 0.06f),
                    size = Size(right - left + paddingH * 2, (bottom - top) * 0.88f + paddingV * 2),
                    cornerRadius = radius,
                )
                runStart = -1
            }
        }
        offset += line.size
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
        TextFontType.GELLYROLL, TextFontType.MONA12, TextFontType.OK_DANDAN -> null
    }

/** Splits a text into words at spaces. */
fun String.splitWords(): List<String> = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

private fun LookShadow.toShadow(density: Density): Shadow = with(density) {
    Shadow(color = color, offset = Offset(offsetX.dp.toPx(), offsetY.dp.toPx()), blurRadius = blur.dp.toPx())
}
