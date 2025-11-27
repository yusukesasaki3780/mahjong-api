package com.example.presentation.routes

import com.example.presentation.dto.dashboard.DashboardSummaryResponse
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

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
                    transportTotal = 500,
                    gameIncomeTotal = 1000,
                    advanceAmount = 2500.0,
                    grossSalary = 201500.0,
                    netSalary = 191500.0,
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
