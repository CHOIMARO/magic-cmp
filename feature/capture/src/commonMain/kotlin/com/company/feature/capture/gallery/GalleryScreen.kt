package com.company.feature.capture.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicIconButtonStyle
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.SourceVideo
import com.company.ui.editor.GalleryMode
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTime

/**
 * Video picker screen. It only draws [state] and sends user actions out.
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 * @param onClose Action of the close button.
 */
@Composable
internal fun GalleryScreen(state: GalleryState, onIntent: (GalleryIntent) -> Unit, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MagicColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "닫기",
                    onClick = onClose,
                    style = MagicIconButtonStyle.SURFACE,
                )
                Text(text = "영상 선택", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(44.dp))
            }

            state.template?.let { template ->
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MagicColors.Accent.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Dashboard, contentDescription = null, tint = MagicColors.AccentPale, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${template.displayName} 템플릿을 입혀드려요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MagicColors.AccentPale,
                    )
                }
            }
            val layoutClipCount = (state.template as? EditorTemplate.Layout)?.layout?.clipCount ?: 1
            Text(
                text = if (layoutClipCount > 1) {
                    "${layoutClipCount}개를 고르면 딱 맞아요 · 누른 순서대로 들어가요"
                } else {
                    "보여주고 싶은 순서대로 눌러주세요"
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                fontSize = 13.sp,
                color = MagicColors.OnSurface60,
            )
            FixedGrid(
                items = state.videos,
                columns = 3,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 170.dp),
                horizontalSpacing = 3.dp,
                verticalSpacing = 3.dp,
            ) { video ->
                val order = state.pickedIds.indexOf(video.id)
                GalleryCell(video = video, order = order, onClick = { onIntent(GalleryIntent.TogglePick(video.id)) })
            }
        }

        GalleryBottomBar(state = state, onNext = { onIntent(GalleryIntent.Next) }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun GalleryCell(video: SourceVideo, order: Int, onClick: () -> Unit) {
    val picked = order >= 0
    StripeBox(
        hue = video.thumbnailHue.toFloat(),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .pressClickable(pressedScale = 0.98f, onClick = onClick),
    ) {
        if (picked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MagicColors.Accent.copy(alpha = 0.15f))
                    .border(3.dp, MagicColors.Accent)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(if (picked) MagicColors.Accent else Color.Black.copy(alpha = 0.25f))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (picked) {
                Text(text = "${order + 1}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MagicColors.OnAccent)
            }
        }
        Text(
            text = formatTime(video.duration),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 6.dp),
            style = LocalTextStyle.current.merge(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f),
            ),
        )
    }
}

@Composable
private fun GalleryBottomBar(state: GalleryState, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val count = state.pickedIds.size
    val hasPicked = count > 0
    val label = when {
        !hasPicked -> "영상을 골라주세요"
        state.mode == GalleryMode.APPEND -> "${count}개 추가"
        state.isMagicMode -> "${count}개로 매직 시작"
        else -> "${count}개 편집하기"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to MagicColors.Background))
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hasPicked) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.pickedIds.forEachIndexed { index, id ->
                    val video = state.videos.find { it.id == id } ?: return@forEachIndexed
                    StripeBox(
                        hue = video.thumbnailHue.toFloat(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 40.dp, height = 52.dp),
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
        // 고른 영상이 없으면 비활성 상태로 두고, 이유는 버튼 문구("영상을 골라주세요")로 알려준다.
        MagicButton(
            text = label,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            style = MagicButtonStyle.PRIMARY,
            leadingIcon = if (state.isMagicMode) Icons.Rounded.AutoAwesome else null,
            enabled = hasPicked,
        )
    }
}
