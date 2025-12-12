package com.example.presentation.routes

/**
 * Ranking routes entry point.
 * - Exposes the ranking API (game type × period) over HTTP.
 * - Validates query parameters, forwards them to the use case, and responds with JSON.
 */

import com.example.domain.model.GameType
import com.example.domain.model.StatsPeriod
import com.example.presentation.dto.MyRankingResponse
import com.example.presentation.dto.MyRankingStatsResponse
import com.example.presentation.dto.RankingEntryResponse
import com.example.presentation.dto.RankingListResponse
import com.example.usecase.game.GetMyRankingUseCase
import com.example.usecase.game.GetRankingUseCase
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Installs the ranking routes.
 */
fun Route.installRankingRoutes(
    getRankingUseCase: GetRankingUseCase,
    getMyRankingUseCase: GetMyRankingUseCase
) {
    get("/ranking/me") {
        val userId = call.userId()
        val modeRaw = call.request.queryParameters["mode"]
            ?: return@get call.respondValidationError("mode", "REQUIRED", "mode は four / three で指定してください。")
        val gameType = modeRaw.toGameType()
            ?: return@get call.respondValidationError("mode", "INVALID_MODE", "mode は four / three のいずれかです。")

        val rangeRaw = call.request.queryParameters["range"]
            ?: return@get call.respondValidationError("range", "REQUIRED", "range は daily / weekly / monthly / yearly のいずれかです。")

        val timeZone = TimeZone.currentSystemDefault()
        val targetDateRaw = call.request.queryParameters["targetDate"]
        val baseDate = when {
            targetDateRaw.isNullOrBlank() -> null
            else -> parseTargetDate(rangeRaw, targetDateRaw)
                ?: return@get call.respondValidationError(
                    field = "targetDate",
                    code = "INVALID_DATE",
                    message = "targetDate の形式が正しくありません。"
                )
        }

        val period = resolveRangePeriod(
            rangeRaw = rangeRaw,
            labelOverride = null,
            timeZone = timeZone,
            baseDate = baseDate
        ) ?: return@get call.respondValidationError(
            field = "range",
            code = "INVALID_RANGE",
            message = "range は daily / weekly / monthly / yearly のいずれかを指定してください。"
        )

        val result = getMyRankingUseCase(
            GetMyRankingUseCase.Command(
                userId = userId,
                gameType = gameType,
                period = period
            )
        )
        call.respond(MyRankingResponse.from(result))
    }

    get("/ranking") {
        val actorId = call.userId()
        val typeRaw = call.request.queryParameters["type"]
            ?: return@get call.respondValidationError("type", "REQUIRED", "type クエリパラメータは必須です。")
        val type = runCatching { GameType.valueOf(typeRaw.uppercase()) }.getOrElse {
            return@get call.respondValidationError("type", "INVALID_GAME_TYPE", "type は SANMA もしくは YONMA を指定してください。")
        }

        val timeZone = TimeZone.currentSystemDefault()
        val rangeRaw = call.request.queryParameters["range"]
        val label = call.request.queryParameters["label"]

        val period = if (rangeRaw != null) {
            resolveRangePeriod(rangeRaw, label, timeZone, baseDate = null)
                ?: return@get call.respondValidationError(
                    field = "range",
                    code = "INVALID_RANGE",
                    message = "range は daily / weekly / monthly / yearly のいずれかを指定してください。"
                )
        } else {
            val startRaw = call.request.queryParameters["start"]
                ?: return@get call.respondValidationError("start", "REQUIRED", "start クエリパラメータは必須です。")
            val start = runCatching { Instant.parse(startRaw) }.getOrElse {
                return@get call.respondValidationError("start", "INVALID_DATETIME", "start は ISO8601 形式で入力してください。")
            }
            val endRaw = call.request.queryParameters["end"]
                ?: return@get call.respondValidationError("end", "REQUIRED", "end クエリパラメータは必須です。")
            val end = runCatching { Instant.parse(endRaw) }.getOrElse {
                return@get call.respondValidationError("end", "INVALID_DATETIME", "end は ISO8601 形式で入力してください。")
            }
            val name = label ?: "custom"
            StatsPeriod(name = name, start = start, end = end)
        }

        val command = GetRankingUseCase.Command(
            gameType = type,
            period = period
        )

        val ranking = getRankingUseCase(command)
        val myIndex = ranking.indexOfFirst { it.userId == actorId }
        val myStats = if (myIndex >= 0) {
            val entry = ranking[myIndex]
            MyRankingStatsResponse(
                games = entry.gameCount,
                averageRank = entry.averagePlace
            )
        } else {
            null
        }
        val response = RankingListResponse(
            myRank = if (myIndex >= 0) myIndex + 1 else null,
            totalPlayers = ranking.size,
            myStats = myStats,
            ranking = ranking.map(RankingEntryResponse::from)
        )
        call.respond(response)
    }
}

private fun resolveRangePeriod(
    rangeRaw: String,
    labelOverride: String?,
    timeZone: TimeZone,
    baseDate: LocalDate?
): StatsPeriod? {
    val today = baseDate ?: Clock.System.now().toLocalDateTime(timeZone).date
    val label = labelOverride ?: rangeRaw.lowercase()
    return when (rangeRaw.lowercase()) {
        "daily" -> periodForDates(label, today, today.plus(DatePeriod(days = 1)), timeZone)
        "weekly" -> {
            val startDate = today.minus(DatePeriod(days = 6))
            periodForDates(label, startDate, today.plus(DatePeriod(days = 1)), timeZone)
        }
        "monthly" -> {
            val startDate = LocalDate(today.year, today.monthNumber, 1)
            periodForDates(label, startDate, startDate.plus(DatePeriod(months = 1)), timeZone)
        }
        "yearly" -> {
            val startDate = LocalDate(today.year, 1, 1)
            periodForDates(label, startDate, startDate.plus(DatePeriod(years = 1)), timeZone)
        }
        else -> null
    }
}

private fun periodForDates(
    name: String,
    startDate: LocalDate,
    endDate: LocalDate,
    timeZone: TimeZone
): StatsPeriod =
    StatsPeriod(
        name = name,
        start = startDate.atStartOfDayIn(timeZone),
        end = endDate.atStartOfDayIn(timeZone)
    )

private fun String.toGameType(): GameType? =
    when (lowercase()) {
        "four", "yonma", "4" -> GameType.YONMA
        "three", "sanma", "3" -> GameType.SANMA
        else -> null
    }

private fun parseTargetDate(rangeRaw: String, raw: String): LocalDate? =
    when (rangeRaw.lowercase()) {
        "monthly" -> parseYearMonth(raw)
        "yearly" -> parseYear(raw)
        "weekly" -> parseWeekDate(raw)
        else -> runCatching { LocalDate.parse(raw) }.getOrNull()
    }

private fun parseWeekDate(raw: String): LocalDate? =
    runCatching { LocalDate.parse(raw) }.getOrElse {
        parseYearMonth(raw)
    }

private fun parseYearMonth(value: String): LocalDate? {
    val parts = value.split("-")
    if (parts.size != 2) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    return LocalDate(year, month, 1)
}

private fun parseYear(value: String): LocalDate? {
    val year = value.toIntOrNull() ?: return null
    return LocalDate(year, 1, 1)
}
