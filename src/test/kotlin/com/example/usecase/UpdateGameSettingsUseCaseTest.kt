package com.example.usecase

import com.example.common.error.DomainValidationException
import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import com.example.domain.repository.GameSettingsRepository
import com.example.usecase.settings.UpdateGameSettingsUseCase
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

class UpdateGameSettingsUseCaseTest {

    private val repository = mockk<GameSettingsRepository>()
    private val useCase = UpdateGameSettingsUseCase(repository, TestAuditSupport.auditLogger)

    @Test
    fun `updates settings when values valid`() = runTest {
        val now = Clock.System.now()
        val existing = sampleSettings().copy(createdAt = now, updatedAt = now)
        coEvery { repository.getSettings(1) } returns existing
        coEvery { repository.updateSettings(eq(1), any()) } returns existing.copy(yonmaGameFee = 500)

        val result = useCase(sampleCommand().copy(yonmaGameFee = 500), TestAuditSupport.auditContext)

        assertEquals(500, result.yonmaGameFee)
        coVerify { repository.updateSettings(eq(1), any()) }
    }

    @Test
    fun `invalid tax rate throws`() = runTest {
        coEvery { repository.getSettings(1) } returns sampleSettings()

        assertFailsWith<ConstraintViolationException> {
            useCase(sampleCommand().copy(incomeTaxRate = 1.5), TestAuditSupport.auditContext)
        }
    }

    @Test
    fun `hourly wage allows null fixed salary`() = runTest {
        val now = Clock.System.now()
        val existing = sampleSettings().copy(createdAt = now, updatedAt = now)
        coEvery { repository.getSettings(1) } returns existing
        coEvery { repository.updateSettings(eq(1), any()) } returns existing.copy(hourlyWage = 1300)

        val result = useCase(
            sampleCommand().copy(fixedSalary = null, hourlyWage = 1300),
            TestAuditSupport.auditContext
        )

        assertEquals(1300, result.hourlyWage)
        coVerify { repository.updateSettings(eq(1), any()) }
    }

    @Test
    fun `fixed wage requires fixed salary`() = runTest {
        coEvery { repository.getSettings(1) } returns sampleSettings()

        val exception = assertFailsWith<DomainValidationException> {
            useCase(
                sampleCommand().copy(wageType = WageType.FIXED, fixedSalary = null),
                TestAuditSupport.auditContext
            )
        }

        assertEquals("fixedWage is required when wageType is FIXED", exception.message)
    }

    private fun sampleCommand() = UpdateGameSettingsUseCase.Command(
        userId = 1,
        yonmaGameFee = 400,
        sanmaGameFee = 250,
        sanmaGameFeeBack = 0,
        yonmaTipUnit = 100,
        sanmaTipUnit = 50,
        wageType = WageType.HOURLY,
        hourlyWage = 1200,
        fixedSalary = 300000,
        nightRateMultiplier = 1.25,
        baseMinWage = 1200,
        incomeTaxRate = 0.1,
        transportPerShift = 500
    )

    private fun sampleSettings(): GameSettings {
        val now = Clock.System.now()
        return GameSettings(
            userId = 1,
            yonmaGameFee = 400,
            sanmaGameFee = 250,
            sanmaGameFeeBack = 0,
            yonmaTipUnit = 100,
            sanmaTipUnit = 50,
            wageType = WageType.HOURLY,
            hourlyWage = 1200,
            fixedSalary = 300000,
            nightRateMultiplier = 1.25,
            baseMinWage = 1200,
            incomeTaxRate = 0.1,
            transportPerShift = 500,
            createdAt = now,
            updatedAt = now
        )
    }
}
