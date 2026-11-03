package com.company.feature.account.manage

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.domain.model.account.Member
import com.company.domain.model.account.SessionState
import com.company.domain.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * State of the account management screen (S15).
 *
 * @property member Signed-in account, or null while the app loads it.
 */
data class AccountState(val member: Member? = null) : State()

/** User actions on the account management screen. */
sealed class AccountIntent : Intent() {
    /** Back : Goes back to the "마이" tab. */
    data object Back : AccountIntent()

    /** OpenWithdraw : Opens the withdrawal screen (S16). */
    data object OpenWithdraw : AccountIntent()
}

/** One-time events of the account management screen. */
sealed class AccountSideEffect : SideEffect() {
    /** Close : Leaves the screen. */
    data object Close : AccountSideEffect()

    /** OpenWithdraw : Navigates to the withdrawal screen. */
    data object OpenWithdraw : AccountSideEffect()
}

/**
 * ViewModel of the account management screen. The screen closes when the user is no longer signed in.
 *
 * @param authRepository Gives the account.
 */
class AccountViewModel(authRepository: AuthRepository) : BaseViewModel<AccountState, AccountIntent, AccountSideEffect>(
    initialState = AccountState(),
) {

    init {
        viewModelScope.launch {
            authRepository.session.collect { session ->
                when (session) {
                    is SessionState.SignedIn -> reduce { copy(member = session.member) }
                    // 토큰이 만료되어 로그아웃되면 이 화면에 머물 이유가 없다.
                    SessionState.SignedOut -> postSideEffect { AccountSideEffect.Close }
                }
            }
        }
        viewModelScope.launch { if (authRepository.session.value is SessionState.SignedIn) authRepository.refreshMember() }
    }

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: AccountIntent) {
        when (intent) {
            AccountIntent.Back -> postSideEffect { AccountSideEffect.Close }
            AccountIntent.OpenWithdraw -> postSideEffect { AccountSideEffect.OpenWithdraw }
        }
    }
}
