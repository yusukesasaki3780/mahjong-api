package com.example.usecase

import com.example.domain.model.AdvancePayment
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.model.WageType
import com.example.domain.repository.AdvancePaymentRepository
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import com.example.domain.repository.ShiftRepository
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalculateMonthlySalaryUseCaseTest {

    private val shiftRepository = mockk<ShiftRepository>()
    private val settingsRepository = mockk<GameSettingsRepository>()
    private val gameResultRepository = mockk<GameResultRepository>()
    private val advancePaymentRepository = mockk<AdvancePaymentRepository>()
    private val useCase = CalculateMonthlySalaryUseCase(
        shiftRepository = shiftRepository,
        settingsRepository = settingsRepository,
        gameResultRepository = gameResultRepository,
        advancePaymentRepository = advancePaymentRepository,
        timeZone = TimeZone.UTC
    )

    @Test
    fun `calculates breakdown including night extra`() = runTest {
        val yearMonth = YearMonth.of(2025, 1)
        val shift = Shift(
            id = 1,
            userId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = Instant.parse("2025-01-01T21:00:00Z"),
            endTime = Instant.parse("2025-01-02T03:00:00Z"),
            memo = null,
            breaks = listOf(
                ShiftBreak(
                    id = 1,
                    shiftId = 1,
                    breakStart = Instant.parse("2025-01-01T23:00:00Z"),
                    breakEnd = Instant.parse("2025-01-01T23:30:00Z")
                )
            ),
            createdAt = Instant.parse("2025-01-01T20:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T20:00:00Z")
        )
        coEvery { shiftRepository.getMonthlyShifts(1, yearMonth) } returns listOf(shift)
        coEvery { gameResultRepository.getTotalIncome(any(), any()) } returns 5000
        coEvery { settingsRepository.getSettings(1) } returns com.example.domain.model.GameSettings(
            userId = 1,
            yonmaGameFee = 400,
            sanmaGameFee = 250,
            sanmaGameFeeBack = 0,
            yonmaTipUnit = 100,
            sanmaTipUnit = 50,
            wageType = WageType.HOURLY,
            hourlyWage = 1200,
            fixedSalary = 0,
            nightRateMultiplier = 1.25,
            baseMinWage = 1200,
            incomeTaxRate = 0.1,
            transportPerShift = 500,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T00:00:00Z")
        )
        coEvery { advancePaymentRepository.findByUserIdAndYearMonth(1, yearMonth) } returns
            AdvancePayment(
                userId = 1,
                yearMonth = yearMonth,
                amount = 2000.0,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T00:00:00Z")
            )

        val result = useCase(1, yearMonth)

        assertEquals(330, result.totalWorkMinutes) // 6時間 - 30分休憩 = 5.5h => 330分
        assertEquals(60, result.totalDayMinutes) // 21:00-22:00
        assertEquals(270, result.totalNightMinutes) // 残りは深夜
        assertEquals(1200 * (5.5), result.baseWageTotal, 0.1)
        assertEquals(1200 * 0.25 * 4.5, result.nightExtraTotal, 0.1)
        assertEquals(500, result.transportTotal)
        assertEquals(5000, result.gameIncomeTotal)
        assertEquals(2000.0, result.advanceAmount)
        assertEquals(result.baseWageTotal + result.nightExtraTotal + 500 + 5000, result.grossSalary, 0.1)
        assertEquals((result.baseWageTotal + result.nightExtraTotal + 5000) * 0.1, result.incomeTax, 0.1)
        val expectedNet = (result.grossSalary - result.incomeTax) - result.advanceAmount
        assertEquals(expectedNet, result.netSalary, 0.1)
    }
}
