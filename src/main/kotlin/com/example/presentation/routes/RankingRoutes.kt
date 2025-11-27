package com.example.presentation.routes

/**
 * Ranking routes entry point.
 * - Exposes the ranking API (game type × period) over HTTP.
 * - Validates query parameters, forwards them to the use case, and responds with JSON.
 */

import com.example.domain.model.GameType
import com.example.domain.model.StatsPeriod
import com.example.presentation.dto.RankingEntryResponse
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
 * Installs the ranking route.
 */
fun Route.installRankingRoutes(
    getRankingUseCase: GetRankingUseCase
) {
    get("/ranking") {
        val typeRaw = call.request.queryParameters["type"]
            ?: return@get call.respondValidationError("type", "REQUIRED", "type クエリパラメータは必須です。")
        val type = runCatching { GameType.valueOf(typeRaw.uppercase()) }.getOrElse {
            return@get call.respondValidationError("type", "INVALID_GAME_TYPE", "type は SANMA もしくは YONMA を指定してください。")
        }

        val timeZone = TimeZone.currentSystemDefault()
        val rangeRaw = call.request.queryParameters["range"]
        val label = call.request.queryParameters["label"]

        val period = if (rangeRaw != null) {
            resolveRangePeriod(rangeRaw, label, timeZone)
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
        call.respond(ranking.map(RankingEntryResponse::from))
    }
}

private fun resolveRangePeriod(
    rangeRaw: String,
    labelOverride: String?,
    timeZone: TimeZone
): StatsPeriod? {
    val today = Clock.System.now().toLocalDateTime(timeZone).date
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
