package com.company.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.model.editor.TextStyleType

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

/**
 * Look of a text overlay.
 *
 * The entrance, the exit, and the line rules come from the "Magic Text Templates" spec.
 *
 * @param scale Size factor. 1 is the editor stage. Previews use smaller values.
 * @param font Font that replaces the font of the style, or null to keep the font of the style.
 */
fun TextStyleType.look(scale: Float = 1f, font: TextFontType? = null): TextLook {
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
    return look.copy(font = font ?: look.font).withLineLimit().scaled(scale)
}

/**
 * Look of the automatic captions.
 *
 * The entrance, the exit, the highlight, and the line rules come from the "Magic Text Templates" spec.
 *
 * @param scale Size factor. 1 is the editor stage. Previews use smaller values.
 */
fun CaptionStyleType.look(scale: Float = 1f): TextLook {
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
    return look.withLineLimit().scaled(scale)
}

/**
 * Text drawn with a [TextLook]: background box, outline, and shadows.
 *
 * The words break into lines with the overflow rule of the look. Each layer draws the same text,
 * so the layers line up.
 *
 * @param words Words of the text.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param frame Animation state of the text.
 * @param wordColor Color of the word at an index, or null for the text color.
 * @param wordHidden True for a word at an index that is not on screen yet. The word keeps its space.
 */
@Composable
private fun LookText(
    words: List<String>,
    look: TextLook,
    modifier: Modifier = Modifier,
    frame: TextFrame = TextFrame.Static,
    wordColor: (Int) -> Color? = { null },
    wordHidden: (Int) -> Boolean = { false },
) {
    val fontFamily = look.font.fontFamily()
    val density = LocalDensity.current
    val lines = remember(words, look) { look.breakLines(words) }
    val fit = lines.fontScale
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

    /** Text of one layer. A highlighted word keeps the layer color when [useWordColors] is false. */
    fun layer(useWordColors: Boolean): AnnotatedString = buildAnnotatedString {
        var index = 0
        lines.lines.forEachIndexed { lineIndex, line ->
            if (lineIndex > 0) append('\n')
            line.forEachIndexed { wordIndex, word ->
                if (wordIndex > 0) append(' ')
                val color = if (useWordColors) wordColor(index) else null
                when {
                    wordHidden(index) -> withStyle(SpanStyle(color = Color.Transparent, shadow = Shadow.None)) { append(word) }
                    color != null -> withStyle(SpanStyle(color = color)) { append(word) }
                    else -> append(word)
                }
                index++
            }
        }
    }

    val background = look.background
    val cornerRadius = look.cornerRadius.dp
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
        // 아래 레이어부터 그린다: 먼 그림자 → 테두리 → 본문(가까운 그림자 포함).
        look.shadows.drop(1).reversed().forEach { shadow ->
            Text(text = layer(useWordColors = false), style = style.copy(shadow = shadow.toShadow(density)), softWrap = false)
        }
        if (look.outline) {
            Text(
                text = layer(useWordColors = false),
                style = style.copy(
                    color = look.outlineColor,
                    drawStyle = Stroke(width = with(density) { (look.outlineWidth * fit).dp.toPx() }, join = StrokeJoin.Round),
                ),
                softWrap = false,
            )
        }
        Text(
            text = layer(useWordColors = true),
            style = style.copy(shadow = look.shadows.firstOrNull()?.toShadow(density)),
            softWrap = false,
        )
    }
}

/**
 * Overlay text with one color.
 *
 * @param text Text to draw.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param frame Animation state of the text.
 */
@Composable
fun OverlayText(text: String, look: TextLook, modifier: Modifier = Modifier, frame: TextFrame = TextFrame.Static) {
    LookText(words = text.splitWords(), look = look, modifier = modifier, frame = frame)
}

/**
 * Caption text. The look decides how the current word shows: with the highlight color, and for
 * some looks, with the next words hidden.
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
    LookText(
        words = words,
        look = look,
        modifier = modifier,
        frame = frame,
        wordColor = { index -> if (look.highlight == CaptionHighlight.CURRENT && index == currentWord) look.highlightColor else null },
        wordHidden = { index -> look.revealByWord && index > currentWord },
    )
}

/** Splits a text into words at spaces. */
fun String.splitWords(): List<String> = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

private fun LookShadow.toShadow(density: Density): Shadow = with(density) {
    Shadow(color = color, offset = Offset(offsetX.dp.toPx(), offsetY.dp.toPx()), blurRadius = blur.dp.toPx())
}
