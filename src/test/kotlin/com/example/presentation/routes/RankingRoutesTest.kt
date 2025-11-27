package com.example.presentation.routes

import com.example.TestFixtures
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.usecase.game.GetRankingUseCase
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.slot
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RankingRoutesTest : RoutesTestBase() {

    @Test
    fun `ranking returns list`() = testApplication {
        coEvery { getRankingUseCase(any<GetRankingUseCase.Command>()) } returns listOf(TestFixtures.rankingEntry())
        installRoutes()
        val response = client.get("/ranking?type=SANMA&start=2025-01-01T00:00:00Z&end=2025-01-31T00:00:00Z") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `ranking supports predefined range`() = testApplication {
        val commandSlot = slot<GetRankingUseCase.Command>()
        coEvery { getRankingUseCase(capture(commandSlot)) } returns listOf(TestFixtures.rankingEntry())
        installRoutes()

        val response = client.get("/ranking?type=SANMA&range=daily") {
            withAuth()
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("daily", commandSlot.captured.period.name)
    }

    @Test
    fun `invalid type returns 400`() = testApplication {
        installRoutes()
        val response = client.get("/ranking?type=INVALID&start=2025-01-01T00:00:00Z&end=2025-01-31T00:00:00Z") {
            withAuth()
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("type", error.errors.first().field)
    }

    @Test
    fun `invalid range returns 400`() = testApplication {
        installRoutes()

        val response = client.get("/ranking?type=SANMA&range=decade") {
            withAuth()
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("range", error.errors.first().field)
    }
}
