package com.company.magic.login

import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.SignInCancelledException
import com.company.domain.model.account.SocialCredential
import com.company.domain.repository.SocialLoginClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Result of a login SDK. The Swift code of the app makes it with [success], [cancelled], or [failure].
 *
 * @property token Access token of Kakao, ID token of Google, or identity token of Apple. Null when the sign-in failed.
 * @property nonce Original nonce of the Apple request, or null.
 * @property authorizationCode Authorization code of Apple, or null.
 * @property nickname Name that Apple gives only on the first sign-in, or null.
 * @property isCancelled True when the user closed the screen of the provider.
 * @property errorMessage Reason of the failure, or null.
 */
class IosLoginResult private constructor(
    val token: String?,
    val nonce: String?,
    val authorizationCode: String?,
    val nickname: String?,
    val isCancelled: Boolean,
    val errorMessage: String?,
) {
    companion object {
        /**
         * Makes the result of a sign-in.
         *
         * @param token Token of the provider.
         * @param nonce Original nonce of the Apple request, or null.
         * @param authorizationCode Authorization code of Apple, or null.
         * @param nickname Name from Apple, or null.
         */
        fun success(token: String, nonce: String?, authorizationCode: String?, nickname: String?): IosLoginResult =
            IosLoginResult(token, nonce, authorizationCode, nickname, isCancelled = false, errorMessage = null)

        /** Makes the result when the user closed the screen of the provider. */
        fun cancelled(): IosLoginResult = IosLoginResult(null, null, null, null, isCancelled = true, errorMessage = null)

        /**
         * Makes the result of a failed sign-in.
         *
         * @param message Reason of the failure.
         */
        fun failure(message: String): IosLoginResult = IosLoginResult(null, null, null, null, isCancelled = false, errorMessage = message)
    }
}

/**
 * Login SDKs that the Swift code of the app calls: Kakao, Google, and Apple.
 *
 * The SDKs of Kakao and Google are Swift packages of the Xcode project. Swift calls each completion one time.
 */
interface IosLoginBridge {
    /**
     * Signs in with KakaoTalk, or with a Kakao account on the web.
     *
     * @param completion Gets the access token of Kakao.
     */
    fun signInWithKakao(completion: (IosLoginResult) -> Unit)

    /**
     * Signs in with Google.
     *
     * @param completion Gets the ID token of Google.
     */
    fun signInWithGoogle(completion: (IosLoginResult) -> Unit)

    /**
     * Signs in with Apple. The request uses the SHA-256 hash of the nonce. The result has the original nonce.
     *
     * @param completion Gets the identity token, the nonce, the authorization code, and the name.
     */
    fun signInWithApple(completion: (IosLoginResult) -> Unit)

    /** Ends the sessions of the Kakao and Google SDKs on this device. */
    fun signOut()

    /**
     * Ends the sessions after a withdrawal. Google revokes the access of the app (`GIDSignIn.disconnect`), because the
     * server cannot revoke it (app-api-guide 2-7). The server disconnects Kakao and Apple.
     */
    fun disconnect()
}

/**
 * [SocialLoginClient] of iOS. It calls the Swift code through [IosLoginBridge].
 *
 * @param bridge Login SDKs of the Swift code.
 */
internal class IosSocialLoginClient(private val bridge: IosLoginBridge) : SocialLoginClient {
    // App Store는 다른 SNS 로그인을 주는 앱에 Apple 로그인도 요구한다(D69).
    override val providers: List<LoginProvider> = listOf(LoginProvider.KAKAO, LoginProvider.GOOGLE, LoginProvider.APPLE)

    override suspend fun signIn(provider: LoginProvider): SocialCredential {
        val result = suspendCancellableCoroutine { cont ->
            val completion: (IosLoginResult) -> Unit = { if (cont.isActive) cont.resume(it) }
            when (provider) {
                LoginProvider.KAKAO -> bridge.signInWithKakao(completion)
                LoginProvider.GOOGLE -> bridge.signInWithGoogle(completion)
                LoginProvider.APPLE -> bridge.signInWithApple(completion)
            }
        }
        if (result.isCancelled) throw SignInCancelledException()
        val token = result.token ?: throw IllegalStateException(result.errorMessage ?: "The provider gave no token")
        return when (provider) {
            LoginProvider.KAKAO -> SocialCredential.Kakao(accessToken = token)
            LoginProvider.GOOGLE -> SocialCredential.Google(idToken = token)
            LoginProvider.APPLE -> SocialCredential.Apple(
                identityToken = token,
                nonce = result.nonce,
                authorizationCode = result.authorizationCode,
                nickname = result.nickname,
            )
        }
    }

    override suspend fun signOut() {
        bridge.signOut()
    }

    override suspend fun disconnect() {
        bridge.disconnect()
    }
}
