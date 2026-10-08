package com.company.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.TextStyleType

/**
 * Soft shadow behind text.
 *
 * @property color Color of the shadow.
 * @property offsetY Vertical offset in dp.
 * @property blur Blur radius in dp.
 */
@Immutable
data class LookShadow(val color: Color, val offsetY: Float, val blur: Float)

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
 * @property serif True for the serif italic font.
 * @property shadows Shadows. The first shadow is on top.
 * @property outline True to draw a black outline around the letters.
 * @property paddingHorizontal Horizontal padding of the text box.
 * @property paddingVertical Vertical padding of the text box.
 * @property cornerRadius Corner radius of the text box in dp.
 * @property letterSpacingEm Letter spacing in em.
 * @property bottomPercent Distance of a caption from the bottom, in percent of the screen height.
 */
@Immutable
data class TextLook(
    val color: Color = Color.White,
    val highlightColor: Color = Color.White,
    val background: Color = Color.Transparent,
    val fontSize: Float,
    val fontWeight: FontWeight,
    val serif: Boolean = false,
    val shadows: List<LookShadow> = emptyList(),
    val outline: Boolean = false,
    val paddingHorizontal: Float = 0f,
    val paddingVertical: Float = 0f,
    val cornerRadius: Float = 8f,
    val letterSpacingEm: Float = 0f,
    val bottomPercent: Float = 17f,
)

private val DefaultTextShadow = LookShadow(Color.Black.copy(alpha = 0.5f), offsetY = 2f, blur = 12f)
private val NeonText = Color(0xFFFFDBFF)
private val NeonGlowNear = Color(0xFFE660FF)
private val NeonGlowFar = Color(0xFFD852F5)
private val MarkerYellow = Color(0xFFFDDC5B)
private val PopYellow = Color(0xFFFBD35F)

/**
 * Look of a text overlay.
 *
 * @param scale Size factor. 1 is the editor stage. Previews use smaller values.
 */
fun TextStyleType.look(scale: Float = 1f): TextLook {
    val base = TextLook(fontSize = 24f * scale, fontWeight = FontWeight.ExtraBold, shadows = listOf(DefaultTextShadow))
    return when (this) {
        TextStyleType.BOLD -> base
        TextStyleType.BOX -> base.copy(
            color = Color(0xFF111111), background = Color.White, fontSize = 18f * scale,
            fontWeight = FontWeight.Bold, shadows = emptyList(),
            paddingVertical = 6f * scale, paddingHorizontal = 11f * scale,
        )
        TextStyleType.OUTLINE -> base.copy(shadows = emptyList(), outline = true)
        TextStyleType.SERIF -> base.copy(
            serif = true, fontWeight = FontWeight.Normal, fontSize = 32f * scale,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.45f), offsetY = 2f, blur = 14f)),
        )
        TextStyleType.NEON -> base.copy(
            color = NeonText,
            shadows = listOf(LookShadow(NeonGlowNear, 0f, 6f), LookShadow(NeonGlowFar, 0f, 16f)),
        )
        TextStyleType.MARKER -> base.copy(
            color = Color(0xFF111111), background = MarkerYellow, fontSize = 19f * scale,
            paddingVertical = 2f * scale, paddingHorizontal = 8f * scale, cornerRadius = 2f, shadows = emptyList(),
        )
        TextStyleType.TAG -> base.copy(
            color = MagicColors.OnAccent, background = MagicColors.Accent, fontSize = 14f * scale,
            fontWeight = FontWeight.Bold, paddingVertical = 5f * scale, paddingHorizontal = 12f * scale,
            cornerRadius = 99f, shadows = emptyList(), letterSpacingEm = 0.03f,
        )
    }
}

/**
 * Look of the automatic captions.
 *
 * @param scale Size factor. 1 is the editor stage. Previews use smaller values.
 */
fun CaptionStyleType.look(scale: Float = 1f): TextLook {
    val base = TextLook(
        fontSize = 17f * scale,
        fontWeight = FontWeight.ExtraBold,
        outline = true,
        shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.6f), offsetY = 2f, blur = 8f)),
        cornerRadius = 6f,
    )
    return when (this) {
        CaptionStyleType.BASIC -> base
        CaptionStyleType.POP -> base.copy(highlightColor = PopYellow)
        CaptionStyleType.BOX -> base.copy(
            fontWeight = FontWeight.SemiBold, fontSize = 15f * scale, background = Color.Black.copy(alpha = 0.72f),
            paddingVertical = 4f * scale, paddingHorizontal = 9f * scale, outline = false, shadows = emptyList(),
        )
        CaptionStyleType.MINIMAL -> base.copy(
            fontWeight = FontWeight.Medium, fontSize = 14f * scale, outline = false,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.7f), offsetY = 1f, blur = 6f)), bottomPercent = 9f,
        )
        CaptionStyleType.SERIF -> base.copy(
            serif = true, fontWeight = FontWeight.Normal, fontSize = 22f * scale, outline = false,
            shadows = listOf(LookShadow(Color.Black.copy(alpha = 0.6f), offsetY = 2f, blur = 10f)),
        )
        CaptionStyleType.BIG -> base.copy(
            fontSize = 25f * scale, fontWeight = FontWeight.Black, highlightColor = MagicColors.Accent, bottomPercent = 40f,
        )
    }
}

/**
 * Text drawn with a [TextLook]: background box, outline, and shadows.
 *
 * @param text Text to draw.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param cornerRadiusOverride Corner radius that replaces [TextLook.cornerRadius], or null.
 * @param maxLines Maximum number of lines.
 */
@Composable
fun LookText(
    text: AnnotatedString,
    look: TextLook,
    modifier: Modifier = Modifier,
    cornerRadiusOverride: Float? = null,
    maxLines: Int = 1,
) {
    val fonts = MagicTheme.fonts
    val density = LocalDensity.current
    val style = TextStyle(
        color = look.color,
        fontSize = look.fontSize.sp,
        fontWeight = look.fontWeight,
        fontFamily = if (look.serif) fonts.serif else fonts.sans,
        fontStyle = if (look.serif) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = look.letterSpacingEm.em,
        lineHeight = 1.25.em,
        textAlign = TextAlign.Center,
    )
    Box(
        modifier = modifier
            .background(look.background, RoundedCornerShape((cornerRadiusOverride ?: look.cornerRadius).dp))
            .padding(horizontal = look.paddingHorizontal.dp, vertical = look.paddingVertical.dp),
    ) {
        // 아래 레이어부터 그린다: 먼 그림자 → 테두리 → 본문(가까운 그림자 포함).
        look.shadows.drop(1).reversed().forEach { shadow ->
            Text(text = text, style = style.copy(shadow = shadow.toShadow(density)), maxLines = maxLines, softWrap = maxLines > 1)
        }
        if (look.outline) {
            Text(
                text = text.text,
                style = style.copy(
                    color = Color.Black,
                    drawStyle = Stroke(width = with(density) { 3.dp.toPx() }, join = StrokeJoin.Round),
                ),
                maxLines = maxLines,
                softWrap = maxLines > 1,
            )
        }
        Text(
            text = text,
            style = style.copy(shadow = look.shadows.firstOrNull()?.toShadow(density)),
            maxLines = maxLines,
            softWrap = maxLines > 1,
        )
    }
}

/**
 * Overlay text with one color.
 *
 * @param text Text to draw.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 */
@Composable
fun OverlayText(text: String, look: TextLook, modifier: Modifier = Modifier) {
    LookText(text = AnnotatedString(text), look = look, modifier = modifier)
}

/**
 * Caption text. The words that the speaker already said use [TextLook.highlightColor].
 *
 * @param spoken Words that the speaker already said.
 * @param upcoming Words that come next.
 * @param look Visual style.
 * @param modifier Modifier for the text box.
 * @param maxLines Maximum number of lines.
 */
@Composable
fun CaptionText(
    spoken: String,
    upcoming: String,
    look: TextLook,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = look.highlightColor)) { append(spoken) }
        append(" ")
        append(upcoming)
    }
    LookText(text = text, look = look, modifier = modifier, cornerRadiusOverride = null, maxLines = maxLines)
}

private fun LookShadow.toShadow(density: Density): Shadow = with(density) {
    Shadow(color = color, offset = Offset(0f, offsetY.dp.toPx()), blurRadius = blur.dp.toPx())
}
