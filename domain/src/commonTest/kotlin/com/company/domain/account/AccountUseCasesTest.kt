package com.company.domain.account

import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.Member
import com.company.domain.model.account.NetworkException
import com.company.domain.model.account.SessionState
import com.company.domain.model.account.SocialCredential
import com.company.domain.repository.AuthRepository
import com.company.domain.repository.SocialLoginClient
import com.company.domain.usecase.account.SignOutUseCase
import com.company.domain.usecase.account.WithdrawUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/** Sign-out and withdrawal with the login provider SDKs (F17, app-api-guide 2-7). */
class AccountUseCasesTest {
    private val calls = mutableListOf<String>()
    private val auth = FakeAuthRepository(calls)
    private val client = FakeSocialLoginClient(calls)

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return assertNotNull(result).getOrThrow()
    }

    @Test
    fun withdraw_deletesTheAccount_thenRemovesTheGoogleAccess() {
        // 서버는 구글 토큰을 취소하지 못한다. 탈퇴에 성공한 뒤 앱이 연결을 끊는다.
        runSuspend { WithdrawUseCase(client, auth)() }

        assertEquals(listOf("withdraw", "disconnect"), calls)
    }

    @Test
    fun failedWithdraw_keepsTheProviderSessions() {
        auth.withdrawFailure = NetworkException()

        assertFailsWith<NetworkException> { runSuspend { WithdrawUseCase(client, auth)() } }

        assertEquals(listOf("withdraw"), calls)
    }

    @Test
    fun signOut_onlyEndsTheSessions() {
        // 로그아웃은 연결을 끊지 않는다. 다음 로그인에서 같은 계정을 다시 고를 수 있다.
        runSuspend { SignOutUseCase(client, auth)() }

        assertEquals(listOf("signOut", "client.signOut"), calls)
    }

    private class FakeAuthRepository(private val calls: MutableList<String>) : AuthRepository {
        var withdrawFailure: Exception? = null
        override val session = MutableStateFlow<SessionState>(SessionState.SignedIn(member = null))
        override suspend fun signIn(credential: SocialCredential) = Unit

        override suspend fun signOut() {
            calls += "signOut"
        }

        override suspend fun withdraw() {
            calls += "withdraw"
            withdrawFailure?.let { throw it }
        }

        override suspend fun refreshMember() = Unit
        override suspend fun updateNickname(nickname: String): Member = error("Not used")
    }

    private class FakeSocialLoginClient(private val calls: MutableList<String>) : SocialLoginClient {
        override val providers = listOf(LoginProvider.KAKAO, LoginProvider.GOOGLE)
        override suspend fun signIn(provider: LoginProvider): SocialCredential = error("Not used")

        override suspend fun signOut() {
            calls += "client.signOut"
        }

        override suspend fun disconnect() {
            calls += "disconnect"
        }
    }
}
