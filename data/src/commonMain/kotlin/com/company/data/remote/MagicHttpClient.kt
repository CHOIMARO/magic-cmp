package com.company.data.remote

import com.company.data.remote.dto.ErrorResponse
import com.company.data.remote.dto.RefreshTokenRequest
import com.company.data.remote.dto.TokenResponse
import com.company.domain.model.account.NetworkException
import com.company.domain.model.account.ServerException
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

/** JSON settings of the server. The server can add fields, so the app ignores unknown fields. */
internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Makes the HTTP client of the API server.
 *
 * The client adds the access token to the requests to the API server. On a 401 response, it gets a new access token
 * one time and sends the request again (app-api-guide 2-8). When the server does not accept the refresh token, it
 * signs the user out.
 *
 * @param sessionStore Keeps the tokens.
 * @param baseUrl Address of the API server.
 * @param engine Engine for tests. Null uses the engine of the platform.
 */
internal fun createMagicHttpClient(
    sessionStore: SessionStore,
    baseUrl: String = ApiConfig.BASE_URL,
    engine: HttpClientEngine? = null,
): HttpClient {
    val apiHost = Url(baseUrl).host
    val config: HttpClientConfig<*>.() -> Unit = {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }
        install(ContentNegotiation) { json(ApiJson) }
        install(Auth) {
            bearer {
                loadTokens { sessionStore.tokens?.let { BearerTokens(it.accessToken, it.refreshToken) } }
                refreshTokens { refresh(sessionStore) }
                // presigned URL(S3)에 토큰을 붙이면 S3가 거절한다(3-5). API 서버로 가는 요청에만 붙인다.
                sendWithoutRequest { request -> request.url.host == apiHost }
            }
        }
        HttpResponseValidator {
            validateResponse { response -> if (!response.status.isSuccess()) throw response.toServerException() }
            handleResponseExceptionWithRequest { cause, _ -> if (cause is IOException) throw NetworkException(cause) }
        }
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
        }
    }
    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}

/**
 * Marks a request to a path under `/auth/`. The client adds no token, and a 401 response does not start a refresh.
 * A failed login also gives 401, so a refresh there repeats without an end (app-api-guide 2-8, item 4).
 */
internal fun HttpRequestBuilder.withoutAuth() {
    attributes.put(AuthCircuitBreaker, Unit)
}

/** Removes the tokens that the client keeps in memory. Call this after each change of the account. */
internal fun HttpClient.forgetCachedTokens() {
    authProvider<BearerAuthProvider>()?.clearToken()
}

/**
 * Gets new tokens. The server changes both tokens at each refresh (refresh token rotation, app-api-guide 2-3).
 *
 * - The saved tokens are the source. When they are not the tokens of the failed request, an earlier refresh saved
 *   new tokens already, and the client uses them. A second refresh with the old refresh token is a reuse for the
 *   server after 1 minute, and the server then ends the sign-in.
 * - The refresh request and the save of both tokens run to their end, also when the caller stops waiting. Otherwise
 *   the server has the new refresh token and the app keeps the old one.
 * - A 401 signs the user out. Other errors go to the caller and keep the tokens, because the server accepts the same
 *   refresh token again for 1 minute.
 */
private suspend fun RefreshTokensParams.refresh(sessionStore: SessionStore): BearerTokens? {
    if (response.call.request.url.encodedPath.startsWith(AUTH_PATH)) return null
    val saved = sessionStore.tokens ?: return null
    if (saved.accessToken != oldTokens?.accessToken) return saved.toBearerTokens()
    // Ktor 3.3에는 nonCancellableRefresh가 없다. 같은 동작을 NonCancellable로 만든다(4-2, 2번).
    return withContext(NonCancellable) {
        try {
            val issued: TokenResponse = client.post("auth/refresh") {
                markAsRefreshTokenRequest()
                setBody(RefreshTokenRequest(saved.refreshToken))
            }.body()
            val rotated = AuthTokens(issued.accessToken, issued.refreshToken)
            // 원래 요청을 다시 보내기 전에 두 토큰을 먼저 저장한다(2-8, 2번).
            if (sessionStore.replaceTokens(rotated)) rotated.toBearerTokens() else null
        } catch (e: ServerException) {
            if (!e.unauthorized) throw e
            // 재발급이 401이면 저장한 토큰을 지우고 비로그인 상태로 바꾼다(2-8, 3번).
            sessionStore.clear()
            null
        }
    }
}

private fun AuthTokens.toBearerTokens() = BearerTokens(accessToken, refreshToken)

private suspend fun HttpResponse.toServerException(): ServerException {
    val error = runCatching { body<ErrorResponse>() }.getOrNull()
    return ServerException(
        status = status.value,
        code = error?.code.orEmpty(),
        message = error?.message?.takeIf { it.isNotBlank() } ?: status.description,
    )
}

private const val AUTH_PATH = "/auth/"
private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val CONNECT_TIMEOUT_MILLIS = 15_000L
