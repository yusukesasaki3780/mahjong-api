package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - 給与計算結果を取得する API のルーティング処理です。
 * - yearMonth パラメータのチェックや userId 照合を済ませてから UseCase を呼び出します。
 */

import com.example.presentation.dto.SalaryResponse
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.YearMonth

/**
 * 給与 API。
 */
fun Route.installSalaryRoutes(
    calculateMonthlySalaryUseCase: CalculateMonthlySalaryUseCase
) {
    get("/users/{userId}/salary/{yearMonth}") {
        val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
        call.requireUserAccess(userId) ?: return@get
        val yearMonthParam = call.parameters["yearMonth"]
            ?: return@get call.respondMissingYearMonth()
        val yearMonth = runCatching { YearMonth.parse(yearMonthParam) }.getOrElse {
            return@get call.respondValidationError(
                field = "yearMonth",
                code = "INVALID_YEAR_MONTH",
                message = "yearMonth は YYYY-MM 形式で入力してください。"
            )
        }

        val salary = calculateMonthlySalaryUseCase(userId, yearMonth)
        call.respond(SalaryResponse.from(salary))
    }
}

