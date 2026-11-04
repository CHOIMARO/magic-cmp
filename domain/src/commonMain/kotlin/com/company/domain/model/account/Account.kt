package com.company.domain.model.account

// =====================================================================
// Accounts (F17, PRD 9.17)
//
// The user can edit and save without an account. The app asks the user to sign in only before it calls a server
// API (D67). The first sign-in makes the account. Works stay on the device; dubbings and my voice need the account.
// =====================================================================

/** Way that the user signs in (D69). Android offers Kakao and Google. iOS also offers Apple. */
enum class LoginProvider {
    /** KAKAO : Kakao login. */
    KAKAO,

    /** GOOGLE : Google login. */
    GOOGLE,

    /** APPLE : Sign in with Apple. Only iOS offers it. */
    APPLE,
}

/**
 * Account of the signed-in user.
 *
 * @property id ID of the account on the server.
 * @property nickname Name from the login provider. The server uses "magic 사용자" when the provider gives no name.
 * @property email Email from the login provider, or null. Kakao can give no email, and Apple can give a relay address.
 * @property profileImageUrl Address of the profile picture, or null.
 * @property providers Login providers that the account uses.
 */
data class Member(
    val id: Long,
    val nickname: String,
    val email: String?,
    val profileImageUrl: String?,
    val providers: List<LoginProvider>,
)

/** Sign-in state of the app. */
sealed interface SessionState {
    /** SignedOut : No account. The server features ask the user to sign in. */
    data object SignedOut : SessionState

    /**
     * SignedIn(member) : The app has tokens for an account.
     *
     * @property member Account, or null while the app has not loaded it yet.
     */
    data class SignedIn(val member: Member?) : SessionState
}

/** Proof of a sign-in that a login provider SDK gives. The server checks it and gives the app its own tokens. */
sealed interface SocialCredential {
    /** Provider of the credential. */
    val provider: LoginProvider

    /**
     * Kakao(accessToken) : Access token of the Kakao SDK.
     *
     * @property accessToken Access token of Kakao.
     */
    data class Kakao(val accessToken: String) : SocialCredential {
        override val provider = LoginProvider.KAKAO
    }

    /**
     * Google(idToken) : ID token of Google.
     *
     * @property idToken ID token of Google.
     */
    data class Google(val idToken: String) : SocialCredential {
        override val provider = LoginProvider.GOOGLE
    }

    /**
     * Apple(identityToken, nonce, authorizationCode, nickname) : Result of Sign in with Apple.
     *
     * @property identityToken Identity token of Apple.
     * @property nonce Original nonce that the app made. The request to Apple used its SHA-256 hash.
     * @property authorizationCode Authorization code. The server keeps the tokens of Apple to revoke them on withdrawal.
     * @property nickname Name that Apple gives only on the first sign-in, or null.
     */
    data class Apple(
        val identityToken: String,
        val nonce: String?,
        val authorizationCode: String?,
        val nickname: String?,
    ) : SocialCredential {
        override val provider = LoginProvider.APPLE
    }
}
