package com.example.presentation.routes

import com.example.presentation.dto.dashboard.DashboardSummaryResponse
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import java.time.YearMonth
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DashboardRoutesTest : RoutesTestBase() {

    @Test
    fun `returns summary when authorized`() = testApplication {
        coEvery { getDashboardSummaryUseCase(1, YearMonth.parse("2025-01")) } returns
            GetDashboardSummaryUseCase.Result(
                userId = 1,
                yearMonth = YearMonth.parse("2025-01"),
                work = GetDashboardSummaryUseCase.WorkSummary(
                    totalWorkMinutes = 6000,
                    totalDayMinutes = 5400,
                    totalNightMinutes = 600,
                    totalShifts = 10
                ),
                salary = GetDashboardSummaryUseCase.SalarySummary(
                    baseWageTotal = 180000.0,
                    nightExtraTotal = 20000.0,
                    specialAllowanceTotal = 5000.0,
                    specialAllowances = listOf(
                        GetDashboardSummaryUseCase.SpecialAllowanceSummary(
                            type = "special_hourly_wage",
                            label = "年末年始",
                            unitPrice = 1500,
                            hours = 2.0,
                            amount = 3000.0,
                            specialHourlyWageId = 1
                        ),
                        GetDashboardSummaryUseCase.SpecialAllowanceSummary(
                            type = "night_bonus",
                            label = "深夜給（特別手当）",
                            unitPrice = 1875,
                            hours = 1.6,
                            amount = 2000.0,
                            specialHourlyWageId = null
                        )
                    ),
                    transportTotal = 500,
                    gameIncomeTotal = 1000,
                    advanceAmount = 2500.0,
                    grossSalary = 206500.0,
                    netSalary = 196500.0,
                    incomeTax = 10000.0
                ),
                game = GetDashboardSummaryUseCase.GameSummary(
                    totalGames = 20,
                    yonma = GetDashboardSummaryUseCase.GameTypeSummary(10, 2.0, 1000),
                    sanma = GetDashboardSummaryUseCase.GameTypeSummary(10, 1.5, 1500)
                )
            )
        installRoutes()

        val response = client.get("/users/1/dashboard/summary?yearMonth=2025-01") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<DashboardSummaryResponse>(response.bodyAsText())
        assertEquals("2025-01", body.yearMonth)
        assertEquals(10, body.workShiftCount)
        assertEquals(20000.0, body.salaryNightExtraTotal)
        assertEquals(5000.0, body.salarySpecialAllowanceTotal)
        assertEquals(2, body.salarySpecialAllowances.size)
        assertEquals(2500.0, body.salaryAdvanceAmount)
        assertEquals(20, body.totalGames)
    }

    @Test
    fun `returns 403 when user mismatch`() = testApplication {
        installRoutes()
        val response = client.get("/users/1/dashboard/summary?yearMonth=2025-01") {
            withAuth(userId = 2)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `missing yearMonth returns 400`() = testApplication {
        installRoutes()
        val response = client.get("/users/1/dashboard/summary") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
