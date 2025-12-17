package com.example.usecase.auth

/**
 * ### このファイルの役割
 * - 保存済みのリフレッシュトークンを検証し、新しいアクセストークンを払い出す処理をまとめたユースケースです。
 * - 攻撃対策としてリフレッシュトークンはハッシュで照合し、期限切れや存在しない場合は例外を投げます。
 */

import com.example.config.JwtProvider
import com.example.domain.repository.RefreshTokenRepository
import com.example.domain.repository.UserRepository
import kotlinx.datetime.Clock
import com.example.usecase.auth.plusDays

/**
 * Refresh Token を用いて Access Token を再発行するユースケース。
 */
class RefreshAccessTokenUseCase(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val refreshTokenTtlDays: Int = 30
) {

    /**
     * リフレッシュ要求に必要なトークンと（任意の）ユーザーIDをまとめたコマンド。
     */
    data class Command(
        val refreshToken: String,
        val userId: Long? = null
    )

    /**
     * 提供されたリフレッシュトークンを検証し、アクセストークンと新しいリフレッシュトークンを発行する。
     */
    suspend operator fun invoke(command: Command): LoginUserUseCase.Result {
        val tokenUserId = RefreshTokenHelper.extractUserId(command.refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token format")
        val requestUserId = command.userId
        if (requestUserId != null && requestUserId != tokenUserId) {
            throw IllegalArgumentException("Refresh token does not match requested userId")
        }
        val userId = tokenUserId

        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found: $userId")

        val now = Clock.System.now()
        refreshTokenRepository.deleteExpired(now)

        val matched = refreshTokenRepository
            .findValidTokens(userId, now)
            .firstOrNull { RefreshTokenHelper.verify(command.refreshToken, it.tokenHash) }
            ?: throw IllegalArgumentException("Invalid refresh token")

        refreshTokenRepository.delete(matched.id ?: error("Refresh token id missing"))

        val newRefreshToken = RefreshTokenHelper.generateToken(userId)
        val refreshTokenHash = RefreshTokenHelper.hash(newRefreshToken)
        val refreshExpiresAt = now.plusDays(refreshTokenTtlDays)
        refreshTokenRepository.create(userId, refreshTokenHash, refreshExpiresAt)

        val tokenResult = jwtProvider.generateToken(userId)

        return LoginUserUseCase.Result(
            user = user,
            token = tokenResult.token,
            refreshToken = newRefreshToken,
            issuedAt = tokenResult.issuedAt
        )
    }
}

