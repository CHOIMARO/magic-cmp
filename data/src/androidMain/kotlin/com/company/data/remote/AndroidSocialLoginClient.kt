package com.company.data.remote

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.SignInCancelledException
import com.company.domain.model.account.SocialCredential
import com.company.domain.repository.SocialLoginClient
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Login SDKs of Android: Kakao and Google (Credential Manager). Android does not offer Apple (D69).
 *
 * The keys come from local.properties: `KAKAO_NATIVE_APP_KEY` and `GOOGLE_WEB_CLIENT_ID`.
 *
 * @param context Context of the app.
 * @param activities Gives the activity on screen.
 */
internal class AndroidSocialLoginClient(
    private val context: Context,
    private val activities: CurrentActivityTracker,
) : SocialLoginClient {
    override val providers: List<LoginProvider> = listOf(LoginProvider.KAKAO, LoginProvider.GOOGLE)

    private val kakaoReady: Boolean by lazy {
        if (ApiConfig.KAKAO_NATIVE_APP_KEY.isEmpty()) return@lazy false
        KakaoSdk.init(context.applicationContext, ApiConfig.KAKAO_NATIVE_APP_KEY)
        true
    }

    override suspend fun signIn(provider: LoginProvider): SocialCredential = when (provider) {
        LoginProvider.KAKAO -> SocialCredential.Kakao(accessToken = kakaoAccessToken())
        LoginProvider.GOOGLE -> SocialCredential.Google(idToken = googleIdToken())
        LoginProvider.APPLE -> throw UnsupportedOperationException("Android does not offer Sign in with Apple")
    }

    override suspend fun signOut() {
        try {
            if (kakaoReady) suspendCancellableCoroutine { cont -> UserApiClient.instance.logout { cont.resume(Unit) } }
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 기기의 SDK 세션을 지우지 못해도 앱의 로그아웃은 이미 끝났다.
        }
    }

    // =====================================================================
    // Kakao
    // =====================================================================

    /** Signs in with KakaoTalk when it is on the device, otherwise with a Kakao account on the web. */
    private suspend fun kakaoAccessToken(): String {
        check(kakaoReady) { "KAKAO_NATIVE_APP_KEY is not in local.properties" }
        val activity = requireActivity()
        val client = UserApiClient.instance
        if (client.isKakaoTalkLoginAvailable(activity)) {
            try {
                return kakaoLogin { callback -> client.loginWithKakaoTalk(activity, callback = callback) }
            } catch (e: SignInCancelledException) {
                // 카카오톡에서 사용자가 취소하면 카카오계정 로그인으로 넘어가지 않는다.
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 카카오톡에 연결된 계정이 없는 경우 등은 카카오계정으로 로그인한다.
            }
        }
        return kakaoLogin { callback -> client.loginWithKakaoAccount(activity, callback = callback) }
    }

    private suspend fun kakaoLogin(start: ((OAuthToken?, Throwable?) -> Unit) -> Unit): String =
        suspendCancellableCoroutine { cont ->
            start { token, error ->
                when {
                    token != null -> cont.resume(token.accessToken)
                    error is ClientError && error.reason == ClientErrorCause.Cancelled ->
                        cont.resumeWithException(SignInCancelledException())
                    else -> cont.resumeWithException(error ?: IllegalStateException("Kakao gave no token"))
                }
            }
        }

    // =====================================================================
    // Google
    // =====================================================================

    /** Shows the "Sign in with Google" screen and returns the ID token for the server client. */
    private suspend fun googleIdToken(): String {
        check(ApiConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()) { "GOOGLE_WEB_CLIENT_ID is not in local.properties" }
        val activity = requireActivity()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(ApiConfig.GOOGLE_WEB_CLIENT_ID).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (_: GetCredentialCancellationException) {
            throw SignInCancelledException()
        }
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type: ${credential.type}"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun requireActivity(): Activity = checkNotNull(activities.activity) { "The app has no activity on screen" }
}
