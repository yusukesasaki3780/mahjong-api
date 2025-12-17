package com.example

import com.example.common.error.AccessDeniedException
import com.example.common.error.DomainValidationException
import com.example.common.error.ErrorResponse
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.presentation.routes.shouldSkipDefaultErrorHandling
import com.example.presentation.util.ValidationMessageResolver
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.event.Level
import org.valiktor.ConstraintViolationException

/**
 * 監視と共通エラーハンドリングの設定をまとめる。
 */
fun Application.configureMonitoring() {

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<ConstraintViolationException> { call, cause ->
            val details = cause.constraintViolations.map { ValidationMessageResolver.fromConstraint(it) }
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationMessagesResponse(
                    message = ValidationMessageResolver.defaultMessage(),
                    errors = details
                )
            )
        }

        exception<DomainValidationException> { call, cause ->
            val details = ValidationMessageResolver.fromFieldErrors(cause.violations)
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationMessagesResponse(
                    message = ValidationMessageResolver.defaultMessage(),
                    errors = details
                )
            )
        }

        status(HttpStatusCode.Unauthorized) { call, status ->
            if (call.shouldSkipDefaultErrorHandling()) return@status
            call.respond(
                status,
                ErrorResponse(
                    errorCode = "UNAUTHORIZED",
                    message = "認証が必要です。"
                )
            )
        }

        exception<AccessDeniedException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse(
                    errorCode = "FORBIDDEN",
                    message = cause.message ?: "You are not allowed to access this resource."
                )
            )
        }

        status(HttpStatusCode.Forbidden) { call, status ->
            if (call.shouldSkipDefaultErrorHandling()) return@status
            call.respond(
                status,
                ErrorResponse(
                    errorCode = "FORBIDDEN",
                    message = "アクセス権限がありません。"
                )
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            if (call.shouldSkipDefaultErrorHandling()) return@status
            call.respond(
                status,
                ErrorResponse(
                    errorCode = "NOT_FOUND",
                    message = "リソースが見つかりません。"
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled server error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    errorCode = "INTERNAL_ERROR",
                    message = "予期しないエラーが発生しました。"
                )
            )
        }
    }
}
