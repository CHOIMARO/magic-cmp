package com.company.feature.main.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicActionCard
import com.company.core.designsystem.component.MagicActionCardStyle
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonDefaults
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicVideoCard
import com.company.core.designsystem.theme.MagicColors
import com.company.domain.model.editor.VideoSummary
import com.company.feature.main.component.MagicLogo
import com.company.feature.main.component.SectionHeader
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.createdLabel
import com.company.feature.main.component.rememberNow
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.home_camera
import com.company.feature.main.resources.home_empty_action
import com.company.feature.main.resources.home_empty_body
import com.company.feature.main.resources.home_empty_title
import com.company.feature.main.resources.home_new_video
import com.company.feature.main.resources.home_recent
import com.company.ui.editor.formatTime
import org.jetbrains.compose.resources.stringResource

/** Side padding of the home tab. */
private val HomePadding = 16.dp

/**
 * Home tab: start cards and recent drafts.
 *
 * Each draft card shows the day when the user made the project. Without drafts, the tab shows a guide card in place of
 * the list, and the section header has no arrow.
 *
 * @param drafts Draft projects. The newest draft is first.
 * @param onNewVideo Action of the "새 영상 만들기" card and the button of the guide card.
 * @param onOpenCamera Action of the "촬영" card.
 * @param onShowAllDrafts Action of the arrow in the draft section.
 * @param onOpenProject Called with the name of the tapped draft.
 */
@Composable
internal fun HomeTab(
    drafts: List<VideoSummary>,
    onNewVideo: () -> Unit,
    onOpenCamera: () -> Unit,
    onShowAllDrafts: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = HomePadding, end = HomePadding, top = 12.dp, bottom = TabContentBottomPadding),
    ) {
        Row(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 22.dp)) {
            MagicLogo()
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MagicActionCard(
                title = stringResource(Res.string.home_new_video),
                icon = Icons.Rounded.Add,
                onClick = onNewVideo,
                modifier = Modifier.weight(1.3f),
            )
            MagicActionCard(
                title = stringResource(Res.string.home_camera),
                icon = Icons.Rounded.PhotoCamera,
                onClick = onOpenCamera,
                modifier = Modifier.weight(1f),
                style = MagicActionCardStyle.SECONDARY,
            )
        }

        SectionHeader(
            title = stringResource(Res.string.home_recent),
            onShowAll = if (drafts.isNotEmpty()) onShowAllDrafts else null,
        )
        if (drafts.isEmpty()) {
            NoDraftsGuide(onPick = onNewVideo)
        } else {
            val now = rememberNow()
            FixedGrid(items = drafts, columns = 2, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { draft ->
                MagicVideoCard(
                    title = draft.name,
                    duration = formatTime(draft.duration),
                    hue = draft.thumbnailHue.toFloat(),
                    onClick = { onOpenProject(draft.name) },
                    modifier = Modifier.fillMaxWidth(),
                    caption = createdLabel(draft.createdAt, now),
                )
            }
        }
    }
}

/** Guide card with a dashed border. It shows in place of the draft list when the user has no drafts. */
@Composable
private fun NoDraftsGuide(onPick: () -> Unit) {
    val borderColor = MagicColors.OnSurface.copy(alpha = 0.16f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val width = 1.5.dp.toPx()
                val dash = 4.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(width / 2, width / 2),
                    size = size.copy(width = size.width - width, height = size.height - width),
                    cornerRadius = CornerRadius(26.dp.toPx() - width / 2),
                    style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )
            }
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Movie, contentDescription = null, tint = MagicColors.AccentSoft, modifier = Modifier.size(36.dp))
        Text(
            text = stringResource(Res.string.home_empty_title),
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.02).em,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.home_empty_body),
            fontSize = 14.sp,
            lineHeight = 1.55.em,
            color = MagicColors.OnSurface60,
            textAlign = TextAlign.Center,
        )
        MagicButton(
            text = stringResource(Res.string.home_empty_action),
            onClick = onPick,
            modifier = Modifier.padding(top = 8.dp),
            style = MagicButtonStyle.PRIMARY_SMALL,
            colors = MagicButtonDefaults.InverseColors,
        )
    }
}
