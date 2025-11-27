package com.example.usecase

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.domain.model.GameType
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import com.example.usecase.game.PatchGameResultUseCase
import com.example.usecase.TestAuditSupport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PatchGameResultUseCaseTest {

    private val repository = mockk<GameResultRepository>()
    private val settingsRepository = mockk<GameSettingsRepository>()
    private val useCase = PatchGameResultUseCase(repository, settingsRepository, TestAuditSupport.auditLogger)

    @Test
    fun `updates income fields when payload is consistent`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()
        val expected = TestFixtures.gameResult().copy(totalIncome = 900)
        coEvery { repository.findById(10) } returns TestFixtures.gameResult()
        coEvery { repository.patchGameResult(eq(1), eq(10), any()) } returns expected

        val result = useCase(
            PatchGameResultUseCase.Command(
                userId = 1,
                resultId = 10,
                gameType = GameType.SANMA,
                place = 2,
                baseIncome = 800,
                tipCount = 2,
                tipIncome = 100,
                totalIncome = 900
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(900, result.totalIncome)
        coVerify { repository.patchGameResult(eq(1), eq(10), any()) }
    }

    @Test
    fun `throws when income fields are partially supplied`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()

        coEvery { repository.findById(10) } returns TestFixtures.gameResult()

        assertFailsWith<DomainValidationException> {
            useCase(
                PatchGameResultUseCase.Command(
                    userId = 1,
                    resultId = 10,
                    totalIncome = 900
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `throws when total income mismatches expected formula`() = runTest {
        coEvery { settingsRepository.getSettings(1) } returns TestFixtures.gameSettings()
        coEvery { repository.findById(10) } returns TestFixtures.gameResult().copy(gameType = GameType.YONMA, place = 2)

        assertFailsWith<DomainValidationException> {
            useCase(
                PatchGameResultUseCase.Command(
                    userId = 1,
                    resultId = 10,
                    gameType = GameType.YONMA,
                    place = 2,
                    baseIncome = 800,
                    tipCount = 1,
                    tipIncome = 100,
                    totalIncome = 900
                ),
                TestAuditSupport.auditContext
            )
        }
    }
}
