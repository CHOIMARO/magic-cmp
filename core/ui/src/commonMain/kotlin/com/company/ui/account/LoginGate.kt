package com.company.ui.account

import com.company.domain.model.account.SessionState
import com.company.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/** Reason of a sign-in request. The login sheet shows a title for each reason (S14). */
enum class LoginReason {
    /** DUBBING : The user wants to make a dubbing. */
    DUBBING,

    /** CAPTION : The user wants to make captions on the server. */
    CAPTION,

    /** MY_VOICE : The user wants to make or use the own voice. */
    MY_VOICE,

    /** MY_TAB : The user tapped the login card or a row of the "마이" tab. */
    MY_TAB,
}

/**
 * Asks the user to sign in before a feature that calls the server (D67).
 *
 * The app root shows the login sheet while [request] is not null. The screen below the sheet stays as it is, so the
 * input of the user stays too. After the sign-in, the caller continues the action that the user tapped.
 *
 * @param authRepository Gives the sign-in state.
 */
class LoginGate(private val authRepository: AuthRepository) {
    private val _request = MutableStateFlow<LoginReason?>(null)

    /** Reason of the open login sheet, or null when the sheet is closed. */
    val request: StateFlow<LoginReason?> = _request.asStateFlow()

    /** True when the user is signed in. */
    val isSignedIn: Boolean
        get() = authRepository.session.value is SessionState.SignedIn

    /**
     * Returns true at once when the user is signed in. Otherwise opens the login sheet and waits until it closes.
     *
     * @param reason Reason that the sheet shows.
     * @return True when the user signed in, false when the user closed the sheet.
     */
    suspend fun ensureSignedIn(reason: LoginReason): Boolean {
        if (isSignedIn) return true
        _request.value = reason
        try {
            _request.first { it == null }
        } catch (e: CancellationException) {
            // 요청한 화면이 사라지면 시트도 닫는다.
            _request.compareAndSet(reason, null)
            throw e
        }
        return isSignedIn
    }

    /**
     * Opens the login sheet without an action after the sign-in, for example from the login card of the "마이" tab.
     *
     * @param reason Reason that the sheet shows.
     */
    fun open(reason: LoginReason) {
        if (!isSignedIn) _request.value = reason
    }

    /** Closes the login sheet. The callers that wait get the sign-in state. */
    fun close() {
        _request.value = null
    }
}
