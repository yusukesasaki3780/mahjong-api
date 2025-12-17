package com.example.usecase.auth

/**
 * ### このファイルの役割
 * - ユーザーIDとパスワードを検証し、アクセストークンとリフレッシュトークンを新規発行するユースケースです。
 * - Command/Result で入力と出力をラップし、UI 層からは単純な呼び出しで済むようにしています。
 * - 内部では古いリフレッシュトークンの削除や JWT 生成など、ログイン時の周辺処理もまとめて実行します。
 */

import com.example.config.JwtProvider
import com.example.domain.model.User
import com.example.domain.repository.RefreshTokenRepository
import com.example.domain.repository.UserCredentialRepository
import com.example.domain.repository.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import com.example.usecase.auth.plusDays

/**
 * ログイン処理を行い、AccessToken / RefreshToken を発行するユースケース。
 */
class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val refreshTokenTtlDays: Int = 30
) {

    class InvalidCredentialsException : RuntimeException("Invalid credentials")
    class DeletedAccountException : RuntimeException("Account deleted")

    data class Command(val email: String, val password: String)

    data class Result(
        val user: User,
        val token: String,
        val refreshToken: String,
        val issuedAt: Instant
    )

    suspend operator fun invoke(command: Command): Result {
        // 1. ユーザー情報と入力されたパスワードを検証する
        val user = userRepository.findByEmail(command.email)
            ?: throw InvalidCredentialsException()
        if (user.isDeleted) {
            throw DeletedAccountException()
        }

        val userId = user.id ?: throw IllegalArgumentException("User id missing.")
        val verified = credentialRepository.verifyPassword(userId, command.password)
        if (!verified) throw InvalidCredentialsException()

        // 2. 既存のリフレッシュトークンを掃除し、新しいトークン情報を DB に保存する
        val now = Clock.System.now()
        refreshTokenRepository.deleteExpired(now)
        refreshTokenRepository.deleteAllForUser(userId)

        val refreshToken = RefreshTokenHelper.generateToken(userId)
        val refreshTokenHash = RefreshTokenHelper.hash(refreshToken)
        val refreshExpiresAt = now.plusDays(refreshTokenTtlDays)
        refreshTokenRepository.create(userId, refreshTokenHash, refreshExpiresAt)

        // 3. JWT を発行して、アクセストークン＋リフレッシュトークンをまとめて返す
        val tokenResult = jwtProvider.generateToken(userId)

        return Result(
            user = user,
            token = tokenResult.token,
            refreshToken = refreshToken,
            issuedAt = tokenResult.issuedAt
        )
    }
}
