package com.example.usecase

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.domain.model.GameType
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import com.example.usecase.game.RecordGameResultUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RecordGameResultUseCaseTest {

    private val repository = mockk<GameResultRepository>()
    private val settingsRepository = mockk<GameSettingsRepository>()
    private val useCase = RecordGameResultUseCase(repository, settingsRepository)

    @Test
    fun `persists result when command is valid`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()
        coEvery { repository.insertGameResult(any()) } returns TestFixtures.gameResult().copy(totalIncome = 900)

        val command = RecordGameResultUseCase.Command(
            userId = 1,
            gameType = GameType.SANMA,
            playedAt = LocalDate(2025, 1, 1),
            place = 2,
            baseIncome = 800,
            tipCount = 2,
            tipIncome = 100,
            otherIncome = 0,
            totalIncome = 900
        )

        val result = useCase(command)

        assertEquals(900, result.totalIncome)
        coVerify { repository.insertGameResult(any()) }
    }

    @Test
    fun `warns but still stores when tip income is inconsistent`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()
        val stored = TestFixtures.gameResult().copy(totalIncome = 920)
        coEvery { repository.insertGameResult(any()) } returns stored

        val command = RecordGameResultUseCase.Command(
            userId = 1,
            gameType = GameType.SANMA,
            playedAt = LocalDate(2025, 1, 1),
            place = 2,
            baseIncome = 800,
            tipCount = 2,
            tipIncome = 120,
            otherIncome = 0,
            totalIncome = 920
        )

        val result = useCase(command)

        assertEquals(920, result.totalIncome)
        coVerify { repository.insertGameResult(any()) }
    }

    @Test
    fun `throws when total income does not match expected`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()

        val command = RecordGameResultUseCase.Command(
            userId = 1,
            gameType = GameType.YONMA,
            playedAt = LocalDate(2025, 1, 1),
            place = 2,
            baseIncome = 800,
            tipCount = 1,
            tipIncome = 100,
            otherIncome = 0,
            totalIncome = 950
        )

        assertFailsWith<DomainValidationException> { useCase(command) }
    }
}
