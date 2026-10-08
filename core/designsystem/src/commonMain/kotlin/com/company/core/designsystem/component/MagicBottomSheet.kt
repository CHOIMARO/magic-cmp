package com.company.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors

private val SheetEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Bottom sheet that slides up over the current screen.
 *
 * Put this composable last in a full-size `Box`. The sheet keeps the last non-null [value]
 * while it slides out, so the content does not disappear during the animation.
 *
 * @param value Data for the sheet content. Null hides the sheet.
 * @param onDismiss Called when the user taps the scrim or presses back.
 * @param modifier Modifier for the sheet container.
 * @param scrimAlpha Opacity of the black scrim.
 * @param spacing Vertical space between the content items.
 * @param content Content of the sheet. It receives the current or last [value].
 */
@Composable
fun <T : Any> MagicBottomSheet(
    value: T?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.4f,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    // 닫히는 애니메이션 동안 보여줄 마지막 값. 화면 상태가 아니므로 Snapshot 상태로 두지 않는다.
    val holder = remember { LastValueHolder<T>() }
    if (value != null) holder.value = value
    val lastValue = holder.value
    val visible = value != null

    SystemBackHandler(enabled = visible, onBack = onDismiss)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(260, easing = SheetEasing)) { it },
            exit = slideOutVertically(tween(200)) { it },
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(elevation = 24.dp, shape = SheetShape)
                    .clip(SheetShape)
                    .background(MagicColors.Surface)
                    // 시트 안쪽을 눌러도 스크림의 닫기 동작이 실행되지 않게 막는다.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
                lastValue?.let { content(it) }
            }
        }
    }
}

private class LastValueHolder<T : Any> {
    var value: T? = null
}

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
