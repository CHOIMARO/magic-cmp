package com.company.data.remote

import com.company.domain.model.account.Member
import com.company.domain.model.account.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tokens of the server.
 *
 * @property accessToken Access token. It is valid for 1 hour.
 * @property refreshToken Refresh token. Each refresh gives a new one that is valid for 30 days. The server accepts
 *   the old one for 1 minute only (app-api-guide 2-3).
 */
@Serializable
internal data class AuthTokens(val accessToken: String, val refreshToken: String)

/** Secure storage of the platform: Android Keystore on Android, and Keychain on iOS. */
internal interface SecureStore {
    /**
     * Reads a value. Returns null when there is no value or the platform cannot read it.
     *
     * @param key Name of the value.
     */
    fun read(key: String): String?

    /**
     * Writes a value. A value with the same key is replaced. The value is on the disk when the call returns.
     *
     * @param key Name of the value.
     * @param value Value to keep.
     * @throws Exception when the platform cannot write the value.
     */
    fun write(key: String, value: String)

    /**
     * Removes a value.
     *
     * @param key Name of the value.
     */
    fun remove(key: String)
}

/**
 * Keeps the tokens and the sign-in state.
 *
 * The HTTP client changes the tokens when it gets new ones or when the server does not accept them. The auth
 * repository shows the same state to the app.
 *
 * @param secureStore Secure storage of the platform.
 */
internal class SessionStore(private val secureStore: SecureStore) {
    private val _tokens = MutableStateFlow(load())

    /** Current tokens, or null when the user is signed out. */
    val tokens: AuthTokens?
        get() = _tokens.value

    private val _state = MutableStateFlow<SessionState>(
        if (_tokens.value != null) SessionState.SignedIn(member = null) else SessionState.SignedOut,
    )

    /** Current sign-in state. */
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * Keeps the tokens of a new sign-in.
     *
     * @param tokens Tokens that the server gave.
     */
    fun signIn(tokens: AuthTokens) {
        save(tokens)
        _state.value = SessionState.SignedIn(member = null)
    }

    /**
     * Keeps both new tokens after a refresh. The server changes the refresh token at each refresh, so the old refresh
     * token is not valid any more. The sign-in state does not change.
     *
     * @param tokens Tokens that the server gave.
     * @return False when the user signed out during the refresh. The tokens then stay out.
     */
    fun replaceTokens(tokens: AuthTokens): Boolean {
        val current = _tokens.value ?: return false
        // 재발급하는 사이에 로그아웃했으면 토큰을 되살리지 않는다.
        if (!_tokens.compareAndSet(current, tokens)) return false
        persist(tokens)
        return true
    }

    /**
     * Keeps the account. Nothing changes when the user is signed out.
     *
     * @param member Account that the server gave.
     */
    fun updateMember(member: Member) {
        _state.update { if (it is SessionState.SignedIn) SessionState.SignedIn(member) else it }
    }

    /** Removes the tokens. The user is then signed out. */
    fun clear() {
        secureStore.remove(KEY_TOKENS)
        _tokens.value = null
        _state.value = SessionState.SignedOut
    }

    private fun save(tokens: AuthTokens) {
        _tokens.value = tokens
        persist(tokens)
    }

    /**
     * Writes the tokens to the secure storage. The memory has them first, so a failed write does not stop this
     * session. For example, the Keychain can be locked before the first unlock after a restart.
     */
    private fun persist(tokens: AuthTokens) {
        try {
            secureStore.write(KEY_TOKENS, Json.encodeToString(AuthTokens.serializer(), tokens))
        } catch (_: Exception) {
            // 다음 재발급에서 다시 저장한다. 앱을 끄면 로그인이 풀릴 수 있다.
        }
    }

    private fun load(): AuthTokens? {
        val text = secureStore.read(KEY_TOKENS) ?: return null
        return runCatching { Json.decodeFromString(AuthTokens.serializer(), text) }.getOrNull()
    }

    private companion object {
        const val KEY_TOKENS = "auth_tokens"
    }
}
