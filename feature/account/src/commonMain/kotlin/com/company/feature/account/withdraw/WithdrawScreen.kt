package com.company.feature.account.withdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonColors
import com.company.core.designsystem.component.MagicButtonLoading
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicDialog
import com.company.core.designsystem.component.SystemBackHandler
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.core.designsystem.theme.oklch
import com.company.feature.account.component.AccountTopBar
import com.company.feature.account.resources.Res
import com.company.feature.account.resources.toast_withdrawn
import com.company.feature.account.resources.withdraw_busy
import com.company.feature.account.resources.withdraw_button
import com.company.feature.account.resources.withdraw_cancel
import com.company.feature.account.resources.withdraw_confirm
import com.company.feature.account.resources.withdraw_confirm_message
import com.company.feature.account.resources.withdraw_confirm_title
import com.company.feature.account.resources.withdraw_gone
import com.company.feature.account.resources.withdraw_gone_account
import com.company.feature.account.resources.withdraw_gone_account_sub
import com.company.feature.account.resources.withdraw_gone_voice
import com.company.feature.account.resources.withdraw_gone_voice_sub
import com.company.feature.account.resources.withdraw_headline
import com.company.feature.account.resources.withdraw_keep
import com.company.feature.account.resources.withdraw_keep_dubbings
import com.company.feature.account.resources.withdraw_keep_gallery
import com.company.feature.account.resources.withdraw_keep_works
import com.company.feature.account.resources.withdraw_title
import com.company.ui.text.resolve
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Time on screen of the toast after the withdrawal. */
private const val WITHDRAWN_TOAST_MILLIS = 2600L

/** Time on screen of the failure toast. */
private const val FAILURE_TOAST_MILLIS = 2200L

/** Colors of the withdrawal button. It is red because the user cannot undo the action. */
private val WithdrawButtonColors = MagicButtonColors(
    container = oklch(0.6f, 0.2f, 25f),
    content = Color.White,
    pressedContainer = oklch(0.54f, 0.2f, 25f),
    disabledContainer = MagicColors.DangerDisabled,
    disabledContent = MagicColors.OnSurface.copy(alpha = 0.35f),
    progressFill = Color.White.copy(alpha = 0.18f),
    spinner = Color.White,
    spinnerTrack = Color.White.copy(alpha = 0.25f),
)

/**
 * Connects [WithdrawViewModel] to [WithdrawScreen] and sends navigation events out.
 *
 * @param onClose Leaves the screen.
 * @param onWithdrawn Goes to the home tab after the withdrawal.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun WithdrawRoute(onClose: () -> Unit, onWithdrawn: () -> Unit, viewModel: WithdrawViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalMagicToast.current
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                WithdrawSideEffect.Close -> onClose()
                WithdrawSideEffect.Withdrawn -> {
                    // 토스트는 화면을 옮겨도 남는다. 홈 화면에서 인사를 보여 준다.
                    toast.show(getString(Res.string.toast_withdrawn), WITHDRAWN_TOAST_MILLIS)
                    onWithdrawn()
                }
                is WithdrawSideEffect.ShowToast -> toast.show(effect.message.resolve(), FAILURE_TOAST_MILLIS)
            }
        }
    }
    WithdrawScreen(state = state, onIntent = { viewModel.postIntent(it) })
}

/**
 * Withdrawal screen (S16): what goes away, what stays, and the withdrawal button.
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun WithdrawScreen(state: WithdrawState, onIntent: (WithdrawIntent) -> Unit) {
    // 서버가 계정을 지우는 동안에는 뒤로 가지 않는다.
    SystemBackHandler(enabled = state.busy) {}

    Column(modifier = Modifier.fillMaxSize().background(MagicColors.Background).navigationBarsPadding()) {
        AccountTopBar(
            title = stringResource(Res.string.withdraw_title),
            onBack = { onIntent(WithdrawIntent.Back) },
            backEnabled = !state.busy,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.withdraw_headline),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 34.sp,
                letterSpacing = (-0.03).em,
            )
            ListCard(
                icon = Icons.Rounded.Delete,
                title = stringResource(Res.string.withdraw_gone),
                color = oklch(0.8f, 0.12f, 25f),
                items = buildList {
                    add(Res.string.withdraw_gone_account to Res.string.withdraw_gone_account_sub)
                    if (state.hasMyVoice) add(Res.string.withdraw_gone_voice to Res.string.withdraw_gone_voice_sub)
                },
            )
            ListCard(
                icon = Icons.Rounded.CheckCircle,
                title = stringResource(Res.string.withdraw_keep),
                color = oklch(0.85f, 0.1f, 160f),
                items = listOf(
                    Res.string.withdraw_keep_works to null,
                    Res.string.withdraw_keep_dubbings to null,
                    Res.string.withdraw_keep_gallery to null,
                ),
            )
        }
        MagicButton(
            text = stringResource(Res.string.withdraw_button),
            onClick = { onIntent(WithdrawIntent.RequestWithdraw) },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp).fillMaxWidth(),
            style = MagicButtonStyle.PRIMARY,
            colors = WithdrawButtonColors,
            loading = if (state.busy) MagicButtonLoading.Spinner(stringResource(Res.string.withdraw_busy)) else null,
        )
    }

    if (state.confirm) {
        MagicDialog(
            title = stringResource(Res.string.withdraw_confirm_title),
            message = stringResource(Res.string.withdraw_confirm_message),
            confirmLabel = stringResource(Res.string.withdraw_confirm),
            onConfirm = { onIntent(WithdrawIntent.ConfirmWithdraw) },
            cancelLabel = stringResource(Res.string.withdraw_cancel),
            onCancel = { onIntent(WithdrawIntent.DismissWithdraw) },
            onDismissRequest = { onIntent(WithdrawIntent.DismissWithdraw) },
            danger = true,
        )
    }
}

/**
 * Card with a header and a list of items.
 *
 * @param icon Icon of the header.
 * @param title Text of the header.
 * @param color Color of the header.
 * @param items Title and optional note of each item.
 */
@Composable
private fun ListCard(icon: ImageVector, title: String, color: Color, items: List<Pair<StringResource, StringResource?>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MagicColors.Surface)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        items.forEach { (itemTitle, note) ->
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MagicColors.Divider))
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = stringResource(itemTitle), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
                if (note != null) {
                    Text(text = stringResource(note), fontSize = 13.sp, lineHeight = 19.sp, color = MagicColors.OnSurface55)
                }
            }
        }
    }
}

@Preview(name = "회원 탈퇴", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun WithdrawScreenPreview() {
    MagicPreview { WithdrawScreen(state = WithdrawState(hasMyVoice = true), onIntent = {}) }
}

@Preview(name = "회원 탈퇴 · 처리 중", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun WithdrawScreenBusyPreview() {
    MagicPreview { WithdrawScreen(state = WithdrawState(busy = true), onIntent = {}) }
}
