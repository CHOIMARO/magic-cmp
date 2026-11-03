package com.company.data.remote

import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.Member
import com.company.domain.model.account.NetworkException
import com.company.domain.model.account.ServerException
import com.company.domain.model.account.SessionState
import com.company.domain.model.account.SignInRequiredException
import com.company.domain.model.account.SocialCredential
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sign-in, token refresh, sign-out, and withdrawal with the API server (F17, app-api-guide 2-6). */
class RemoteAuthRepositoryTest {
    private val secureStore = FakeSecureStore()
    private val sessionStore = SessionStore(secureStore)
    private val requests = mutableListOf<HttpRequestData>()
    private var server: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { error("No response for ${it.method.value} ${it.url.encodedPath}") }

    private val client: HttpClient = createMagicHttpClient(
        sessionStore = sessionStore,
        baseUrl = BASE_URL,
        engine = MockEngine { request ->
            requests += request
            server(request)
        },
    )
    private val repository = RemoteAuthRepository(client, sessionStore)

    private val member = Member(id = 1, nickname = "마로", email = null, profileImageUrl = null, providers = listOf(LoginProvider.KAKAO))

    /** Runs a test on a real dispatcher. With virtual time, HttpTimeout ends the requests at once. */
    private fun runApiTest(block: suspend () -> Unit) = runTest { withContext(Dispatchers.Default) { block() } }

    private fun signedIn(accessToken: String = "a1") = sessionStore.signIn(AuthTokens(accessToken, "r1"))

    private val HttpRequestData.route get() = "${method.value} ${url.encodedPath}"
    private val HttpRequestData.authorization get() = headers[HttpHeaders.Authorization]
    private val HttpRequestData.text get() = (body as? TextContent)?.text

    @Test
    fun signIn_sendsProviderToken_thenLoadsAccount() = runApiTest {
        server = { request ->
            when (request.route) {
                "POST /auth/login/kakao" -> json(TOKENS_A1)
                "GET /members/me" -> json(MEMBER)
                else -> error(request.route)
            }
        }

        repository.signIn(SocialCredential.Kakao(accessToken = "kakao-token"))

        assertEquals(SessionState.SignedIn(member), repository.session.value)
        assertEquals(AuthTokens("a1", "r1"), SessionStore(secureStore).tokens)
        assertEquals("""{"token":"kakao-token"}""", requests[0].text)
        assertNull(requests[0].authorization)
        assertEquals("Bearer a1", requests[1].authorization)
    }

    @Test
    fun signIn_apple_sendsNonceAndName() = runApiTest {
        server = { request -> if (request.route == "POST /auth/login/apple") json(TOKENS_A1) else json(MEMBER) }

        repository.signIn(SocialCredential.Apple("id-token", nonce = "n1", authorizationCode = "c1", nickname = null))

        assertEquals("""{"token":"id-token","nonce":"n1","authorizationCode":"c1"}""", requests[0].text)
    }

    @Test
    fun failedLogin_doesNotRefresh() = runApiTest {
        // 로그인 실패도 401이다. 재발급하면 끝없이 반복하므로 /auth/ 요청은 재발급하지 않는다(2-6, 4번).
        signedIn()
        server = { json(INVALID_SNS_TOKEN, HttpStatusCode.Unauthorized) }

        val error = assertFailsWith<ServerException> { repository.signIn(SocialCredential.Google(idToken = "bad")) }

        assertEquals(401, error.status)
        assertEquals("INVALID_SNS_TOKEN", error.code)
        assertEquals("유효하지 않은 SNS 토큰입니다", error.message)
        assertEquals(listOf("POST /auth/login/google"), requests.map { it.route })
        assertNull(requests[0].authorization)
    }

    @Test
    fun expiredAccessToken_refreshesOnce_thenSendsAgain() = runApiTest {
        signedIn(accessToken = "old")
        server = { request ->
            when {
                request.route == "POST /auth/refresh" -> json(TOKENS_A2)
                request.authorization == "Bearer a2" -> json(MEMBER)
                else -> json(UNAUTHORIZED, HttpStatusCode.Unauthorized)
            }
        }

        repository.refreshMember()

        assertEquals(SessionState.SignedIn(member), repository.session.value)
        assertEquals(listOf("GET /members/me", "POST /auth/refresh", "GET /members/me"), requests.map { it.route })
        assertEquals("""{"refreshToken":"r1"}""", requests[1].text)
        assertNull(requests[1].authorization)
        assertEquals(AuthTokens("a2", "r1"), SessionStore(secureStore).tokens)
    }

    @Test
    fun rejectedRefreshToken_signsOut() = runApiTest {
        signedIn()
        server = { request ->
            if (request.route == "POST /auth/refresh") {
                json(INVALID_REFRESH_TOKEN, HttpStatusCode.Unauthorized)
            } else {
                json(UNAUTHORIZED, HttpStatusCode.Unauthorized)
            }
        }

        assertFailsWith<SignInRequiredException> { repository.updateNickname("새 이름") }

        assertEquals(SessionState.SignedOut, repository.session.value)
        assertTrue(secureStore.values.isEmpty())
        assertEquals(listOf("PATCH /members/me", "POST /auth/refresh"), requests.map { it.route })
    }

    @Test
    fun refreshServerError_keepsAccount() = runApiTest {
        // 서버가 잠깐 실패해도 로그아웃하지 않는다. 재발급이 401일 때만 로그아웃한다.
        signedIn()
        server = { request ->
            if (request.route == "POST /auth/refresh") {
                json(INTERNAL_ERROR, HttpStatusCode.InternalServerError)
            } else {
                json(UNAUTHORIZED, HttpStatusCode.Unauthorized)
            }
        }

        val error = assertFailsWith<ServerException> { repository.updateNickname("새 이름") }

        assertEquals(500, error.status)
        assertIs<SessionState.SignedIn>(repository.session.value)
    }

    @Test
    fun signedOut_serverFeatureNeedsSignIn() = runApiTest {
        assertFailsWith<SignInRequiredException> { repository.withdraw() }

        assertTrue(requests.isEmpty())
    }

    @Test
    fun updateNickname_keepsNewAccount() = runApiTest {
        signedIn()
        server = { json(MEMBER.replace("\"마로\"", "\"새 이름\"")) }

        val changed = repository.updateNickname("새 이름")

        assertEquals("새 이름", changed.nickname)
        assertEquals(SessionState.SignedIn(changed), repository.session.value)
        assertEquals("""{"nickname":"새 이름"}""", requests[0].text)
    }

    @Test
    fun signOut_removesTokens_evenWhenServerFails() = runApiTest {
        signedIn()
        server = { json(INTERNAL_ERROR, HttpStatusCode.InternalServerError) }

        repository.signOut()

        assertEquals(SessionState.SignedOut, repository.session.value)
        assertTrue(secureStore.values.isEmpty())
        assertEquals("POST /auth/logout", requests[0].route)
        assertEquals("""{"refreshToken":"r1"}""", requests[0].text)
        assertNull(requests[0].authorization)
    }

    @Test
    fun signOut_thenServerFeature_sendsNoOldToken() = runApiTest {
        signedIn()
        server = { request -> if (request.route == "POST /auth/logout") respond("", HttpStatusCode.NoContent) else json(MEMBER) }
        repository.refreshMember()

        repository.signOut()
        client.get("dubbing-options")

        assertNull(requests.last().authorization)
    }

    @Test
    fun withdraw_deletesAccount_thenSignsOut() = runApiTest {
        signedIn()
        server = { respond("", HttpStatusCode.NoContent) }

        repository.withdraw()

        assertEquals("DELETE /members/me", requests[0].route)
        assertEquals("Bearer a1", requests[0].authorization)
        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun failedWithdraw_keepsAccount() = runApiTest {
        signedIn()
        server = { json(INTERNAL_ERROR, HttpStatusCode.InternalServerError) }

        assertFailsWith<ServerException> { repository.withdraw() }

        assertIs<SessionState.SignedIn>(repository.session.value)
    }

    @Test
    fun otherHosts_getNoToken() = runApiTest {
        // presigned URL(S3)에 토큰을 붙이면 S3가 거절한다(3-5).
        signedIn()
        server = { respond("mp3") }

        client.get("https://bucket.s3.test/dubbing/1.mp3")

        assertNull(requests[0].authorization)
    }

    @Test
    fun noNetwork_becomesNetworkException() = runApiTest {
        server = { throw IOException("offline") }

        assertFailsWith<NetworkException> { repository.signIn(SocialCredential.Kakao(accessToken = "kakao-token")) }

        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private companion object {
        const val BASE_URL = "http://api.test"
        const val TOKENS_A1 = """{"accessToken":"a1","refreshToken":"r1"}"""
        const val TOKENS_A2 = """{"accessToken":"a2","refreshToken":"r1"}"""
        const val MEMBER =
            """{"id":1,"email":null,"nickname":"마로","profileImageUrl":null,"providers":["KAKAO"],"createdAt":"2026-11-02T15:40:43.105877"}"""
        const val UNAUTHORIZED = """{"code":"UNAUTHORIZED","message":"인증이 필요합니다","fieldErrors":[]}"""
        const val INVALID_REFRESH_TOKEN = """{"code":"INVALID_REFRESH_TOKEN","message":"다시 로그인해 주세요","fieldErrors":[]}"""
        const val INVALID_SNS_TOKEN = """{"code":"INVALID_SNS_TOKEN","message":"유효하지 않은 SNS 토큰입니다","fieldErrors":[]}"""
        const val INTERNAL_ERROR = """{"code":"INTERNAL_ERROR","message":"서버 오류","fieldErrors":[]}"""
    }
}
