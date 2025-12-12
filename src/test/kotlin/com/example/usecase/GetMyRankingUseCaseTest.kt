package com.example.usecase

import com.example.TestFixtures
import com.example.domain.model.GameType
import com.example.domain.model.StatsPeriod
import com.example.domain.repository.UserRepository
import com.example.usecase.game.GetMyRankingUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetMyRankingUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val useCase = GetMyRankingUseCase(userRepository)

    private val period = StatsPeriod(
        name = "daily",
        start = Instant.parse("2025-01-01T00:00:00Z"),
        end = Instant.parse("2025-01-02T00:00:00Z")
    )

    @Test
    fun `returns rank when entry exists`() = runTest {
        val user = TestFixtures.user(id = 10)
        val ranking = listOf(
            TestFixtures.rankingEntry(userId = 5),
            TestFixtures.rankingEntry(userId = 10, name = user.nickname)
        )
        coEvery { userRepository.findById(10) } returns user
        coEvery { userRepository.findRanking(GameType.YONMA, period) } returns ranking

        val result = useCase(
            GetMyRankingUseCase.Command(
                userId = 10,
                gameType = GameType.YONMA,
                period = period
            )
        )

        assertEquals(2, result.rank)
        assertEquals(2, result.totalPlayers)
        assertEquals(ranking[1].totalIncome, result.totalProfit)
        assertEquals(user.nickname, result.user.nickname)
    }

    @Test
    fun `returns null rank when entry missing`() = runTest {
        val user = TestFixtures.user(id = 20)
        coEvery { userRepository.findById(20) } returns user
        coEvery { userRepository.findRanking(GameType.SANMA, period) } returns emptyList()

        val result = useCase(
            GetMyRankingUseCase.Command(
                userId = 20,
                gameType = GameType.SANMA,
                period = period
            )
        )

        assertEquals(null, result.rank)
        assertEquals(0, result.totalPlayers)
        assertEquals(0, result.totalProfit)
        assertEquals(0, result.gameCount)
    }
}
