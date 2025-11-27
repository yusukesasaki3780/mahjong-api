package com.example.presentation.dto

import com.example.usecase.auth.LoginUserUseCase
import kotlinx.serialization.Serializable

/**
 * 認証系で利用するリクエスト/レスポンス DTO 群。
 */
@Serializable
data class RegisterRequest(
    val name: String,
    val nickname: String,
    val storeName: String,
    val prefectureCode: String,
    val email: String,
    val password: String,
    val passwordConfirm: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val issuedAt: String,
    val user: UserResponse
) {
    companion object {
        fun from(result: LoginUserUseCase.Result) = LoginResponse(
            token = result.token,
            refreshToken = result.refreshToken,
            issuedAt = result.issuedAt.toString(),
            user = UserResponse.from(result.user)
        )
    }
}

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val userId: Long? = null
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
