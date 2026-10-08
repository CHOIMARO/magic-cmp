package com.company.feature.editor.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonDefaults
import com.company.core.designsystem.component.MagicButtonLoading
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.ui.editor.formatTime
import com.company.ui.editor.toColorFilter

/**
 * Export screen. It only draws [state] and sends user actions to [onIntent].
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun ExportScreen(state: ExportState, onIntent: (ExportIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MagicColors.Background)
            .navigationBarsPadding()
            .padding(bottom = 18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            MagicIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "뒤로",
                onClick = { onIntent(ExportIntent.Back) },
                iconSize = 26.dp,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp), contentAlignment = Alignment.Center) {
            ExportThumbnail(state = state)
        }
        when (state.phase) {
            ExportPhase.SETUP, ExportPhase.RUNNING -> SaveContent(state = state, onIntent = onIntent)
            ExportPhase.DONE -> DoneContent(onIntent = onIntent)
        }
    }
}

@Composable
private fun ExportThumbnail(state: ExportState) {
    val project = state.project
    val shape = RoundedCornerShape(18.dp)
    val filter = remember(project.filter) { project.filter.toColorFilter() }
    BoxWithConstraints(
        modifier = Modifier
            .width(176.dp)
            .aspectRatio(9f / 16f)
            .shadow(elevation = 24.dp, shape = shape)
            .clip(shape)
            .background(MagicColors.Surface),
    ) {
        val firstClip = project.clips.firstOrNull()
        if (firstClip != null) {
            StripeBox(hue = firstClip.thumbnailHue.toFloat(), lightness = 0.52f, colorFilter = filter, modifier = Modifier.fillMaxSize())
        }
        project.texts.firstOrNull()?.let { overlay ->
            Text(
                text = overlay.text,
                modifier = Modifier.fillMaxWidth().padding(top = maxHeight * 0.26f, start = 10.dp, end = 10.dp),
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current.merge(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 4f), 20f),
                ),
            )
        }
        when (state.phase) {
            ExportPhase.RUNNING -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    Text(text = "${state.progress}%", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(state.progress / 100f)
                        .height(5.dp)
                        .background(MagicColors.Accent),
                )
            }
            ExportPhase.DONE -> Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape).background(MagicColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(34.dp))
                }
            }
            ExportPhase.SETUP -> Unit
        }
    }
}

/**
 * Content of the setup and running steps.
 *
 * During the export, the save button keeps its size and color and shows the progress
 * ("저장하는 중 n%"). The text button under it changes to "취소".
 */
@Composable
private fun ColumnScope.SaveContent(state: ExportState, onIntent: (ExportIntent) -> Unit) {
    val running = state.phase == ExportPhase.RUNNING
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = if (running) "저장하는 중" else "다 됐어요!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = if (running) {
                    "앱을 닫지 말고 잠시만 기다려주세요"
                } else {
                    "${formatTime(state.project.totalDuration)} · ${state.resolution.label} · 약 ${state.estimatedMegabytes}MB"
                },
                fontSize = 14.sp,
                color = MagicColors.OnSurface60,
            )
        }
        if (state.showOptions && !running) {
            SegmentedControl(
                options = ExportResolution.entries.map { SegmentOption(it.label, it.note) },
                selectedIndex = state.resolution.ordinal,
                onSelect = { onIntent(ExportIntent.SelectResolution(ExportResolution.entries[it])) },
                contentColor = MagicColors.OnSurface80,
                itemHeight = 52.dp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        MagicButton(
            text = "저장하기",
            onClick = { onIntent(ExportIntent.Start) },
            modifier = Modifier.fillMaxWidth(),
            style = MagicButtonStyle.PRIMARY,
            leadingIcon = Icons.Rounded.Download,
            loading = if (running) MagicButtonLoading.Progress(label = "저장하는 중", fraction = state.progress / 100f) else null,
        )
        MagicButton(
            text = when {
                running -> "취소"
                state.showOptions -> "화질 설정 닫기"
                else -> "화질 바꾸기"
            },
            onClick = { onIntent(if (running) ExportIntent.Cancel else ExportIntent.ToggleOptions) },
            modifier = Modifier.align(Alignment.CenterHorizontally).offset(y = (-6).dp),
            style = MagicButtonStyle.TEXT,
        )
    }
}

private data class ShareTarget(val icon: ImageVector, val label: String)

private val ShareTargets = listOf(
    ShareTarget(Icons.Outlined.SmartDisplay, "쇼츠"),
    ShareTarget(Icons.Outlined.PhotoCamera, "릴스"),
    ShareTarget(Icons.Outlined.Link, "링크 복사"),
    ShareTarget(Icons.Outlined.Share, "더보기"),
)

@Composable
private fun ColumnScope.DoneContent(onIntent: (ExportIntent) -> Unit) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "갤러리에 저장했어요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "바로 올려볼까요?", fontSize = 14.sp, color = MagicColors.OnSurface60)
        }
        FixedGrid(items = ShareTargets, columns = 4, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { target ->
            Column(
                modifier = Modifier.fillMaxWidth().pressClickable {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(MagicColors.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(target.icon, contentDescription = null, modifier = Modifier.size(26.dp))
                }
                Text(text = target.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface80)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MagicButton(
                text = "더 편집하기",
                onClick = { onIntent(ExportIntent.Back) },
                modifier = Modifier.weight(1f),
                style = MagicButtonStyle.SECONDARY,
            )
            // 디자인의 "홈으로"는 밝은 버튼이다. 스펙에 없는 색이라 보조 버튼 크기에 칩의 눌림 색을 쓴다.
            MagicButton(
                text = "홈으로",
                onClick = { onIntent(ExportIntent.GoHome) },
                modifier = Modifier.weight(1f),
                style = MagicButtonStyle.SECONDARY,
                colors = MagicButtonDefaults.InverseColors,
            )
        }
    }
}
