package com.example.presentation.routes

import com.example.TestFixtures
import com.example.presentation.dto.PatchGameResultRequest
import com.example.presentation.dto.UpsertGameResultRequest
import com.example.common.error.ErrorResponse
import com.example.usecase.game.DeleteGameResultUseCase
import com.example.usecase.game.EditGameResultUseCase
import com.example.usecase.game.GetUserStatsUseCase
import com.example.usecase.game.PatchGameResultUseCase
import com.example.usecase.game.RecordGameResultUseCase
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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameResultRoutesTest : RoutesTestBase() {

    @Test
    fun `get user stats requires range`() = testApplication {
        coEvery { getUserStatsUseCase(any<GetUserStatsUseCase.Command>()) } returns TestFixtures.userStats()
        installRoutes()
        val response = client.get("/users/1/results?startDate=2025-01-01&endDate=2025-01-31") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `get user stats accepts datetime params`() = testApplication {
        coEvery { getUserStatsUseCase(any<GetUserStatsUseCase.Command>()) } returns TestFixtures.userStats()
        installRoutes()
        val response =
            client.get("/users/1/results?start=2025-01-01T00:00:00Z&end=2025-01-31T23:59:59Z") { withAuth(userId = 1) }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `get single result returns entry`() = testApplication {
        coEvery { getGameResultUseCase(1, 10) } returns TestFixtures.gameResult()
        installRoutes()

        val response = client.get("/users/1/results/10") {
            withAuth(userId = 1)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `get single result returns 404 when missing`() = testApplication {
        coEvery { getGameResultUseCase(1, 10) } returns null
        installRoutes()

        val response = client.get("/users/1/results/10") {
            withAuth(userId = 1)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `missing range defaults to all time`() = testApplication {
        coEvery { getUserStatsUseCase(any<GetUserStatsUseCase.Command>()) } returns TestFixtures.userStats()
        installRoutes()
        val response = client.get("/users/1/results") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `post result creates record`() = testApplication {
        coEvery { recordGameResultUseCase(any()) } returns TestFixtures.gameResult()
        installRoutes()
        val request = UpsertGameResultRequest(
            gameType = com.example.domain.model.GameType.SANMA,
            playedAt = LocalDate.parse("2025-01-10"),
            place = 1,
            baseIncome = 1000,
            tipCount = 1,
            tipIncome = 100,
            otherIncome = 0,
            totalIncome = 1100
        )
        val response = client.post("/users/1/results") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        coVerify { recordGameResultUseCase(any()) }
    }

    @Test
    fun `put result updates`() = testApplication {
        coEvery { editGameResultUseCase(any(), any()) } returns TestFixtures.gameResult()
        installRoutes()
        val request = UpsertGameResultRequest(
            gameType = com.example.domain.model.GameType.SANMA,
            playedAt = LocalDate.parse("2025-01-15"),
            place = 2,
            baseIncome = 900,
            tipCount = 0,
            tipIncome = 0,
            otherIncome = 0,
            totalIncome = 900
        )
        val response = client.put("/users/1/results/10") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { editGameResultUseCase(any(), any()) }
    }

    @Test
    fun `patch result updates partial`() = testApplication {
        coEvery { patchGameResultUseCase(any(), any()) } returns TestFixtures.gameResult().copy(place = 3)
        installRoutes()
        val response = client.patch("/users/1/results/10") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PatchGameResultRequest(place = 3)))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { patchGameResultUseCase(any(), any()) }
    }

    @Test
    fun `delete result removes`() = testApplication {
        coEvery { deleteGameResultUseCase(10, any()) } returns true
        installRoutes()
        val response = client.delete("/users/1/results/10") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `patch result with mismatched token returns 403`() = testApplication {
        installRoutes()
        val response = client.patch("/users/1/results/10") {
            withAuth(userId = 2)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PatchGameResultRequest(place = 2)))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        val error = json.decodeFromString<ErrorResponse>(response.bodyAsText())
        assertEquals("FORBIDDEN", error.errorCode)
    }
}

