package com.company.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview

/** Height of the popup buttons. */
private val DialogButtonHeight = 48.dp

/** Colors of the cancel button. A light tint on the card keeps the button visible in all states. */
private val CancelButtonColors = MagicButtonDefaults.colors(MagicButtonStyle.SECONDARY).copy(
    container = MagicColors.OnSurface.copy(alpha = 0.08f),
    pressedContainer = MagicColors.OnSurface.copy(alpha = 0.14f),
)

/**
 * Popup with a title, a message, and one or two buttons. Use it to ask the user before an action.
 *
 * The title and the message are in the center. With two buttons, each button gets half of the width.
 * The cancel button is on the left, and the confirm button is on the right.
 *
 * @param title Title of the popup.
 * @param message Text under the title. The message area has the height of two lines or more.
 * @param confirmLabel Label of the confirm button.
 * @param onConfirm Action of the confirm button.
 * @param cancelLabel Label of the cancel button. Null shows only the confirm button.
 * @param onCancel Action of the cancel button.
 * @param onDismissRequest Action of the back action and of a tap outside the popup. Null keeps the popup open
 *   until the user taps a button.
 */
@Composable
fun MagicDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String? = null,
    onCancel: () -> Unit = {},
    onDismissRequest: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismissRequest != null,
            dismissOnClickOutside = onDismissRequest != null,
            usePlatformDefaultWidth = false,
        ),
    ) {
        MagicDialogCard(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
            cancelLabel = cancelLabel,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun MagicDialogCard(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String?,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .background(MagicColors.SurfaceHighest, RoundedCornerShape(28.dp))
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                // 한 줄 메시지도 두 줄 높이의 가운데에 두어, 팝업마다 높이가 크게 달라지지 않게 한다.
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 42.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MagicColors.OnSurface70,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cancelLabel != null) {
                    MagicButton(
                        text = cancelLabel,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        style = MagicButtonStyle.SECONDARY,
                        colors = CancelButtonColors,
                        height = DialogButtonHeight,
                    )
                }
                MagicButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    style = MagicButtonStyle.PRIMARY_SMALL,
                    height = DialogButtonHeight,
                )
            }
        }
    }
}

/** Shows the popup card on the app background. A preview cannot show the popup window itself. */
@Composable
private fun DialogPreview(content: @Composable () -> Unit) {
    MagicPreview {
        Box(modifier = Modifier.padding(vertical = 24.dp)) { content() }
    }
}

@Preview(name = "MagicDialog - two buttons", widthDp = 360)
@Composable
private fun MagicDialogTwoButtonsPreview() {
    DialogPreview {
        MagicDialogCard(
            title = "편집을 그만둘까요?",
            message = "지금 나가면 편집한 내용이 사라져요",
            confirmLabel = "나가기",
            onConfirm = {},
            cancelLabel = "취소",
            onCancel = {},
        )
    }
}

@Preview(name = "MagicDialog - one button", widthDp = 360)
@Composable
private fun MagicDialogOneButtonPreview() {
    DialogPreview {
        MagicDialogCard(
            title = "저장했어요",
            message = "갤러리에서 영상을 볼 수 있어요",
            confirmLabel = "확인",
            onConfirm = {},
            cancelLabel = null,
            onCancel = {},
        )
    }
}

@Preview(name = "MagicDialog - long text", widthDp = 360)
@Composable
private fun MagicDialogLongTextPreview() {
    DialogPreview {
        MagicDialogCard(
            title = "제목이 길어지면 이렇게 여러 줄로 보여요. 가운데 정렬은 그대로예요",
            message = "메시지가 길어지면 이렇게 여러 줄로 보여요. 메시지 영역은 두 줄보다 짧아지지 않고, 길어지면 내용만큼 늘어나요",
            confirmLabel = "확인",
            onConfirm = {},
            cancelLabel = "취소",
            onCancel = {},
        )
    }
}
