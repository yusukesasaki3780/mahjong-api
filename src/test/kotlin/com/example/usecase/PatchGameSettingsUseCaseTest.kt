package com.example.usecase

import com.example.common.error.DomainValidationException
import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import com.example.domain.repository.GameSettingsRepository
import com.example.usecase.settings.PatchGameSettingsUseCase
import com.example.usecase.TestAuditSupport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import kotlin.test.assertFailsWith

class PatchGameSettingsUseCaseTest {

    private val repository = mockk<GameSettingsRepository>()
    private val useCase = PatchGameSettingsUseCase(repository, TestAuditSupport.auditLogger)

    @Test
    fun `patch updates selective fields`() = runTest {
        val existing = sampleSettings(yonma = 400, sanma = 250)
        val result = sampleSettings(yonma = 450, sanma = 260)
        coEvery { repository.getSettings(1) } returns existing
        coEvery { repository.patchSettings(eq(1), any()) } returns result

        val updated = useCase(
            PatchGameSettingsUseCase.Command(
                userId = 1,
                yonmaGameFee = 450,
                sanmaGameFee = 260
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(450, updated.yonmaGameFee)
        assertEquals(260, updated.sanmaGameFee)
        coVerify {
            repository.getSettings(1)
            repository.patchSettings(eq(1), any())
        }
    }

    @Test
    fun `negative fee is rejected`() = runTest {
        assertFailsWith<ConstraintViolationException> {
            useCase(
                PatchGameSettingsUseCase.Command(
                    userId = 1,
                    yonmaGameFee = -1
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `patch requires fixed salary when switching to fixed wage`() = runTest {
        val existing = sampleSettings(yonma = 400, sanma = 250, wageType = WageType.HOURLY, fixedSalary = 0)
        coEvery { repository.getSettings(1) } returns existing

        assertFailsWith<DomainValidationException> {
            useCase(
                PatchGameSettingsUseCase.Command(
                    userId = 1,
                    wageType = WageType.FIXED
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `patch allows fixed wage when salary provided`() = runTest {
        val existing = sampleSettings(yonma = 400, sanma = 250, wageType = WageType.HOURLY, fixedSalary = 0)
        val patched = existing.copy(wageType = WageType.FIXED, fixedSalary = 350000)
        coEvery { repository.getSettings(1) } returns existing
        coEvery { repository.patchSettings(eq(1), any()) } returns patched

        val result = useCase(
            PatchGameSettingsUseCase.Command(
                userId = 1,
                wageType = WageType.FIXED,
                fixedSalary = 350000
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(WageType.FIXED, result.wageType)
        assertEquals(350000, result.fixedSalary)
    }

    private fun sampleSettings(
        yonma: Int,
        sanma: Int,
        wageType: WageType = WageType.HOURLY,
        fixedSalary: Int = 300000
    ): GameSettings {
        val now = Clock.System.now()
        return GameSettings(
            userId = 1,
            yonmaGameFee = yonma,
            sanmaGameFee = sanma,
            sanmaGameFeeBack = 0,
            yonmaTipUnit = 100,
            sanmaTipUnit = 50,
            wageType = wageType,
            hourlyWage = 1200,
            fixedSalary = fixedSalary,
            nightRateMultiplier = 1.25,
            baseMinWage = 1200,
            incomeTaxRate = 0.1,
            transportPerShift = 500,
            createdAt = now,
            updatedAt = now
        )
    }
}
