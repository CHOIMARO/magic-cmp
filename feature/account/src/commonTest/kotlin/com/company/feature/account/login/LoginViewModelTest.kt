package com.company.feature.account.login

import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.Member
import com.company.domain.model.account.NetworkException
import com.company.domain.model.account.SessionState
import com.company.domain.model.account.SignInCancelledException
import com.company.domain.model.account.SocialCredential
import com.company.domain.repository.AuthRepository
import com.company.domain.repository.SocialLoginClient
import com.company.domain.usecase.account.SignInUseCase
import com.company.ui.account.LoginGate
import com.company.ui.account.LoginReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Login sheet (S14): sign-in, failure, and cancel. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val auth = FakeAuthRepository()
    private val client = FakeSocialLoginClient()
    private val gate = LoginGate(auth)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = LoginViewModel(gate, SignInUseCase(client, auth), client)

    @Test
    fun signIn_closesSheet_andCallerContinues() = runTest {
        val viewModel = viewModel()
        val waiting = async { gate.ensureSignedIn(LoginReason.DUBBING) }
        runCurrent()
        assertEquals(LoginReason.DUBBING, viewModel.uiState.value.reason)

        viewModel.handleIntent(LoginIntent.SignIn(LoginProvider.KAKAO))

        assertTrue(waiting.await())
        assertEquals(SocialCredential.Kakao("kakao-token"), auth.lastCredential)
        assertNull(viewModel.uiState.value.reason)
    }

    @Test
    fun failure_keepsSheet_andShowsToast() = runTest {
        auth.failure = NetworkException()
        val viewModel = viewModel()
        gate.open(LoginReason.MY_TAB)

        viewModel.handleIntent(LoginIntent.SignIn(LoginProvider.GOOGLE))

        assertIs<LoginSideEffect.ShowToast>(viewModel.sideEffect.first())
        assertEquals(LoginReason.MY_TAB, viewModel.uiState.value.reason)
        assertNull(viewModel.uiState.value.busyProvider)
    }

    @Test
    fun cancel_keepsSheet_withoutToast() = runTest {
        client.cancel = true
        val viewModel = viewModel()
        gate.open(LoginReason.MY_TAB)

        viewModel.handleIntent(LoginIntent.SignIn(LoginProvider.KAKAO))

        assertEquals(LoginReason.MY_TAB, viewModel.uiState.value.reason)
        assertNull(viewModel.uiState.value.busyProvider)
        assertNull(auth.lastCredential)
    }

    @Test
    fun busy_ignoresClose_andOtherTaps() = runTest {
        client.pending = CompletableDeferred()
        val viewModel = viewModel()
        gate.open(LoginReason.MY_TAB)

        viewModel.handleIntent(LoginIntent.SignIn(LoginProvider.KAKAO))
        viewModel.handleIntent(LoginIntent.SignIn(LoginProvider.GOOGLE))
        viewModel.handleIntent(LoginIntent.Close)

        // 로그인 중에는 시트를 닫지 않고, 다른 버튼도 받지 않는다.
        assertEquals(LoginProvider.KAKAO, viewModel.uiState.value.busyProvider)
        assertEquals(LoginReason.MY_TAB, gate.request.value)
        assertEquals(listOf(LoginProvider.KAKAO), client.requests)
        client.pending?.complete(Unit)
    }

    private class FakeSocialLoginClient : SocialLoginClient {
        var cancel = false
        var pending: CompletableDeferred<Unit>? = null
        val requests = mutableListOf<LoginProvider>()
        override val providers = listOf(LoginProvider.KAKAO, LoginProvider.GOOGLE)

        override suspend fun signIn(provider: LoginProvider): SocialCredential {
            requests += provider
            pending?.await()
            if (cancel) throw SignInCancelledException()
            return when (provider) {
                LoginProvider.KAKAO -> SocialCredential.Kakao("kakao-token")
                else -> SocialCredential.Google("google-token")
            }
        }

        override suspend fun signOut() = Unit
        override suspend fun disconnect() = Unit
    }

    private class FakeAuthRepository : AuthRepository {
        var failure: Exception? = null
        var lastCredential: SocialCredential? = null
        override val session = MutableStateFlow<SessionState>(SessionState.SignedOut)

        override suspend fun signIn(credential: SocialCredential) {
            failure?.let { throw it }
            lastCredential = credential
            session.value = SessionState.SignedIn(member = null)
        }

        override suspend fun signOut() = Unit
        override suspend fun withdraw() = Unit
        override suspend fun refreshMember() = Unit
        override suspend fun updateNickname(nickname: String): Member = error("Not used")
    }
}
