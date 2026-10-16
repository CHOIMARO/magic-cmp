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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    SheetHeader(onClose = onClose, closeLabel = closeLabel) {
        Text(text = title, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Top row of an editor sheet with custom content in place of the title, for example a text field.
 *
 * While the keyboard is open, the close button shows a "hide keyboard" icon. Then a tap hides the keyboard and does
 * not close the sheet. When the keyboard is closed, the button shows [closeLabel] again and closes the sheet.
 *
 * @param onClose Action of the close button.
 * @param closeLabel Label of the close button.
 * @param action Optional button before the close button, for example the main action of the sheet.
 *   Make it [SheetHeaderButtonHeight] high.
 * @param content Content at the start of the row. Give it `Modifier.weight(1f)` to fill the free width.
 */
@Composable
fun SheetHeader(
    onClose: () -> Unit,
    closeLabel: String = "닫기",
    action: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val keyboard = WindowInsets.sheetIme
    val density = LocalDensity.current
    val keyboardOpen by remember(keyboard, density) { derivedStateOf { keyboard.getBottom(density) > 0 } }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        action?.invoke()
        // 디자인의 작은 "닫기" 버튼(36dp, 14sp)은 스펙에 없는 크기라서 크기는 두고 눌림 색만 맞춘다.
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val background by animateColorAsState(
            targetValue = if (pressed) MagicColors.SwitchTrackOff else MagicColors.SurfaceHighest,
            animationSpec = tween(PRESS_MILLIS),
        )
        Box(
            modifier = Modifier
                .height(SheetHeaderButtonHeight)
                .pressClickable(
                    interactionSource = interactionSource,
                    onClick = {
                        if (keyboardOpen) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        } else {
                            onClose()
                        }
                    },
                )
                .clip(CircleShape)
                .background(background)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 아이콘으로 바뀌어도 버튼 폭은 "닫기" 글자 폭을 유지한다. 그래야 입력창 폭이 흔들리지 않는다.
            Text(
                text = closeLabel,
                modifier = if (keyboardOpen) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (keyboardOpen) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardHide,
                    contentDescription = "키보드 내리기",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Height of the buttons in a [SheetHeader]. */
val SheetHeaderButtonHeight = 36.dp

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
