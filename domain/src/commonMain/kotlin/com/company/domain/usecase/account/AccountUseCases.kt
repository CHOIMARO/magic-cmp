package com.company.domain.usecase.account

import com.company.domain.model.account.LoginProvider
import com.company.domain.repository.AuthRepository
import com.company.domain.repository.SocialLoginClient

/**
 * Signs in with a login provider (S14): the provider SDK gives a credential, and the server gives the app its tokens.
 * The first sign-in makes the account.
 */
class SignInUseCase(
    private val socialLoginClient: SocialLoginClient,
    private val authRepository: AuthRepository,
) {
    /**
     * @param provider Provider that the user tapped.
     * @throws com.company.domain.model.account.SignInCancelledException when the user closes the provider screen.
     * @throws com.company.domain.model.account.ServerException when the server does not accept the credential.
     * @throws com.company.domain.model.account.NetworkException when the app cannot reach the server.
     */
    suspend operator fun invoke(provider: LoginProvider) {
        val credential = socialLoginClient.signIn(provider)
        authRepository.signIn(credential)
    }
}

/**
 * Signs out. The works on the device and the dubbings that are in them stay (9.17).
 */
class SignOutUseCase(
    private val socialLoginClient: SocialLoginClient,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.signOut()
        socialLoginClient.signOut()
    }
}

/**
 * Deletes the account (S16). The server deletes the account data and disconnects the login providers. The works on
 * the device stay.
 */
class WithdrawUseCase(
    private val socialLoginClient: SocialLoginClient,
    private val authRepository: AuthRepository,
) {
    /**
     * @throws com.company.domain.model.account.ServerException when the server does not delete the account.
     * @throws com.company.domain.model.account.NetworkException when the app cannot reach the server.
     */
    suspend operator fun invoke() {
        authRepository.withdraw()
        socialLoginClient.signOut()
    }
}
