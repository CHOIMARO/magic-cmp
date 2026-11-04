package com.company.feature.account.login

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.SignInCancelledException
import com.company.domain.repository.SocialLoginClient
import com.company.domain.usecase.account.SignInUseCase
import com.company.feature.account.resources.Res
import com.company.feature.account.resources.toast_login_failed
import com.company.ui.account.LoginGate
import com.company.ui.account.LoginReason
import com.company.ui.text.UiText
import com.company.ui.text.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * State of the login sheet (S14).
 *
 * @property reason Reason of the open sheet. Null means the sheet is closed.
 * @property providers Login buttons from top to bottom: Kakao, Google, and on iOS Apple (D69).
 * @property busyProvider Provider of the sign-in in progress, or null. While it is set, the sheet ignores taps and
 *   does not close.
 */
data class LoginState(
    val reason: LoginReason? = null,
    val providers: List<LoginProvider> = emptyList(),
    val busyProvider: LoginProvider? = null,
) : State()

/** User actions on the login sheet. */
sealed class LoginIntent : Intent() {
    /** SignIn(provider) : Signs in with the provider. The sheet closes after the sign-in. */
    data class SignIn(val provider: LoginProvider) : LoginIntent()

    /** Close : The user closed the sheet with a swipe, a tap outside, or the back action. */
    data object Close : LoginIntent()
}

/** One-time events of the login sheet. */
sealed class LoginSideEffect : SideEffect() {
    /** ShowToast(message) : Shows a short message. */
    data class ShowToast(val message: UiText) : LoginSideEffect()
}

/**
 * ViewModel of the login sheet. The app root shows the sheet while [LoginGate.request] is not null.
 *
 * @param loginGate Opens and closes the sheet.
 * @param signIn Signs in with a provider.
 * @param socialLoginClient Gives the providers of the platform.
 */
class LoginViewModel(
    private val loginGate: LoginGate,
    private val signIn: SignInUseCase,
    socialLoginClient: SocialLoginClient,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(
    initialState = LoginState(providers = socialLoginClient.providers),
) {

    init {
        viewModelScope.launch {
            loginGate.request.collect { reason -> reduce { copy(reason = reason, busyProvider = null) } }
        }
    }

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.SignIn -> signIn(intent.provider)
            LoginIntent.Close -> if (currentState.busyProvider == null) loginGate.close()
        }
    }

    // =====================================================================
    // Sign-in
    // =====================================================================

    /** Signs in. A failure keeps the sheet open and shows a toast. A cancel by the user shows nothing. */
    private fun signIn(provider: LoginProvider) {
        if (currentState.reason == null || currentState.busyProvider != null) return
        reduce { copy(busyProvider = provider) }
        viewModelScope.launch {
            try {
                signIn.invoke(provider)
                // 시트를 닫으면 기다리던 화면이 누른 동작을 이어서 한다.
                loginGate.close()
            } catch (e: CancellationException) {
                throw e
            } catch (_: SignInCancelledException) {
                reduce { copy(busyProvider = null) }
            } catch (_: Exception) {
                reduce { copy(busyProvider = null) }
                postSideEffect { LoginSideEffect.ShowToast(uiText(Res.string.toast_login_failed)) }
            }
        }
    }
}
