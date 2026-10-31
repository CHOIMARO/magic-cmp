package com.company.feature.main.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicIconButtonStyle
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.domain.model.editor.EditorTemplate
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.show_all
import com.company.ui.editor.TemplatePreview
import com.company.ui.editor.displayName
import com.company.ui.editor.subLabel
import org.jetbrains.compose.resources.stringResource

/** Space under the content of a tab. The floating bottom tabs cover this space. */
internal val TabContentBottomPadding = 112.dp

/** CSS ease-in-out. */
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** "magic" logo: serif italic word and a sparkle that grows and turns. */
@Composable
internal fun MagicLogo() {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "magic",
            fontFamily = MagicTheme.fonts.serif,
            fontStyle = FontStyle.Italic,
            fontSize = 48.sp,
            lineHeight = 1.em,
            letterSpacing = (-0.02).em,
        )
        LogoSparkle(modifier = Modifier.padding(top = 3.dp).offset(x = (-1).dp))
    }
}

/** Four-point star next to the logo. One cycle takes 2.6 seconds. */
@Composable
private fun LogoSparkle(modifier: Modifier = Modifier) {
    val progress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = EaseInOut), RepeatMode.Reverse),
    )
    Canvas(
        modifier = modifier
            .size(15.dp)
            .graphicsLayer {
                val scale = 0.55f + 0.45f * progress
                scaleX = scale
                scaleY = scale
                rotationZ = 90f * progress
                alpha = 0.6f + 0.4f * progress
            },
    ) {
        val w = size.width
        val h = size.height
        // CSS clip-path: polygon(50% 0%, 60% 40%, 100% 50%, 60% 60%, 50% 100%, 40% 60%, 0% 50%, 40% 40%)
        val star = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.6f, h * 0.4f)
            lineTo(w, h * 0.5f)
            lineTo(w * 0.6f, h * 0.6f)
            lineTo(w * 0.5f, h)
            lineTo(w * 0.4f, h * 0.6f)
            lineTo(0f, h * 0.5f)
            lineTo(w * 0.4f, h * 0.4f)
            close()
        }
        drawPath(star, MagicColors.AccentSparkle)
    }
}

/**
 * Large title at the top of a tab.
 *
 * @param text Title text.
 * @param modifier Modifier for the title.
 */
@Composable
internal fun TabTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 42.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 1.em,
        letterSpacing = (-0.045).em,
    )
}

/**
 * Section title with a round arrow button that opens the full list.
 *
 * @param title Section title.
 * @param onShowAll Action of the arrow button, or null to hide the button. The header keeps its height.
 */
@Composable
internal fun SectionHeader(title: String, onShowAll: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 30.dp, bottom = 12.dp).heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em)
        if (onShowAll != null) {
            MagicIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(Res.string.show_all, title),
                onClick = onShowAll,
                style = MagicIconButtonStyle.SURFACE,
                size = 40.dp,
                iconSize = 20.dp,
            )
        }
    }
}

/**
 * Size of a [TemplateCard].
 *
 * @property previewScale Size factor of the preview content.
 * @property cornerRadius Corner radius of the preview.
 * @property spacing Space between the preview and the name.
 * @property nameSize Font size of the name.
 * @property nameWeight Font weight of the name.
 * @property nameLetterSpacing Letter spacing of the name.
 * @property showSubLabel True to show the note under the name.
 */
internal enum class TemplateCardSize(
    val previewScale: Float,
    val cornerRadius: Dp,
    val spacing: Dp,
    val nameSize: TextUnit,
    val nameWeight: FontWeight,
    val nameLetterSpacing: TextUnit,
    val showSubLabel: Boolean,
) {
    /** Card in the two-column grid of the template tab. */
    LARGE(0.7f, 20.dp, 10.dp, 16.sp, FontWeight.ExtraBold, (-0.02).em, showSubLabel = true),
}

/**
 * Template card with a preview, a name, and an optional note.
 *
 * @param template Template to show.
 * @param onClick Action on click.
 * @param modifier Modifier for the card.
 * @param size Size of the card.
 * @param isNew True to show the "NEW" badge.
 */
@Composable
internal fun TemplateCard(
    template: EditorTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: TemplateCardSize = TemplateCardSize.LARGE,
    isNew: Boolean = false,
) {
    Column(modifier = modifier.pressClickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(size.spacing)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(size.cornerRadius))) {
            TemplatePreview(template = template, scale = size.previewScale, modifier = Modifier.matchParentSize())
            if (isNew) {
                Text(
                    text = "NEW",
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(MagicColors.OnSurface)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = MagicColors.Background,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.04.em,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = template.displayName,
                fontSize = size.nameSize,
                fontWeight = size.nameWeight,
                letterSpacing = size.nameLetterSpacing,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (size.showSubLabel) {
                Text(text = template.subLabel, fontSize = 12.sp, color = MagicColors.OnSurface50, maxLines = 1)
            }
        }
    }
}
