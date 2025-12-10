package com.example.usecase

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.domain.model.GameType
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import com.example.usecase.game.EditGameResultUseCase
import com.example.usecase.TestAuditSupport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class EditGameResultUseCaseTest {

    private val repository = mockk<GameResultRepository>()
    private val settingsRepository = mockk<GameSettingsRepository>()
    private val useCase = EditGameResultUseCase(repository, settingsRepository, TestAuditSupport.auditLogger)

    @Test
    fun `updates result when command is valid`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()
        coEvery { repository.findById(10) } returns TestFixtures.gameResult()
        coEvery { repository.updateGameResult(any()) } returns TestFixtures.gameResult().copy(totalIncome = 900)

        val command = EditGameResultUseCase.Command(
            id = 10,
            userId = 1,
            gameType = GameType.SANMA,
            playedAt = LocalDate(2025, 1, 1),
            place = 2,
            baseIncome = 800,
            tipCount = 2,
            tipIncome = 100,
            otherIncome = 0,
            totalIncome = 900,
            createdAt = Clock.System.now()
        )

        val result = useCase(command, TestAuditSupport.auditContext)

        assertEquals(900, result.totalIncome)
        coVerify { repository.updateGameResult(any()) }
    }

    @Test
    fun `warns but still updates when tip income is inconsistent`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()
        coEvery { repository.findById(10) } returns TestFixtures.gameResult()
        val updated = TestFixtures.gameResult().copy(totalIncome = 850)
        coEvery { repository.updateGameResult(any()) } returns updated

        val command = EditGameResultUseCase.Command(
            id = 10,
            userId = 1,
            gameType = GameType.SANMA,
            playedAt = LocalDate(2025, 1, 1),
            place = 2,
            baseIncome = 800,
            tipCount = 2,
            tipIncome = 50,
            otherIncome = 0,
            totalIncome = 850,
            createdAt = Clock.System.now()
        )

        val result = useCase(command, TestAuditSupport.auditContext)

        assertEquals(850, result.totalIncome)
        coVerify { repository.updateGameResult(any()) }
    }

    @Test
    fun `throws when total income mismatches expected`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()
        coEvery { repository.findById(10) } returns TestFixtures.gameResult().copy(gameType = GameType.YONMA, place = 2)

        val command = EditGameResultUseCase.Command(
            id = 10,
            userId = 1,
            gameType = GameType.YONMA,
            playedAt = LocalDate(2025, 1, 1),
            place = 2,
            baseIncome = 800,
            tipCount = 1,
            tipIncome = 100,
            otherIncome = 0,
            totalIncome = 950,
            createdAt = Clock.System.now()
        )

        assertFailsWith<DomainValidationException> { useCase(command, TestAuditSupport.auditContext) }
    }
}
