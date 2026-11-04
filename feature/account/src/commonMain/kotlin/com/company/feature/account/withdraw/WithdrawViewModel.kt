package com.company.feature.account.withdraw

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.domain.model.editor.MyVoiceStatus
import com.company.domain.repository.MyVoiceRepository
import com.company.domain.usecase.account.WithdrawUseCase
import com.company.feature.account.resources.Res
import com.company.feature.account.resources.toast_withdraw_failed
import com.company.ui.text.UiText
import com.company.ui.text.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * State of the withdrawal screen (S16).
 *
 * @property hasMyVoice True when the user has an own voice. The list of the things that go away then shows it.
 * @property confirm True while the popup asks the user.
 * @property busy True while the server deletes the account. The screen then blocks the back action.
 */
data class WithdrawState(
    val hasMyVoice: Boolean = false,
    val confirm: Boolean = false,
    val busy: Boolean = false,
) : State()

/** User actions on the withdrawal screen. */
sealed class WithdrawIntent : Intent() {
    /** Back : Goes back to the account management screen. The screen ignores it while it is busy. */
    data object Back : WithdrawIntent()

    /** RequestWithdraw : Asks the user before the withdrawal. */
    data object RequestWithdraw : WithdrawIntent()

    /** ConfirmWithdraw : Deletes the account. */
    data object ConfirmWithdraw : WithdrawIntent()

    /** DismissWithdraw : Closes the popup. The account stays. */
    data object DismissWithdraw : WithdrawIntent()
}

/** One-time events of the withdrawal screen. */
sealed class WithdrawSideEffect : SideEffect() {
    /** Close : Leaves the screen. */
    data object Close : WithdrawSideEffect()

    /** Withdrawn : The account is gone. The app goes to the home tab and says thanks. */
    data object Withdrawn : WithdrawSideEffect()

    /** ShowToast(message) : Shows a short message. */
    data class ShowToast(val message: UiText) : WithdrawSideEffect()
}

/**
 * ViewModel of the withdrawal screen.
 *
 * @param withdraw Deletes the account on the server and signs out.
 * @param myVoiceRepository Gives and removes the own voice of the user.
 */
class WithdrawViewModel(
    private val withdraw: WithdrawUseCase,
    private val myVoiceRepository: MyVoiceRepository,
) : BaseViewModel<WithdrawState, WithdrawIntent, WithdrawSideEffect>(
    initialState = WithdrawState(),
) {

    init {
        viewModelScope.launch {
            myVoiceRepository.status.collect { reduce { copy(hasMyVoice = it == MyVoiceStatus.Ready) } }
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
    override fun handleIntent(intent: WithdrawIntent) {
        when (intent) {
            WithdrawIntent.Back -> if (!currentState.busy) postSideEffect { WithdrawSideEffect.Close }
            WithdrawIntent.RequestWithdraw -> if (!currentState.busy) reduce { copy(confirm = true) }
            WithdrawIntent.ConfirmWithdraw -> confirmWithdraw()
            WithdrawIntent.DismissWithdraw -> reduce { copy(confirm = false) }
        }
    }

    // =====================================================================
    // Withdrawal
    // =====================================================================

    /** Deletes the account. A failure keeps the screen and shows a toast. */
    private fun confirmWithdraw() {
        if (currentState.busy) return
        reduce { copy(confirm = false, busy = true) }
        viewModelScope.launch {
            try {
                withdraw()
                // 서버는 내 목소리도 지운다. 이 기기의 상태도 없앤다.
                myVoiceRepository.delete()
                postSideEffect { WithdrawSideEffect.Withdrawn }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                reduce { copy(busy = false) }
                postSideEffect { WithdrawSideEffect.ShowToast(uiText(Res.string.toast_withdraw_failed)) }
            }
        }
    }
}
