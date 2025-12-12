package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - ルート内で共通して使う拡張関数やバリデーションレスポンスを集めたファイルです。
 * - userId 取得・StatsRange 変換・権限チェックなどの小さな処理をここに集約しています。
 */

import com.example.common.error.ErrorResponse
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.StatsRange
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.presentation.util.ValidationMessageResolver
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.host
import io.ktor.server.request.path
import io.ktor.server.response.respond
import java.time.YearMonth
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private val ALL_TIME_START = Instant.parse("1900-01-01T00:00:00Z")
private val ALL_TIME_END = Instant.parse("3000-01-01T00:00:00Z")

/**
 * ルーティングで共有するヘルパー拡張。
 */
internal fun ApplicationCall.userIdOrNull(): Long? =
    parameters["userId"]?.toLongOrNull()

/**
 * JWT から現在ログインしているユーザーIDを取得します。
 * 認証情報が無い場合は 401 を返して処理を停止します。
 */
internal suspend fun ApplicationCall.userId(): Long {
    val actorId = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
    if (actorId == null) {
        respondUnauthorized()
        error("Missing authenticated user id.")
    }
    return actorId
}

internal suspend fun ApplicationCall.respondValidationErrors(
    errors: List<FieldError>,
    message: String = ValidationMessageResolver.defaultMessage(),
    status: HttpStatusCode = HttpStatusCode.BadRequest
) {
    val details = ValidationMessageResolver.fromFieldErrors(errors)
    respond(
        status,
        ValidationMessagesResponse(
            message = message,
            errors = details
        )
    )
}

internal suspend fun ApplicationCall.respondValidationError(
    field: String?,
    code: String,
    message: String,
    status: HttpStatusCode = HttpStatusCode.BadRequest
) {
    respondValidationErrors(
        errors = listOf(FieldError(field = field, code = code, message = message)),
        status = status
    )
}

internal suspend fun ApplicationCall.respondInvalidUserId() {
    respondValidationError(
        field = "userId",
        code = "INVALID_USER_ID",
        message = "ユーザーIDは数値で指定してください。"
    )
}

internal fun ApplicationCall.statsRangeOrNull(): StatsRange? {
    val startRaw = request.queryParameters["startDate"] ?: request.queryParameters["start"]
    val endRaw = request.queryParameters["endDate"] ?: request.queryParameters["end"]

    if (startRaw == null && endRaw == null) {
        return StatsRange(ALL_TIME_START, ALL_TIME_END)
    }
    if (startRaw == null || endRaw == null) return null

    val zone = TimeZone.currentSystemDefault()
    val startDate = normalizeToLocalDate(startRaw, zone) ?: return null
    val endDate = normalizeToLocalDate(endRaw, zone) ?: return null
    val start = startDate.atStartOfDayIn(zone)
    val endExclusive = endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
    return StatsRange(start, endExclusive)
}

internal suspend fun ApplicationCall.respondMissingStatsRange() {
    respondValidationErrors(
        errors = listOf(
            FieldError("start", "REQUIRED", "開始日は必須です。"),
            FieldError("end", "REQUIRED", "終了日は必須です。")
        )
    )
}

private fun normalizeToLocalDate(raw: String, zone: TimeZone): LocalDate? {
    return runCatching { LocalDate.parse(raw) }.getOrElse {
        runCatching { Instant.parse(raw).toLocalDateTime(zone).date }.getOrNull()
    }
}

internal fun ApplicationCall.queryYearMonth(): YearMonth? =
    request.queryParameters["yearMonth"]?.let { runCatching { YearMonth.parse(it) }.getOrNull() }

internal suspend fun ApplicationCall.respondMissingYearMonth() {
    respondValidationError(
        field = "yearMonth",
        code = "REQUIRED",
        message = "yearMonth は YYYY-MM 形式で入力してください。"
    )
}

internal suspend fun ApplicationCall.requireUserAccess(targetUserId: Long): Long? {
    val actorId = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
        ?: run {
            respondUnauthorized()
            return null
        }
    if (actorId != targetUserId) {
        respondForbidden()
        return null
    }
    return actorId
}

internal suspend fun ApplicationCall.requireAuditContext(targetUserId: Long): AuditContext? {
    val actorId = requireUserAccess(targetUserId) ?: return null
    return buildAuditContext(actorId)
}

internal fun ApplicationCall.buildAuditContext(actorId: Long): AuditContext {
    val path = request.path()
    val ip = request.headers[HttpHeaders.XForwardedFor]
        ?: request.headers["X-Real-IP"]
        ?: request.host()
    return AuditContext(
        performedBy = actorId,
        path = path,
        ipAddress = ip
    )
}

internal suspend fun ApplicationCall.respondForbidden(
    message: String = "You are not allowed to access this resource."
) {
    respond(
        HttpStatusCode.Forbidden,
        ErrorResponse(
            errorCode = "FORBIDDEN",
            message = message
        )
    )
}

private suspend fun ApplicationCall.respondUnauthorized() {
    respond(
        HttpStatusCode.Unauthorized,
        ErrorResponse(
            errorCode = "UNAUTHORIZED",
            message = "Authentication required."
        )
    )
}

internal fun ApplicationCall.pathYearMonthOrNull(paramName: String): YearMonth? =
    parameters[paramName]?.let {
        runCatching { YearMonth.parse(it) }.getOrNull()
    }

internal suspend fun ApplicationCall.respondInvalidYearMonth(paramName: String) {
    respondValidationError(
        field = paramName,
        code = "INVALID_YEAR_MONTH",
        message = "$paramName は YYYY-MM 形式で入力してください。"
    )
}

