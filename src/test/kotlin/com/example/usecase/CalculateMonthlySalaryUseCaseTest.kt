package com.example.usecase

import com.example.domain.model.AdvancePayment
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.model.SpecialHourlyWage
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
    fun `calculates breakdown for regular shift without special allowance`() = runTest {
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
            updatedAt = Instant.parse("2025-01-01T20:00:00Z"),
            specialHourlyWage = null
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

        assertEquals(330, result.totalWorkMinutes)
        assertEquals(60, result.totalDayMinutes)
        assertEquals(270, result.totalNightMinutes)
        assertEquals(1200.0, result.baseWageTotal, 0.1)
        assertEquals(1500.0 * 4.5, result.nightExtraTotal, 0.1)
        assertEquals(0.0, result.specialAllowanceTotal, 0.0)
        assertEquals(0, result.specialAllowances.size)
        assertEquals(500, result.transportTotal)
        assertEquals(5000, result.gameIncomeTotal)
        assertEquals(2000.0, result.advanceAmount)
        assertEquals(result.baseWageTotal + result.nightExtraTotal + 500 + 5000, result.grossSalary, 0.1)
        assertEquals((result.baseWageTotal + result.nightExtraTotal + 5000) * 0.1, result.incomeTax, 0.1)
        val expectedNet = (result.grossSalary - result.incomeTax) - result.advanceAmount
        assertEquals(expectedNet, result.netSalary, 0.1)
    }

    @Test
    fun `uses special hourly wage when shift has allowance`() = runTest {
        val yearMonth = YearMonth.of(2025, 2)
        val start = Instant.parse("2025-02-05T09:00:00Z")
        val end = Instant.parse("2025-02-05T13:00:00Z")
        val special = SpecialHourlyWage(
            id = 99,
            userId = 1,
            label = "年末年始特別",
            hourlyWage = 2000,
            createdAt = start,
            updatedAt = start
        )
        val shift = Shift(
            id = 1,
            userId = 1,
            workDate = LocalDate(2025, 2, 5),
            startTime = start,
            endTime = end,
            memo = null,
            specialHourlyWage = special,
            breaks = emptyList(),
            createdAt = start,
            updatedAt = start
        )
        coEvery { shiftRepository.getMonthlyShifts(1, yearMonth) } returns listOf(shift)
        coEvery { gameResultRepository.getTotalIncome(any(), any()) } returns 0
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
            incomeTaxRate = null,
            transportPerShift = null,
            createdAt = start,
            updatedAt = start
        )
        coEvery { advancePaymentRepository.findByUserIdAndYearMonth(1, yearMonth) } returns null

        val result = useCase(1, yearMonth)

        assertEquals(240, result.totalWorkMinutes)
        assertEquals(240, result.totalDayMinutes)
        assertEquals(0, result.totalNightMinutes)
        assertEquals(0.0, result.baseWageTotal, 0.0)
        assertEquals(0.0, result.nightExtraTotal, 0.0)
        assertEquals(8000.0, result.specialAllowanceTotal, 0.1)
        val allowance = result.specialAllowances.single()
        assertEquals("special_hourly_wage", allowance.type)
        assertEquals(2000, allowance.unitPrice)
        assertEquals(4.0, allowance.hours, 0.01)
        assertEquals(allowance.amount, result.specialAllowanceTotal, 0.1)
        assertEquals(result.specialAllowanceTotal, result.grossSalary, 0.1)
        assertEquals(result.grossSalary, result.netSalary, 0.1)
    }

    @Test
    fun `adds night bonus only for special shift night hours`() = runTest {
        val yearMonth = YearMonth.of(2025, 3)
        val start = Instant.parse("2025-03-10T21:00:00Z")
        val end = Instant.parse("2025-03-11T02:00:00Z")
        val special = SpecialHourlyWage(
            id = 101,
            userId = 1,
            label = "繁忙期",
            hourlyWage = 2000,
            createdAt = start,
            updatedAt = start
        )
        val shift = Shift(
            id = 10,
            userId = 1,
            workDate = LocalDate(2025, 3, 10),
            startTime = start,
            endTime = end,
            memo = null,
            specialHourlyWage = special,
            breaks = emptyList(),
            createdAt = start,
            updatedAt = start
        )
        coEvery { shiftRepository.getMonthlyShifts(1, yearMonth) } returns listOf(shift)
        coEvery { gameResultRepository.getTotalIncome(any(), any()) } returns 0
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
            incomeTaxRate = null,
            transportPerShift = null,
            createdAt = start,
            updatedAt = start
        )
        coEvery { advancePaymentRepository.findByUserIdAndYearMonth(1, yearMonth) } returns null

        val result = useCase(1, yearMonth)

        assertEquals(300, result.totalWorkMinutes)
        assertEquals(60, result.totalDayMinutes)
        assertEquals(240, result.totalNightMinutes)
        assertEquals(0.0, result.baseWageTotal, 0.0)
        assertEquals(0.0, result.nightExtraTotal, 0.0)
        assertEquals(2, result.specialAllowances.size)
        val baseAllowance = result.specialAllowances.first { it.type == "special_hourly_wage" }
        val nightBonus = result.specialAllowances.first { it.type == "night_bonus" }
        assertEquals(1.0, baseAllowance.hours, 0.01)
        assertEquals(2000, baseAllowance.unitPrice)
        assertEquals(2000.0, baseAllowance.amount, 0.1)
        assertEquals(4.0, nightBonus.hours, 0.01)
        assertEquals(2500, nightBonus.unitPrice)
        assertEquals(10000.0, nightBonus.amount, 0.1)
        assertEquals(12000.0, result.specialAllowanceTotal, 0.1)
    }
}
