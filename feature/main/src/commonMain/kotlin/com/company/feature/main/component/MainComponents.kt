package com.company.feature.main.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.DurationBadge
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.VideoSummary
import com.company.ui.editor.TemplatePreview
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTime
import com.company.ui.editor.subLabel

/**
 * "magic" logo: serif italic word and a sparkle.
 *
 * @param fontSize Size of the word.
 * @param iconSize Size of the sparkle.
 */
@Composable
internal fun MagicLogo(fontSize: TextUnit, iconSize: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = "magic",
            fontFamily = MagicTheme.fonts.serif,
            fontStyle = FontStyle.Italic,
            fontSize = fontSize,
            letterSpacing = (-0.02).em,
        )
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MagicColors.AccentLogo,
            modifier = Modifier.size(iconSize),
        )
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
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).em,
    )
}

/**
 * Section title with a "전체 보기" button.
 *
 * @param title Section title.
 * @param onShowAll Action of the button.
 */
@Composable
internal fun SectionHeader(title: String, onShowAll: () -> Unit) {
    // 텍스트 버튼은 좌우 여백 12dp가 있으므로 오른쪽 여백을 그만큼 줄여 글자 위치를 디자인과 맞춘다.
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        MagicButton(text = "전체 보기", onClick = onShowAll, style = MagicButtonStyle.TEXT)
    }
}

/**
 * Template card with a preview, a name, and an optional note.
 *
 * @param template Template to show.
 * @param scale Size factor of the preview content.
 * @param onClick Action on click.
 * @param modifier Modifier for the card.
 * @param isNew True to show the "NEW" badge.
 * @param showSubLabel True to show the note under the name.
 * @param nameSize Font size of the name.
 */
@Composable
internal fun TemplateCard(
    template: EditorTemplate,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    showSubLabel: Boolean = true,
    nameSize: TextUnit = 13.sp,
) {
    Column(modifier = modifier.pressClickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))) {
            TemplatePreview(template = template, scale = scale, modifier = Modifier.matchParentSize())
            if (isNew) {
                Text(
                    text = "NEW",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(MagicColors.Accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MagicColors.OnAccent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = template.displayName,
                fontSize = nameSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showSubLabel) {
                Text(text = template.subLabel, fontSize = 11.sp, color = MagicColors.OnSurface50, maxLines = 1)
            }
        }
    }
}

/**
 * Video card with a placeholder thumbnail, a duration badge, and a name.
 *
 * @param video Video to show.
 * @param onClick Action on click.
 * @param modifier Modifier for the card.
 * @param saved True to show the check badge of a completed video.
 * @param compact True for the small cards of the home tab.
 */
@Composable
internal fun VideoCard(
    video: VideoSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier.pressClickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StripeBox(
            hue = video.thumbnailHue.toFloat(),
            lightness = 0.46f,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f),
        ) {
            DurationBadge(
                text = formatTime(video.duration),
                modifier = Modifier.align(Alignment.BottomStart).padding(if (compact) 5.dp else 6.dp),
                fontSize = if (compact) 10.sp else 10.5.sp,
            )
            if (saved) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MagicColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(15.dp))
                }
            }
        }
        Text(
            text = video.name,
            modifier = Modifier.padding(start = if (compact) 0.dp else 2.dp),
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = if (compact) FontWeight.Medium else FontWeight.SemiBold,
            color = if (compact) MagicColors.OnSurface80 else MagicColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
