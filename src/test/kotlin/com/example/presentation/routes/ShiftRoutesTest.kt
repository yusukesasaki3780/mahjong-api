package com.example.presentation.routes

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.presentation.dto.PatchShiftBreakRequest
import com.example.presentation.dto.PatchShiftRequest
import com.example.presentation.dto.ShiftRequest
import com.example.presentation.dto.ShiftRequirementUpsertRequest
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.usecase.shift.GetShiftBoardUseCase
import com.example.usecase.shift.GetShiftStatsUseCase
import com.example.usecase.shift.RegisterShiftUseCase
import com.example.domain.model.ShiftRequirement
import com.example.domain.model.ShiftSlotType
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ShiftRoutesTest : RoutesTestBase() {

    @Test
    fun `get monthly shifts`() = testApplication {
        coEvery { getMonthlyShiftUseCase(1, 1, YearMonth.parse("2025-01")) } returns listOf(TestFixtures.shift())
        installRoutes()

        val response = client.get("/users/1/shifts?yearMonth=2025-01") { withAuth(userId = 1) }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getMonthlyShiftUseCase(1, 1, YearMonth.parse("2025-01")) }
    }

    @Test
    fun `invalid yearMonth returns 400`() = testApplication {
        installRoutes()

        val response = client.get("/users/1/shifts?yearMonth=invalid") { withAuth(userId = 1) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("yearMonth", error.errors.first().field)
    }

    @Test
    fun `post shift creates record`() = testApplication {
        val command = slot<RegisterShiftUseCase.Command>()
        coEvery { registerShiftUseCase(capture(command), any()) } returns TestFixtures.shift()
        installRoutes()

        val response = client.post("/users/1/shifts?storeId=1") {
            withAuth(userId = 1)
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
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(1, command.captured.targetUserId)
        assertEquals(1, command.captured.actorId)
        assertEquals(1, command.captured.requestedStoreId)
    }

    @Test
    fun `post shift without storeId lets context decide`() = testApplication {
        val command = slot<RegisterShiftUseCase.Command>()
        coEvery { registerShiftUseCase(capture(command), any()) } returns TestFixtures.shift()
        installRoutes()

        val response = client.post("/users/1/shifts") {
            withAuth(userId = 1)
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
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(null, command.captured.requestedStoreId)
    }

    @Test
    fun `post shift ignores blank storeId query`() = testApplication {
        val command = slot<RegisterShiftUseCase.Command>()
        coEvery { registerShiftUseCase(capture(command), any()) } returns TestFixtures.shift()
        installRoutes()

        val response = client.post("/users/1/shifts?storeId=") {
            withAuth(userId = 1)
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
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(null, command.captured.requestedStoreId)
    }

    @Test
    fun `post shift surfaces validation errors`() = testApplication {
        coEvery { registerShiftUseCase(any(), any()) } throws DomainValidationException(
            violations = listOf(FieldError("breaks[0]", "BREAK_OUTSIDE", "?????????????"))
        )
        installRoutes()

        val response = client.post("/users/1/shifts?storeId=1") {
            withAuth(userId = 1)
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
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `admin can suggest store via query`() = testApplication {
        val command = slot<RegisterShiftUseCase.Command>()
        coEvery { registerShiftUseCase(capture(command), any()) } returns TestFixtures.shift(userId = 2, storeId = 99)
        installRoutes()

        val response = client.post("/users/2/shifts?storeId=99") {
            withAuth(userId = 1)
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ShiftRequest(
                        workDate = "2025-01-01",
                        startTime = "09:00",
                        endTime = "18:00",
                        memo = "help shift",
                        breaks = emptyList()
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(99, command.captured.requestedStoreId)
    }

    @Test
    fun `put shift replaces record`() = testApplication {
        coEvery { editShiftUseCase(any(), any()) } returns TestFixtures.shift()
        installRoutes()

        val response = client.put("/users/1/shifts/5?storeId=1") {
            withAuth(userId = 1)
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
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { editShiftUseCase(any(), any()) }
    }

    @Test
    fun `patch shift updates subset`() = testApplication {
        coEvery { patchShiftUseCase(any(), any()) } returns TestFixtures.shift().copy(memo = "patched")
        installRoutes()

        val response = client.patch("/users/1/shifts/5?storeId=1") {
            withAuth(userId = 1)
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
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `delete shift`() = testApplication {
        coEvery { deleteShiftUseCase(any(), eq(5), any()) } returns true
        installRoutes()

        val response = client.delete("/users/1/shifts/5?storeId=1") { withAuth(userId = 1) }

        assertEquals(HttpStatusCode.NoContent, response.status)
        coVerify { deleteShiftUseCase(1, 5, any()) }
    }

    @Test
    fun `get shift stats returns summary`() = testApplication {
        coEvery {
            getShiftStatsUseCase(1, 1, YearMonth.parse("2025-11"))
        } returns GetShiftStatsUseCase.Result(
            userId = 1,
            yearMonth = YearMonth.parse("2025-11"),
            totalMinutes = 1200,
            nightMinutes = 300,
            workDays = 10,
            shiftCount = 10
        )
        installRoutes()

        val response = client.get("/users/1/shifts/stats?yearMonth=2025-11") { withAuth(userId = 1) }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getShiftStatsUseCase(1, 1, YearMonth.parse("2025-11")) }
    }

    @Test
    fun `get weekly shifts with date range`() = testApplication {
        val start = LocalDate.parse("2025-11-24")
        val end = LocalDate.parse("2025-11-30")
        coEvery { getShiftRangeUseCase(1, 1, start, end) } returns listOf(TestFixtures.shift())
        installRoutes()

        val response = client.get("/users/1/shifts?rangeType=week&start=$start&end=$end") { withAuth(userId = 1) }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getShiftRangeUseCase(1, 1, start, end) }
    }

    @Test
    fun `week range missing start returns 400`() = testApplication {
        installRoutes()
        val response = client.get("/users/1/shifts?rangeType=week&end=2025-11-30") { withAuth(userId = 1) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `store shift board returns aggregated data`() = testApplication {
        val command = slot<GetShiftBoardUseCase.Command>()
        coEvery { getShiftBoardUseCase(capture(command)) } returns GetShiftBoardUseCase.Result(
            storeId = 10,
            startDate = LocalDate.parse("2025-01-01"),
            endDate = LocalDate.parse("2025-01-07"),
            users = emptyList(),
            shifts = emptyList(),
            requirements = emptyList(),
            editable = true
        )
        installRoutes()

        val response = client.get("/stores/10/shift-board?startDate=2025-01-01&endDate=2025-01-07") { withAuth(userId = 1) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, command.captured.actorId)
    }

    @Test
    fun `shift board accepts ISO date strings`() = testApplication {
        coEvery { getShiftBoardUseCase(any()) } returns GetShiftBoardUseCase.Result(
            storeId = 22,
            startDate = LocalDate.parse("2025-04-01"),
            endDate = LocalDate.parse("2025-04-07"),
            users = emptyList(),
            shifts = emptyList(),
            requirements = emptyList(),
            editable = false
        )
        installRoutes()

        val response = client.get("/stores/22/shift-board?startDate=2025-04-01T00:00:00Z&endDate=2025-04-07T23:59:59Z") {
            withAuth(userId = 5)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `shift board surfaces validation errors`() = testApplication {
        coEvery { getShiftBoardUseCase(any()) } throws DomainValidationException(
            violations = listOf(FieldError("storeId", "STORE_MISMATCH", "???????????????????"))
        )
        installRoutes()

        val response = client.get("/stores/99/shift-board?startDate=2025-01-01&endDate=2025-01-07") { withAuth(userId = 8) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `admin can upsert shift requirement`() = testApplication {
        val actor = TestFixtures.user(id = 1, storeId = 10, isAdmin = true)
        val requirement = ShiftRequirement(
            id = 99,
            storeId = 10,
            targetDate = LocalDate.parse("2025-01-05"),
            shiftType = ShiftSlotType.EARLY,
            startRequired = 3,
            endRequired = 1,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        coEvery { getUserUseCase(1) } returns actor
        coEvery { upsertShiftRequirementUseCase(any()) } returns requirement
        installRoutes()

        val response = client.put("/stores/10/shift-requirements") {
            withAuth(userId = 1)
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ShiftRequirementUpsertRequest(
                        targetDate = "2025-01-05",
                        shiftType = ShiftSlotType.EARLY,
                        startRequired = 3,
                        endRequired = 1
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
