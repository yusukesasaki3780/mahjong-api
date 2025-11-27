package com.example.presentation.routes

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.presentation.dto.PatchShiftBreakRequest
import com.example.presentation.dto.PatchShiftRequest
import com.example.presentation.dto.ShiftRequest
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.usecase.shift.DeleteShiftUseCase
import com.example.usecase.shift.EditShiftUseCase
import com.example.usecase.shift.GetMonthlyShiftUseCase
import com.example.usecase.shift.GetShiftStatsUseCase
import com.example.usecase.shift.PatchShiftUseCase
import com.example.usecase.shift.RegisterShiftUseCase
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.datetime.LocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ShiftRoutesTest : RoutesTestBase() {

    @Test
    fun `get monthly shifts`() = testApplication {
        coEvery { getMonthlyShiftUseCase(1, java.time.YearMonth.parse("2025-01")) } returns listOf(TestFixtures.shift())
        installRoutes()
        val response = client.get("/users/1/shifts?yearMonth=2025-01") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `invalid yearMonth returns 400`() = testApplication {
        installRoutes()
        val response = client.get("/users/1/shifts?yearMonth=invalid") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("yearMonth", error.errors.first().field)
    }

    @Test
    fun `post shift creates record`() = testApplication {
        coEvery { registerShiftUseCase(any()) } returns TestFixtures.shift()
        installRoutes()
        val response = client.post("/users/1/shifts") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ShiftRequest(
                        workDate = "2025-01-01",
                        startTime = "09:00",
                        endTime = "18:00",
                        memo = "memo",
                        breaks = emptyList()
                    )
                )
            )
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        coVerify { registerShiftUseCase(any()) }
    }

    @Test
    fun `post shift returns friendly validation errors`() = testApplication {
        coEvery { registerShiftUseCase(any()) } throws DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "breaks[0]",
                    code = "BREAK_OUTSIDE",
                    message = "休憩時間が勤務時間外です。"
                )
            )
        )
        installRoutes()
        val response = client.post("/users/1/shifts") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ShiftRequest(
                        workDate = "2025-01-01",
                        startTime = "09:00",
                        endTime = "18:00",
                        memo = "memo",
                        breaks = emptyList()
                    )
                )
            )
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("入力内容に誤りがあります。", body.message)
        assertEquals(listOf("breaks[0]"), body.errors.mapNotNull { it.field })
        assertEquals(listOf("休憩時間が勤務時間外です。"), body.errors.map { it.message })
    }

    @Test
    fun `put shift replaces record`() = testApplication {
        coEvery { editShiftUseCase(any(), any()) } returns TestFixtures.shift()
        installRoutes()
        val response = client.put("/users/1/shifts/5") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ShiftRequest(
                        workDate = "2025-01-01",
                        startTime = "09:00",
                        endTime = "18:00",
                        memo = "memo",
                        breaks = emptyList()
                    )
                )
            )
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { editShiftUseCase(any(), any()) }
    }

    @Test
    fun `patch shift updates subset`() = testApplication {
        coEvery { patchShiftUseCase(any(), any()) } returns TestFixtures.shift().copy(memo = "patched")
        installRoutes()
        val response = client.patch("/users/1/shifts/5") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    PatchShiftRequest(
                        workDate = "2025-01-01",
                        startTime = "09:00",
                        endTime = "18:00",
                        memo = "patched",
                        breaks = listOf(PatchShiftBreakRequest(id = 100, delete = true))
                    )
                )
            )
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { patchShiftUseCase(any(), any()) }
    }

    @Test
    fun `delete shift`() = testApplication {
        coEvery { deleteShiftUseCase(5, any()) } returns true
        installRoutes()
        val response = client.delete("/users/1/shifts/5") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `get shift stats returns summary`() = testApplication {
        coEvery {
            getShiftStatsUseCase(1, YearMonth.parse("2025-11"))
        } returns GetShiftStatsUseCase.Result(
            userId = 1,
            yearMonth = YearMonth.parse("2025-11"),
            totalMinutes = 1200,
            nightMinutes = 300,
            workDays = 10,
            shiftCount = 10
        )
        installRoutes()

        val response = client.get("/users/1/shifts/stats?yearMonth=2025-11") {
            withAuth(userId = 1)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getShiftStatsUseCase(1, YearMonth.parse("2025-11")) }
    }

    @Test
    fun `get weekly shifts with date range`() = testApplication {
        val start = LocalDate.parse("2025-11-24")
        val end = LocalDate.parse("2025-11-30")
        coEvery { getShiftRangeUseCase(1, start, end) } returns listOf(TestFixtures.shift())
        installRoutes()

        val response = client.get("/users/1/shifts?rangeType=week&start=$start&end=$end") {
            withAuth(userId = 1)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getShiftRangeUseCase(1, start, end) }
    }

    @Test
    fun `week range missing start returns 400`() = testApplication {
        installRoutes()

        val response = client.get("/users/1/shifts?rangeType=week&end=2025-11-30") {
            withAuth(userId = 1)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("start", error.errors.first().field)
    }
}



