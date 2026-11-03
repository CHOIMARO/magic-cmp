package com.company.data.remote

import com.company.data.remote.dto.AppleLoginRequest
import com.company.data.remote.dto.MemberResponse
import com.company.data.remote.dto.RefreshTokenRequest
import com.company.data.remote.dto.SnsLoginRequest
import com.company.data.remote.dto.TokenResponse
import com.company.data.remote.dto.UpdateProfileRequest
import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.Member
import com.company.domain.model.account.ServerException
import com.company.domain.model.account.SessionState
import com.company.domain.model.account.SignInRequiredException
import com.company.domain.model.account.SocialCredential
import com.company.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Account and tokens from the API server (F17).
 *
 * @param client HTTP client of the API server.
 * @param sessionStore Keeps the tokens.
 */
internal class RemoteAuthRepository(
    private val client: HttpClient,
    private val sessionStore: SessionStore,
) : AuthRepository {
    override val session: StateFlow<SessionState> = sessionStore.state

    override suspend fun signIn(credential: SocialCredential) {
        val tokens: TokenResponse = when (credential) {
            is SocialCredential.Kakao -> login("kakao", SnsLoginRequest(credential.accessToken))
            is SocialCredential.Google -> login("google", SnsLoginRequest(credential.idToken))
            is SocialCredential.Apple -> login(
                "apple",
                AppleLoginRequest(
                    token = credential.identityToken,
                    nonce = credential.nonce,
                    nickname = credential.nickname,
                    authorizationCode = credential.authorizationCode,
                ),
            )
        }
        sessionStore.signIn(AuthTokens(tokens.accessToken, tokens.refreshToken))
        client.forgetCachedTokens()
        refreshMember()
    }

    override suspend fun signOut() {
        val refreshToken = sessionStore.tokens?.refreshToken
        // 서버에 닿지 않아도 이 기기에서는 로그아웃한다. 토큰 두 개를 모두 지운다(2-4).
        sessionStore.clear()
        client.forgetCachedTokens()
        if (refreshToken == null) return
        try {
            client.post("auth/logout") {
                withoutAuth()
                setBody(RefreshTokenRequest(refreshToken))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 서버의 refresh 토큰은 30일 뒤에 저절로 만료된다.
        }
    }

    override suspend fun withdraw() {
        authorized { client.delete("members/me") }
        sessionStore.clear()
        client.forgetCachedTokens()
    }

    override suspend fun refreshMember() {
        if (sessionStore.tokens == null) return
        try {
            val member = authorized { client.get("members/me").body<MemberResponse>() }
            sessionStore.updateMember(member.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 불러오지 못하면 지금 상태를 그대로 둔다. 토큰이 무효면 authorized가 이미 로그아웃했다.
        }
    }

    override suspend fun updateNickname(nickname: String): Member {
        val member = authorized {
            client.patch("members/me") { setBody(UpdateProfileRequest(nickname)) }.body<MemberResponse>()
        }.toDomain()
        sessionStore.updateMember(member)
        return member
    }

    private suspend inline fun <reified T : Any> login(provider: String, body: T): TokenResponse =
        client.post("auth/login/$provider") {
            withoutAuth()
            setBody(body)
        }.body()

    /**
     * Calls an API that needs the tokens. A 401 response after the refresh means that the tokens are not valid any
     * more, so the user is signed out.
     */
    private suspend fun <T> authorized(block: suspend () -> T): T {
        if (sessionStore.tokens == null) throw SignInRequiredException()
        return try {
            block()
        } catch (e: ServerException) {
            if (!e.unauthorized) throw e
            sessionStore.clear()
            client.forgetCachedTokens()
            throw SignInRequiredException()
        }
    }
}

/** Changes the account of the server to the account of the app. Unknown providers are left out. */
internal fun MemberResponse.toDomain(): Member = Member(
    id = id,
    nickname = nickname,
    email = email,
    profileImageUrl = profileImageUrl,
    providers = providers.mapNotNull { name -> LoginProvider.entries.firstOrNull { it.name == name } },
)
