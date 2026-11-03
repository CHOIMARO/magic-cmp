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
 * @property refreshToken Refresh token. It is valid for 30 days from the sign-in.
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
     * Writes a value. A value with the same key is replaced.
     *
     * @param key Name of the value.
     * @param value Value to keep.
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
     * Keeps the tokens after a refresh. The sign-in state does not change.
     *
     * @param tokens Tokens that the server gave.
     */
    fun replaceTokens(tokens: AuthTokens) {
        if (_tokens.value == null) return
        save(tokens)
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
        secureStore.write(KEY_TOKENS, Json.encodeToString(AuthTokens.serializer(), tokens))
        _tokens.value = tokens
    }

    private fun load(): AuthTokens? {
        val text = secureStore.read(KEY_TOKENS) ?: return null
        return runCatching { Json.decodeFromString(AuthTokens.serializer(), text) }.getOrNull()
    }

    private companion object {
        const val KEY_TOKENS = "auth_tokens"
    }
}
