package com.company.data.remote

import com.company.domain.model.account.Member
import com.company.domain.model.account.SessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tokens and sign-in state on the device (F17). */
class SessionStoreTest {
    private val secureStore = FakeSecureStore()
    private val member = Member(id = 1, nickname = "마로", email = null, profileImageUrl = null, providers = emptyList())

    @Test
    fun noTokens_startsSignedOut() {
        val store = SessionStore(secureStore)

        assertEquals(SessionState.SignedOut, store.state.value)
        assertNull(store.tokens)
    }

    @Test
    fun storedTokens_startSignedInAfterRestart() {
        SessionStore(secureStore).signIn(AuthTokens("a1", "r1"))

        // 앱을 다시 시작해도 로그인 상태를 이어 간다. 계정 정보는 서버에서 다시 불러온다.
        val restarted = SessionStore(secureStore)

        assertEquals(SessionState.SignedIn(member = null), restarted.state.value)
        assertEquals(AuthTokens("a1", "r1"), restarted.tokens)
    }

    @Test
    fun brokenValue_startsSignedOut() {
        SessionStore(secureStore).signIn(AuthTokens("a1", "r1"))
        secureStore.values.keys.forEach { secureStore.values[it] = "not json" }

        assertEquals(SessionState.SignedOut, SessionStore(secureStore).state.value)
    }

    @Test
    fun clear_removesTokens() {
        val store = SessionStore(secureStore)
        store.signIn(AuthTokens("a1", "r1"))

        store.clear()

        assertEquals(SessionState.SignedOut, store.state.value)
        assertNull(store.tokens)
        assertTrue(secureStore.values.isEmpty())
    }

    @Test
    fun replaceTokens_afterSignOut_keepsSignedOut() {
        // 재발급 응답이 로그아웃보다 늦게 와도 토큰을 되살리지 않는다.
        val store = SessionStore(secureStore)
        store.signIn(AuthTokens("a1", "r1"))
        store.clear()

        store.replaceTokens(AuthTokens("a2", "r1"))

        assertNull(store.tokens)
        assertTrue(secureStore.values.isEmpty())
    }

    @Test
    fun updateMember_whenSignedOut_keepsSignedOut() {
        val store = SessionStore(secureStore)

        store.updateMember(member)

        assertEquals(SessionState.SignedOut, store.state.value)
    }

    @Test
    fun updateMember_whenSignedIn_keepsMember() {
        val store = SessionStore(secureStore)
        store.signIn(AuthTokens("a1", "r1"))

        store.updateMember(member)

        assertEquals(SessionState.SignedIn(member), store.state.value)
    }
}
