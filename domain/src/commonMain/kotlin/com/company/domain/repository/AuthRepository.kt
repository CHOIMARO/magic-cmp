package com.company.domain.repository

import com.company.domain.model.account.Member
import com.company.domain.model.account.SessionState
import com.company.domain.model.account.SocialCredential
import kotlinx.coroutines.flow.StateFlow

/**
 * Account and tokens of the app (F17).
 *
 * The tokens stay until sign-out or withdrawal. When the server does not accept the tokens and the app cannot get new
 * ones, the session becomes signed out. The next server feature then asks the user to sign in.
 */
interface AuthRepository {
    /** Current sign-in state. */
    val session: StateFlow<SessionState>

    /**
     * Signs in with the credential of a login provider. The first sign-in makes the account.
     *
     * @param credential Credential that the provider SDK gave.
     * @throws com.company.domain.model.account.ServerException when the server does not accept the credential.
     * @throws com.company.domain.model.account.NetworkException when the app cannot reach the server.
     */
    suspend fun signIn(credential: SocialCredential)

    /** Signs out. The app removes its tokens even when the server cannot be reached. */
    suspend fun signOut()

    /**
     * Deletes the account on the server, then removes the tokens of the app (S16).
     *
     * @throws com.company.domain.model.account.ServerException when the server does not delete the account.
     * @throws com.company.domain.model.account.NetworkException when the app cannot reach the server.
     */
    suspend fun withdraw()

    /** Loads the account again from the server. A failure keeps the current state. */
    suspend fun refreshMember()

    /**
     * Changes the name of the account.
     *
     * @param nickname New name, up to 20 characters.
     * @return The changed account.
     */
    suspend fun updateNickname(nickname: String): Member
}
