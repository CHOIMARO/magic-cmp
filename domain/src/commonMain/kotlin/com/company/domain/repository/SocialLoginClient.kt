package com.company.domain.repository

import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.SocialCredential

/**
 * Login provider SDKs of the platform: Kakao, Google, and on iOS also Apple.
 *
 * The platform code calls the SDKs. The common code only gets the credential and sends it to the server.
 */
interface SocialLoginClient {
    /** Providers that this platform offers, in the order of the login sheet: Kakao, Google, Apple (D69). */
    val providers: List<LoginProvider>

    /**
     * Shows the sign-in screen of a provider.
     *
     * @param provider Provider to use.
     * @return The credential of the provider.
     * @throws com.company.domain.model.account.SignInCancelledException when the user closes the provider screen.
     */
    suspend fun signIn(provider: LoginProvider): SocialCredential

    /** Ends the sessions of the provider SDKs on this device. Errors are ignored. */
    suspend fun signOut()
}
