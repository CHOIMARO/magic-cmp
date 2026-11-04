package com.company.feature.account.manage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.Member
import com.company.feature.account.component.AccountTopBar
import com.company.feature.account.component.KakaoYellow
import com.company.feature.account.component.logo
import com.company.feature.account.resources.Res
import com.company.feature.account.resources.account_group_login
import com.company.feature.account.resources.account_provider_apple
import com.company.feature.account.resources.account_provider_google
import com.company.feature.account.resources.account_provider_kakao
import com.company.feature.account.resources.account_signed_in_with
import com.company.feature.account.resources.account_title
import com.company.feature.account.resources.account_withdraw
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [AccountViewModel] to [AccountScreen] and sends navigation events out.
 *
 * @param onClose Leaves the screen.
 * @param onOpenWithdraw Opens the withdrawal screen (S16).
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun AccountRoute(onClose: () -> Unit, onOpenWithdraw: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                AccountSideEffect.Close -> onClose()
                AccountSideEffect.OpenWithdraw -> onOpenWithdraw()
            }
        }
    }
    AccountScreen(state = state, onIntent = { viewModel.postIntent(it) })
}

/**
 * Account management screen (S15): the login provider of the account and the withdrawal.
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun AccountScreen(state: AccountState, onIntent: (AccountIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MagicColors.Background).navigationBarsPadding()) {
        AccountTopBar(title = stringResource(Res.string.account_title), onBack = { onIntent(AccountIntent.Back) })
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.account_group_login),
                modifier = Modifier.padding(start = 4.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MagicColors.OnSurface50,
            )
            state.member?.let { LoginMethodCard(member = it) }
            Spacer(modifier = Modifier.weight(1f).heightIn(min = 24.dp))
            WithdrawRow(onClick = { onIntent(AccountIntent.OpenWithdraw) })
        }
    }
}

@Composable
private fun LoginMethodCard(member: Member) {
    val provider = member.providers.firstOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MagicColors.Surface).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (provider != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (provider == LoginProvider.KAKAO) KakaoYellow else Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(imageVector = provider.logo, contentDescription = null, modifier = Modifier.size(21.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider?.let { stringResource(Res.string.account_signed_in_with, stringResource(it.label)) }.orEmpty(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            // 카카오는 이메일을 주지 않을 수 있다. 없으면 줄을 두지 않는다.
            member.email?.takeIf { it.isNotBlank() }?.let { email ->
                Text(
                    text = email,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 13.sp,
                    color = MagicColors.OnSurface55,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WithdrawRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .pressClickable(pressedScale = 0.99f, onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(MagicColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = MagicColors.Danger, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(Res.string.account_withdraw),
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MagicColors.Danger,
        )
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MagicColors.OnSurface40, modifier = Modifier.size(20.dp))
    }
}

/** Name of the provider in "~로 로그인됨". */
internal val LoginProvider.label
    get() = when (this) {
        LoginProvider.KAKAO -> Res.string.account_provider_kakao
        LoginProvider.GOOGLE -> Res.string.account_provider_google
        LoginProvider.APPLE -> Res.string.account_provider_apple
    }

@Preview(name = "계정 관리 · Google", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun AccountScreenPreview() {
    MagicPreview {
        AccountScreen(
            state = AccountState(
                Member(id = 1, nickname = "김민지", email = "minji.daily@gmail.com", profileImageUrl = null, providers = listOf(LoginProvider.GOOGLE)),
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "계정 관리 · 카카오", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun AccountScreenKakaoPreview() {
    MagicPreview {
        AccountScreen(
            state = AccountState(Member(id = 1, nickname = "민지", email = null, profileImageUrl = null, providers = listOf(LoginProvider.KAKAO))),
            onIntent = {},
        )
    }
}
