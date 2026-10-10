package com.company.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview

/** Small bar at the top of a bottom sheet. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MagicColors.OnSurface.copy(alpha = 0.25f))
    )
}

/**
 * Title row of an editor sheet with a "닫기" button.
 *
 * @param title Title of the sheet.
 * @param onClose Action of the close button.
 * @param closeLabel Label of the close button.
 */
@Composable
fun SheetHeader(title: String, onClose: () -> Unit, closeLabel: String = "닫기") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        // 디자인의 작은 "닫기" 버튼(36dp, 14sp)은 스펙에 없는 크기라서 크기는 두고 눌림 색만 맞춘다.
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val background by animateColorAsState(
            targetValue = if (pressed) MagicColors.SwitchTrackOff else MagicColors.SurfaceHighest,
            animationSpec = tween(PRESS_MILLIS),
        )
        Box(
            modifier = Modifier
                .height(36.dp)
                .pressClickable(interactionSource = interactionSource, onClick = onClose)
                .clip(CircleShape)
                .background(background)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = closeLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// MagicModalBottomSheet는 별도 창으로 열려 정적 프리뷰에 그려지지 않는다. 시트 표면의 모양만 보여 준다.
@Preview(name = "Sheet parts", widthDp = 360)
@Composable
private fun MagicSheetPartsPreview() {
    MagicPreview {
        Column(
            modifier = Modifier
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MagicColors.Surface)
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            SheetHeader(title = "필터", onClose = {})
            Text(text = "영상 전체에 적용돼요", fontSize = 13.sp, color = MagicColors.OnSurface60)
        }
    }
}
