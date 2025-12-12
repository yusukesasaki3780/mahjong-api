package com.example.presentation.routes

import com.example.TestFixtures
import com.example.presentation.dto.MyRankingResponse
import com.example.presentation.dto.RankingListResponse
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.usecase.game.GetMyRankingUseCase
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
    fun `my ranking returns summary`() = testApplication {
        val commandSlot = slot<GetMyRankingUseCase.Command>()
        coEvery { getMyRankingUseCase(capture(commandSlot)) } returns
            GetMyRankingUseCase.Result(
                rank = 2,
                totalPlayers = 5,
                averageRank = 1.88,
                totalProfit = -2000,
                gameCount = 8,
                user = GetMyRankingUseCase.Result.UserSummary(id = 100, nickname = "ゆっこテスト2")
            )
        installRoutes()

        val response = client.get("/ranking/me?mode=four&range=monthly&targetDate=2025-12") {
            withAuth(userId = 100)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<MyRankingResponse>(response.bodyAsText())
        assertEquals(2, body.rank)
        assertEquals(5, body.totalPlayers)
        assertEquals(100, body.user.id)
        assertEquals("monthly", commandSlot.captured.period.name)
    }

    @Test
    fun `my ranking invalid mode returns 400`() = testApplication {
        installRoutes()
        val response = client.get("/ranking/me?mode=invalid&range=daily") {
            withAuth()
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("mode", error.errors.first().field)
    }

    @Test
    fun `ranking returns list`() = testApplication {
        coEvery { getRankingUseCase(any<GetRankingUseCase.Command>()) } returns listOf(TestFixtures.rankingEntry(userId = 1))
        installRoutes()
        val response = client.get("/ranking?type=SANMA&start=2025-01-01T00:00:00Z&end=2025-01-31T00:00:00Z") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<RankingListResponse>(response.bodyAsText())
        assertEquals(1, body.myRank)
        assertEquals(1, body.totalPlayers)
        assertEquals(1, body.ranking.size)
        val expectedEntry = TestFixtures.rankingEntry(userId = 1)
        assertEquals(expectedEntry.gameCount, body.myStats?.games)
        assertEquals(expectedEntry.averagePlace, body.myStats?.averageRank)
    }

    @Test
    fun `ranking supports predefined range`() = testApplication {
        val commandSlot = slot<GetRankingUseCase.Command>()
        coEvery { getRankingUseCase(capture(commandSlot)) } returns listOf(TestFixtures.rankingEntry(userId = 1))
        installRoutes()

        val response = client.get("/ranking?type=SANMA&range=daily") {
            withAuth()
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<RankingListResponse>(response.bodyAsText())
        assertEquals("daily", commandSlot.captured.period.name)
        assertEquals(1, body.myRank)
    }

    @Test
    fun `ranking returns null stats when user not ranked`() = testApplication {
        coEvery { getRankingUseCase(any<GetRankingUseCase.Command>()) } returns listOf(TestFixtures.rankingEntry(userId = 99))
        installRoutes()

        val response = client.get("/ranking?type=SANMA&range=daily") {
            withAuth(userId = 1)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<RankingListResponse>(response.bodyAsText())
        assertEquals(null, body.myRank)
        assertEquals(null, body.myStats)
        assertEquals(1, body.totalPlayers)
    }

    @Test
    fun `my ranking accepts yyyy dash mm for weekly`() = testApplication {
        coEvery { getMyRankingUseCase(any()) } returns
            GetMyRankingUseCase.Result(
                rank = null,
                totalPlayers = 0,
                averageRank = null,
                totalProfit = 0,
                gameCount = 0,
                user = GetMyRankingUseCase.Result.UserSummary(id = 1, nickname = "me")
            )
        installRoutes()

        val response = client.get("/ranking/me?mode=three&range=weekly&targetDate=2025-12") {
            withAuth()
        }

        assertEquals(HttpStatusCode.OK, response.status)
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
