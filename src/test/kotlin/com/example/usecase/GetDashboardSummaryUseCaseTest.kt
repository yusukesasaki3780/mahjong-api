package com.example.usecase

import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.model.StatsRange
import com.example.domain.repository.ShiftRepository
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import com.example.usecase.game.GetUserStatsUseCase
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

class GetDashboardSummaryUseCaseTest {

    private val shiftRepository = mockk<ShiftRepository>()
    private val calculateMonthlySalaryUseCase = mockk<CalculateMonthlySalaryUseCase>()
    private val getUserStatsUseCase = mockk<GetUserStatsUseCase>()
    private val useCase = GetDashboardSummaryUseCase(
        shiftRepository = shiftRepository,
        calculateMonthlySalaryUseCase = calculateMonthlySalaryUseCase,
        getUserStatsUseCase = getUserStatsUseCase,
        timeZone = TimeZone.UTC
    )

    @Test
    fun `aggregates work salary and stats`() = runTest {
        val yearMonth = YearMonth.of(2025, 1)
        val shift = Shift(
            id = 1,
            userId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = Instant.parse("2025-01-01T05:00:00Z"),
            endTime = Instant.parse("2025-01-01T09:00:00Z"),
            memo = null,
            breaks = listOf(
                ShiftBreak(
                    id = 1,
                    shiftId = 1,
                    breakStart = Instant.parse("2025-01-01T07:00:00Z"),
                    breakEnd = Instant.parse("2025-01-01T07:15:00Z")
                )
            ),
            createdAt = Instant.parse("2025-01-01T04:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T04:00:00Z")
        )
        coEvery { shiftRepository.getMonthlyShifts(1, yearMonth) } returns listOf(shift)
        coEvery { calculateMonthlySalaryUseCase(1, yearMonth) } returns
            CalculateMonthlySalaryUseCase.Result(
                userId = 1,
                yearMonth = yearMonth,
                totalWorkMinutes = 225,
                totalDayMinutes = 225,
                totalNightMinutes = 0,
                baseWageTotal = 1200.0,
                nightExtraTotal = 0.0,
                specialAllowanceTotal = 0.0,
                specialAllowances = emptyList(),
                transportTotal = 500,
                gameIncomeTotal = 2000,
                advanceAmount = 0.0,
                grossSalary = 3700.0,
                incomeTax = 100.0,
                netSalary = 3600.0
            )
        coEvery { getUserStatsUseCase(any()) } returns GetUserStatsUseCase.Result(
            userId = 1,
            range = StatsRange(
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z")
            ),
            averagePlace = 2.0,
            totalGames = 3,
            totalIncome = 3000,
            results = listOf(
                GameResult(
                    id = 1,
                    userId = 1,
                    gameType = GameType.SANMA,
                    playedAt = Instant.parse("2025-01-01T10:00:00Z"),
                    place = 1,
                    baseIncome = 500,
                    tipCount = 1,
                    tipIncome = 100,
                    otherIncome = 0,
                    totalIncome = 600,
                    createdAt = Instant.parse("2025-01-01T10:10:00Z"),
                    updatedAt = Instant.parse("2025-01-01T10:10:00Z")
                ),
                GameResult(
                    id = 2,
                    userId = 1,
                    gameType = GameType.YONMA,
                    playedAt = Instant.parse("2025-01-02T10:00:00Z"),
                    place = 3,
                    baseIncome = 400,
                    tipCount = 0,
                    tipIncome = 0,
                    otherIncome = 0,
                    totalIncome = 400,
                    createdAt = Instant.parse("2025-01-02T10:10:00Z"),
                    updatedAt = Instant.parse("2025-01-02T10:10:00Z")
                )
            )
        )

        val result = useCase(1, yearMonth)

        assertEquals(1, result.work.totalShifts)
        assertEquals(225, result.work.totalDayMinutes)
        assertEquals(500, result.salary.transportTotal)
        assertEquals(3, result.game.totalGames)
        assertEquals(1, result.game.sanma.games)
    }
}
