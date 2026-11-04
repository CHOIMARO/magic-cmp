package com.company.ui.account

import com.company.domain.model.account.Member
import com.company.domain.model.account.SessionState
import com.company.domain.model.account.SocialCredential
import com.company.domain.repository.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sign-in requests of the screens that call the server (D67). */
class LoginGateTest {
    private val auth = FakeAuthRepository()
    private val gate = LoginGate(auth)

    @Test
    fun signedIn_continuesAtOnce() = runTest {
        auth.session.value = SessionState.SignedIn(member = null)

        assertTrue(gate.ensureSignedIn(LoginReason.DUBBING))
        assertNull(gate.request.value)
    }

    @Test
    fun signedOut_opensSheet_thenContinuesAfterSignIn() = runTest {
        val result = async { gate.ensureSignedIn(LoginReason.DUBBING) }
        runCurrent()
        assertEquals(LoginReason.DUBBING, gate.request.value)

        // 로그인 시트가 로그인한 뒤 시트를 닫는다.
        auth.session.value = SessionState.SignedIn(member = null)
        gate.close()

        assertTrue(result.await())
    }

    @Test
    fun closedSheet_doesNotContinue() = runTest {
        val result = async { gate.ensureSignedIn(LoginReason.MY_VOICE) }
        runCurrent()

        gate.close()

        assertFalse(result.await())
    }

    @Test
    fun cancelledCaller_closesSheet() = runTest {
        val job = launch { gate.ensureSignedIn(LoginReason.CAPTION) }
        runCurrent()

        job.cancel()
        runCurrent()

        assertNull(gate.request.value)
    }

    @Test
    fun open_whenSignedIn_doesNothing() {
        auth.session.value = SessionState.SignedIn(member = null)

        gate.open(LoginReason.MY_TAB)

        assertNull(gate.request.value)
    }

    private class FakeAuthRepository : AuthRepository {
        override val session = MutableStateFlow<SessionState>(SessionState.SignedOut)
        override suspend fun signIn(credential: SocialCredential) = Unit
        override suspend fun signOut() = Unit
        override suspend fun withdraw() = Unit
        override suspend fun refreshMember() = Unit
        override suspend fun updateNickname(nickname: String): Member = error("Not used")
    }
}
