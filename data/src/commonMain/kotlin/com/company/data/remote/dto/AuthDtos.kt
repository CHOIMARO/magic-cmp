package com.company.data.remote.dto

import kotlinx.serialization.Serializable

// =====================================================================
// Request and response bodies of the server (live OpenAPI document of the server)
// =====================================================================

/**
 * Body of the Kakao and Google login requests.
 *
 * @property token Access token of Kakao, or ID token of Google.
 */
@Serializable
internal data class SnsLoginRequest(val token: String)

/**
 * Body of the Apple login request.
 *
 * @property token Identity token of Apple.
 * @property nonce Original nonce that the app made.
 * @property nickname Name that Apple gives only on the first sign-in.
 * @property authorizationCode Authorization code of Apple.
 */
@Serializable
internal data class AppleLoginRequest(
    val token: String,
    val nonce: String? = null,
    val nickname: String? = null,
    val authorizationCode: String? = null,
)

/**
 * Body of the refresh and logout requests.
 *
 * @property refreshToken Refresh token of the app.
 */
@Serializable
internal data class RefreshTokenRequest(val refreshToken: String)

/**
 * Tokens that the server gives.
 *
 * @property accessToken Access token. It is valid for 1 hour.
 * @property refreshToken Refresh token. The refresh also gives a new one, and the new one is valid for 30 days.
 */
@Serializable
internal data class TokenResponse(val accessToken: String, val refreshToken: String)

/**
 * Body of the profile change request.
 *
 * @property nickname New name, up to 20 characters.
 */
@Serializable
internal data class UpdateProfileRequest(val nickname: String)

/**
 * Account that the server gives.
 *
 * @property id ID of the account.
 * @property email Email, or null.
 * @property nickname Name of the account.
 * @property profileImageUrl Address of the profile picture, or null.
 * @property providers Login providers of the account, for example "KAKAO".
 * @property createdAt Time of the sign-up in Korea time.
 */
@Serializable
internal data class MemberResponse(
    val id: Long,
    val email: String? = null,
    val nickname: String,
    val profileImageUrl: String? = null,
    val providers: List<String> = emptyList(),
    val createdAt: String? = null,
)

/**
 * Error body of the server.
 *
 * @property code Error code, for example "INVALID_SNS_TOKEN".
 * @property message Korean sentence for the user.
 */
@Serializable
internal data class ErrorResponse(val code: String = "", val message: String = "")
