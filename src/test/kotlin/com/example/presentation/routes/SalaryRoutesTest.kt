package com.example.presentation.routes

import com.example.presentation.dto.ValidationMessagesResponse
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import java.time.YearMonth
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SalaryRoutesTest : RoutesTestBase() {

    @Test
    fun `salary endpoint returns data`() = testApplication {
        coEvery { calculateMonthlySalaryUseCase(1, YearMonth.parse("2025-01")) } returns
            CalculateMonthlySalaryUseCase.Result(
                userId = 1,
                yearMonth = YearMonth.parse("2025-01"),
                totalWorkMinutes = 6000,
                totalDayMinutes = 5400,
                totalNightMinutes = 600,
                baseWageTotal = 180000.0,
                nightExtraTotal = 20000.0,
                specialAllowanceTotal = 0.0,
                specialAllowances = emptyList(),
                gameIncomeTotal = 1000,
                transportTotal = 500,
                advanceAmount = 2500.0,
                grossSalary = 201500.0,
                incomeTax = 10000.0,
                netSalary = 191500.0
        )
        installRoutes()
        val response = client.get("/users/1/salary/2025-01") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `invalid yearMonth returns validation error`() = testApplication {
        installRoutes()
        val response = client.get("/users/1/salary/invalid") {
            withAuth()
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("yearMonth", error.errors.first().field)
    }
}
