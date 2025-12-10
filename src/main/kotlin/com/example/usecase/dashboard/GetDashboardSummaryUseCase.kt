package com.example.usecase.dashboard

/**
 * ### このファイルの役割
 * - ダッシュボード画面のサマリー値（勤務時間・給与内訳・ゲーム統計）を一括で集計するユースケースです。
 * - シフト情報、給与計算、ゲーム成績を横断的に呼び出し、返り値にまとまった DTO を構築します。
 */

import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.repository.ShiftRepository
import com.example.usecase.common.toStatsRange
import com.example.usecase.game.GetUserStatsUseCase
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import com.example.usecase.shift.ShiftTimeCalculator
import java.time.YearMonth
import kotlinx.datetime.TimeZone

/**
 * ダッシュボード用サマリーを返すユースケース。
 */
class GetDashboardSummaryUseCase(
    private val shiftRepository: ShiftRepository,
    private val calculateMonthlySalaryUseCase: CalculateMonthlySalaryUseCase,
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    data class Result(
        val userId: Long,
        val yearMonth: YearMonth,
        val work: WorkSummary,
        val salary: SalarySummary,
        val game: GameSummary
    )

    data class WorkSummary(
        val totalWorkMinutes: Long,
        val totalDayMinutes: Long,
        val totalNightMinutes: Long,
        val totalShifts: Int
    )

    data class SalarySummary(
        val baseWageTotal: Double,
        val nightExtraTotal: Double,
        val specialAllowanceTotal: Double,
        val specialAllowances: List<SpecialAllowanceSummary>,
        val transportTotal: Long,
        val gameIncomeTotal: Long,
        val advanceAmount: Double,
        val grossSalary: Double,
        val netSalary: Double,
        val incomeTax: Double
    )

    data class SpecialAllowanceSummary(
        val type: String,
        val label: String,
        val unitPrice: Int,
        val hours: Double,
        val amount: Double,
        val specialHourlyWageId: Long? = null
    )

    data class GameSummary(
        val totalGames: Int,
        val yonma: GameTypeSummary,
        val sanma: GameTypeSummary
    )

    data class GameTypeSummary(
        val games: Int,
        val avgPlace: Double?,
        val totalIncome: Long
    )

    suspend operator fun invoke(userId: Long, yearMonth: YearMonth): Result {
        val shifts = shiftRepository.getMonthlyShifts(userId, yearMonth)
        val minutes = ShiftTimeCalculator.calculateMinutes(shifts, timeZone)
        val salary = calculateMonthlySalaryUseCase(userId, yearMonth)

        val statsRange = yearMonth.toStatsRange(timeZone)
        val stats = getUserStatsUseCase(
            GetUserStatsUseCase.Command(
                userId = userId,
                range = statsRange
            )
        )

        val yonma = buildGameTypeSummary(GameType.YONMA, stats.results)
        val sanma = buildGameTypeSummary(GameType.SANMA, stats.results)

        return Result(
            userId = userId,
            yearMonth = yearMonth,
            work = WorkSummary(
                totalWorkMinutes = minutes.totalMinutes,
                totalDayMinutes = minutes.dayMinutes,
                totalNightMinutes = minutes.nightMinutes,
                totalShifts = shifts.size
            ),
            salary = SalarySummary(
                baseWageTotal = salary.baseWageTotal,
                nightExtraTotal = salary.nightExtraTotal,
                specialAllowanceTotal = salary.specialAllowanceTotal,
                specialAllowances = salary.specialAllowances.map {
                    SpecialAllowanceSummary(
                        type = it.type,
                        label = it.label,
                        unitPrice = it.unitPrice,
                        hours = it.hours,
                        amount = it.amount,
                        specialHourlyWageId = it.specialHourlyWageId
                    )
                },
                transportTotal = salary.transportTotal,
                gameIncomeTotal = salary.gameIncomeTotal,
                advanceAmount = salary.advanceAmount,
                grossSalary = salary.grossSalary,
                netSalary = salary.netSalary,
                incomeTax = salary.incomeTax
            ),
            game = GameSummary(
                totalGames = stats.totalGames,
                yonma = yonma,
                sanma = sanma
            )
        )
    }

    private fun buildGameTypeSummary(
        type: GameType,
        results: List<GameResult>
    ): GameTypeSummary {
        val filtered = results.filter { it.gameType == type }
        val avgPlace = if (filtered.isEmpty()) {
            null
        } else {
            filtered.map { it.place }.average()
        }
        val income = filtered.sumOf { it.totalIncome }
        return GameTypeSummary(
            games = filtered.size,
            avgPlace = avgPlace,
            totalIncome = income
        )
    }
}

