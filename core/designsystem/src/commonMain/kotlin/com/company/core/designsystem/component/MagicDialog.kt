package com.company.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.oklch

/** Height of the popup buttons. */
private val DialogButtonHeight = 48.dp

/** Colors of the cancel button. A light tint on the card keeps the button visible in all states. */
private val CancelButtonColors = MagicButtonDefaults.colors(MagicButtonStyle.SECONDARY).copy(
    container = MagicColors.OnSurface.copy(alpha = 0.08f),
    pressedContainer = MagicColors.OnSurface.copy(alpha = 0.14f),
)

/** Background of the input of [MagicInputDialog]. It is darker than the popup card. */
private val InputBackground = Color(0xFF1F1D25)

/** Shape of the input of [MagicInputDialog]. */
private val InputShape = RoundedCornerShape(14.dp)

/** Colors of the confirm button of an action that the user cannot undo, for example a delete. */
private val DangerConfirmColors = MagicButtonDefaults.colors(MagicButtonStyle.PRIMARY_SMALL).copy(
    container = oklch(0.6f, 0.2f, 25f),
    content = Color.White,
    pressedContainer = oklch(0.53f, 0.2f, 25f),
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
 * @param danger True for an action that the user cannot undo, for example a delete. The confirm button is red.
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
    danger: Boolean = false,
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
            danger = danger,
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
    danger: Boolean = false,
) {
    DialogCardFrame(
        title = title,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        cancelLabel = cancelLabel,
        onCancel = onCancel,
        danger = danger,
    ) {
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
    }
}

/**
 * Card of a popup: the title, [body], and the buttons. [MagicDialog] and [MagicInputDialog] share it, so that all
 * popups have the same look.
 */
@Composable
private fun DialogCardFrame(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String?,
    onCancel: () -> Unit,
    danger: Boolean = false,
    confirmEnabled: Boolean = true,
    body: @Composable () -> Unit,
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
            body()
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
                    colors = if (danger) DangerConfirmColors else MagicButtonDefaults.colors(MagicButtonStyle.PRIMARY_SMALL),
                    enabled = confirmEnabled,
                    height = DialogButtonHeight,
                )
            }
        }
    }
}

/**
 * Popup with a title, a one-line input, and two buttons. Use it to ask the user for a short text, for example a name.
 *
 * It has the look of [MagicDialog]. When it opens, the input has the focus and all of its text is selected. A counter
 * under the input shows the length and [maxLength]. A line break in a pasted text becomes a space.
 *
 * @param title Title of the popup.
 * @param initialText Text in the input when the popup opens.
 * @param maxLength Most characters in the input. A longer input does not go in.
 * @param confirmLabel Label of the confirm button.
 * @param cancelLabel Label of the cancel button.
 * @param onConfirm Called with the input when the user taps the confirm button or the done key of the keyboard.
 * @param onDismissRequest Called for the cancel button, the back action, and a tap outside the popup.
 * @param inputDescription Name of the input for screen readers.
 * @param canConfirm Tells if the confirm button is on for an input.
 */
@Composable
fun MagicInputDialog(
    title: String,
    initialText: String,
    maxLength: Int,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    inputDescription: String,
    canConfirm: (String) -> Boolean = { it.isNotBlank() },
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        MagicInputDialogCard(
            title = title,
            initialText = initialText,
            maxLength = maxLength,
            confirmLabel = confirmLabel,
            cancelLabel = cancelLabel,
            onConfirm = onConfirm,
            onCancel = onDismissRequest,
            inputDescription = inputDescription,
            canConfirm = canConfirm,
        )
    }
}

@Composable
private fun MagicInputDialogCard(
    title: String,
    initialText: String,
    maxLength: Int,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    inputDescription: String,
    canConfirm: (String) -> Boolean,
    focus: Boolean = true,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(0, initialText.length)))
    }
    val enabled = canConfirm(value.text)
    val focusRequester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DialogCardFrame(
        title = title,
        confirmLabel = confirmLabel,
        onConfirm = { if (enabled) onConfirm(value.text) },
        cancelLabel = cancelLabel,
        onCancel = onCancel,
        confirmEnabled = enabled,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { next ->
                // 줄을 바꿀 수 없는 한 줄 입력이다. 길이가 같아 선택 위치는 그대로 쓸 수 있다.
                val text = next.text.replace('\n', ' ').replace('\r', ' ')
                if (text.length <= maxLength) value = next.copy(text = text)
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = inputDescription },
            textStyle = TextStyle(color = MagicColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (enabled) onConfirm(value.text) }),
            singleLine = true,
            cursorBrush = SolidColor(MagicColors.Accent),
            decorationBox = { input ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(InputBackground, InputShape)
                        .border(1.5.dp, MagicColors.Accent, InputShape)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { input() }
            },
        )
        Text(
            text = "${value.text.length}/$maxLength",
            modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
            fontSize = 12.sp,
            color = MagicColors.OnSurface50,
            textAlign = TextAlign.End,
        )
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
            title = "기존 자막을 바꿀까요?",
            message = "지금 있는 자막 3개를 지우고 새로 넣어요",
            confirmLabel = "바꾸기",
            onConfirm = {},
            cancelLabel = "취소",
            onCancel = {},
        )
    }
}

@Preview(name = "MagicDialog - danger", widthDp = 360)
@Composable
private fun MagicDialogDangerPreview() {
    DialogPreview {
        MagicDialogCard(
            title = "이 작업을 삭제할까요?",
            message = "편집 내용은 되돌릴 수 없어요. 갤러리에 저장한 영상은 그대로 남아요.",
            confirmLabel = "삭제",
            onConfirm = {},
            cancelLabel = "취소",
            onCancel = {},
            danger = true,
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

@Preview(name = "MagicInputDialog", widthDp = 360)
@Composable
private fun MagicInputDialogPreview() {
    DialogPreview {
        MagicInputDialogCard(
            title = "이름 바꾸기",
            initialText = "새 작업 2",
            maxLength = 30,
            confirmLabel = "저장",
            cancelLabel = "취소",
            onConfirm = {},
            onCancel = {},
            inputDescription = "작업 이름",
            canConfirm = { it.isNotBlank() },
            focus = false,
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
