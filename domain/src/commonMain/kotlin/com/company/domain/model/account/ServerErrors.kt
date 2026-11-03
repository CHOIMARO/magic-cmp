package com.company.domain.model.account

/**
 * Error that the server sent.
 *
 * Branch on [status] and [code]. Do not branch on [message]: it can change.
 *
 * @property status HTTP status.
 * @property code Error code of the server, for example "INVALID_SNS_TOKEN".
 * @property message Korean sentence for the user. A screen can show it as it is.
 */
class ServerException(val status: Int, val code: String, override val message: String) : Exception(message) {
    /** True when the server did not accept the tokens of the app. */
    val unauthorized: Boolean
        get() = status == UNAUTHORIZED

    private companion object {
        const val UNAUTHORIZED = 401
    }
}

/**
 * The app could not reach the server, for example because the device has no internet.
 *
 * @param cause Error of the network library.
 */
class NetworkException(cause: Throwable? = null) : Exception("The app could not reach the server", cause)

/** The user closed the screen of the login provider. The app shows no message and keeps the login sheet. */
class SignInCancelledException : Exception("The user cancelled the sign-in")

/** The account has no valid tokens any more. The user must sign in again. */
class SignInRequiredException : Exception("The user must sign in")
