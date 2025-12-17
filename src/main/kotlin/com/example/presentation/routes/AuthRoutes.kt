package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - 認証系 API（登録・ログイン・リフレッシュ）のルーティングをまとめたファイルです。
 * - ログイン試行数の制御や共通レスポンス処理を使って、安全なエンドポイントを構築しています。
 */

import com.example.common.error.ErrorResponse
import com.example.presentation.dto.LoginRequest
import com.example.presentation.dto.LoginResponse
import com.example.presentation.dto.RefreshRequest
import com.example.presentation.dto.RefreshTokenResponse
import com.example.presentation.dto.RegisterRequest
import com.example.presentation.dto.UserResponse
import com.example.security.LoginAttemptTracker
import com.example.usecase.auth.LoginUserUseCase
import com.example.usecase.auth.RefreshAccessTokenUseCase
import com.example.usecase.user.CreateUserUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * 認証系 API のルーティング。
 */
fun Route.installAuthRoutes(
    createUserUseCase: CreateUserUseCase,
    loginUserUseCase: LoginUserUseCase,
    refreshAccessTokenUseCase: RefreshAccessTokenUseCase,
    loginAttemptTracker: LoginAttemptTracker,
    accessTokenExpiresInSec: Long
) {
    val invalidLoginMessage = "ID またはパスワードが不正です。"
    val lockedMessage = "一定回数ログインに失敗したため、しばらくログインできません。"

    suspend fun handleRegister(call: ApplicationCall) {
        val request = call.receive<RegisterRequest>()
        val created = createUserUseCase(
            CreateUserUseCase.Command(
                name = request.name,
                nickname = request.nickname,
                storeId = request.storeId,
                prefectureCode = request.prefectureCode,
                email = request.email,
                zooId = request.zooId,
                password = request.password,
                passwordConfirm = request.passwordConfirm
            )
        )
        call.respond(HttpStatusCode.Created, UserResponse.from(created))
    }

    post("/register") { handleRegister(call) }

    route("/auth") {
        post("/register") { handleRegister(call) }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val normalizedEmail = request.email.trim()
            val clientIp = call.clientIp()
            val lockRemaining = loginAttemptTracker.currentLockRemaining(normalizedEmail, clientIp)
            if (lockRemaining != null) {
                return@post call.respond(
                    HttpStatusCode.TooManyRequests,
                    ErrorResponse(
                        errorCode = "LOGIN_LOCKED",
                        message = lockedMessage
                    )
                )
            }

            val result = try {
                loginUserUseCase(
                    LoginUserUseCase.Command(
                        email = normalizedEmail,
                        password = request.password
                    )
                )
            } catch (ex: LoginUserUseCase.InvalidCredentialsException) {
                loginAttemptTracker.registerFailure(normalizedEmail, clientIp)
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        errorCode = "LOGIN_FAILED",
                        message = invalidLoginMessage
                    )
                )
            } catch (ex: LoginUserUseCase.DeletedAccountException) {
                call.skipDefaultErrorHandling()
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    ErrorResponse(
                        errorCode = "ACCOUNT_DELETED",
                        message = "このアカウントは削除されています。"
                    )
                )
            }
            loginAttemptTracker.reset(normalizedEmail, clientIp)
            call.respond(LoginResponse.from(result))
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val result = try {
                refreshAccessTokenUseCase(
                    RefreshAccessTokenUseCase.Command(
                        refreshToken = request.refreshToken,
                        userId = request.userId
                    )
                )
            } catch (ex: IllegalArgumentException) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        errorCode = "UNAUTHORIZED",
                        message = "Invalid refresh token"
                    )
                )
            }
            call.respond(
                RefreshTokenResponse(
                    accessToken = result.token,
                    refreshToken = result.refreshToken,
                    expiresIn = accessTokenExpiresInSec
                )
            )
        }
    }
}

private fun ApplicationCall.clientIp(): String =
    request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
        ?: request.local.remoteHost
