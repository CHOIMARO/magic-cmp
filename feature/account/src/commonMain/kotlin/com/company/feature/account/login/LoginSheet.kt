package com.company.feature.account.login

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import com.company.core.designsystem.component.MagicModalBottomSheet
import com.company.core.designsystem.component.MagicSpinner
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.domain.model.account.LoginProvider
import com.company.feature.account.component.KakaoYellow
import com.company.feature.account.component.logo
import com.company.feature.account.resources.Res
import com.company.feature.account.resources.login_apple
import com.company.feature.account.resources.login_google
import com.company.feature.account.resources.login_kakao
import com.company.feature.account.resources.login_privacy
import com.company.feature.account.resources.login_terms
import com.company.feature.account.resources.login_terms_of_use
import com.company.feature.account.resources.login_title_caption
import com.company.feature.account.resources.login_title_dubbing
import com.company.feature.account.resources.login_title_my
import com.company.feature.account.resources.login_title_my_voice
import com.company.ui.account.LoginReason
import com.company.ui.text.resolve
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Time on screen of the failure toast. */
private const val FAILURE_TOAST_MILLIS = 2200L

/** Alpha of the other buttons while one provider signs in. */
private const val BUSY_OTHER_ALPHA = 0.4f

/**
 * Login sheet (S14). Put it one time at the app root, above the screens. It opens when a screen asks for a sign-in
 * with [com.company.ui.account.LoginGate].
 *
 * @param viewModel ViewModel of the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheetHost(viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalMagicToast.current
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is LoginSideEffect.ShowToast -> toast.show(effect.message.resolve(), FAILURE_TOAST_MILLIS)
            }
        }
    }

    // 로그인 중에는 끌어내리기, 바깥 누르기, 뒤로 가기로 닫지 않는다.
    val busy = state.busyProvider != null
    MagicModalBottomSheet(
        target = state.reason,
        onDismissRequest = { viewModel.postIntent(LoginIntent.Close) },
        showDragHandle = true,
        gestureDismissEnabled = !busy,
        backDismissEnabled = !busy,
    ) { reason, _ ->
        LoginSheetContent(
            reason = reason,
            providers = state.providers,
            busyProvider = state.busyProvider,
            onSignIn = { viewModel.postIntent(LoginIntent.SignIn(it)) },
        )
    }
}

/**
 * Content of the login sheet: the title for the reason, one button for each provider, and the terms.
 *
 * @param reason Reason of the sign-in.
 * @param providers Providers from top to bottom.
 * @param busyProvider Provider of the sign-in in progress, or null.
 * @param onSignIn Called with the tapped provider.
 */
@Composable
internal fun LoginSheetContent(
    reason: LoginReason,
    providers: List<LoginProvider>,
    busyProvider: LoginProvider?,
    onSignIn: (LoginProvider) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(reason.title),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 30.sp,
            letterSpacing = (-0.02).em,
        )
        providers.forEach { provider ->
            ProviderButton(
                provider = provider,
                busy = busyProvider == provider,
                dimmed = busyProvider != null && busyProvider != provider,
                onClick = { onSignIn(provider) },
            )
        }
        TermsText()
    }
}

@Composable
private fun ProviderButton(provider: LoginProvider, busy: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val (container, content) = when (provider) {
        LoginProvider.KAKAO -> KakaoYellow to Color.Black.copy(alpha = 0.85f)
        LoginProvider.GOOGLE -> Color.White to Color(0xFF1F1F1F)
        LoginProvider.APPLE -> Color.White to Color.Black
    }
    val alpha by animateFloatAsState(if (dimmed) BUSY_OTHER_ALPHA else 1f, tween(150))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(alpha)
            .pressClickable(enabled = !busy && !dimmed, onClick = onClick)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp).size(22.dp), contentAlignment = Alignment.Center) {
            Image(imageVector = provider.logo, contentDescription = null, modifier = Modifier.size(provider.logoSize))
        }
        if (busy) {
            MagicSpinner(color = content, trackColor = content.copy(alpha = 0.2f), size = 20.dp)
        } else {
            Text(text = stringResource(provider.label), color = content, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TermsText() {
    val terms = stringResource(Res.string.login_terms_of_use)
    val privacy = stringResource(Res.string.login_privacy)
    val full = stringResource(Res.string.login_terms, terms, privacy)
    val linkStyle = SpanStyle(color = MagicColors.OnSurface80, textDecoration = TextDecoration.Underline)
    val text = buildAnnotatedString {
        append(full)
        listOf(terms, privacy).forEach { link ->
            val start = full.indexOf(link)
            if (start >= 0) addStyle(linkStyle, start, start + link.length)
        }
    }
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp),
        color = MagicColors.OnSurface50,
        fontSize = 12.5.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
    )
}

private val LoginReason.title
    get() = when (this) {
        LoginReason.DUBBING -> Res.string.login_title_dubbing
        LoginReason.CAPTION -> Res.string.login_title_caption
        LoginReason.MY_VOICE -> Res.string.login_title_my_voice
        LoginReason.MY_TAB -> Res.string.login_title_my
    }

private val LoginProvider.label
    get() = when (this) {
        LoginProvider.KAKAO -> Res.string.login_kakao
        LoginProvider.GOOGLE -> Res.string.login_google
        LoginProvider.APPLE -> Res.string.login_apple
    }

/** Drawn size of the logo. The logos of the design have different sizes in the same 22 dp area. */
private val LoginProvider.logoSize
    get() = when (this) {
        LoginProvider.KAKAO -> 22.dp
        LoginProvider.GOOGLE -> 20.dp
        LoginProvider.APPLE -> 21.dp
    }

@Preview(name = "로그인 시트 · iOS", widthDp = 360)
@Composable
private fun LoginSheetPreview() {
    MagicPreview {
        Box(modifier = Modifier.background(MagicColors.Surface)) {
            LoginSheetContent(
                reason = LoginReason.DUBBING,
                providers = LoginProvider.entries,
                busyProvider = null,
                onSignIn = {},
            )
        }
    }
}

@Preview(name = "로그인 시트 · 로그인 중", widthDp = 360)
@Composable
private fun LoginSheetBusyPreview() {
    MagicPreview {
        Box(modifier = Modifier.background(MagicColors.Surface)) {
            LoginSheetContent(
                reason = LoginReason.MY_TAB,
                providers = listOf(LoginProvider.KAKAO, LoginProvider.GOOGLE),
                busyProvider = LoginProvider.KAKAO,
                onSignIn = {},
            )
        }
    }
}
