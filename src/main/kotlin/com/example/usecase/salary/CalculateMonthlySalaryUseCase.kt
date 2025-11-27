package com.example.usecase.salary

/**
 * ### このファイルの役割
 * - 月次給与を計算し、昼夜の勤務時間や税額などの内訳を同時に算出するユースケースです。
 * - Shift 情報を分解して日中/深夜の分数を求め、ゲーム収支や交通費を含めた最終的な支給額を計算します。
 */

import com.example.domain.model.WageType
import com.example.domain.repository.AdvancePaymentRepository
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import com.example.domain.repository.ShiftRepository
import com.example.usecase.common.toStatsRange
import com.example.usecase.shift.ShiftTimeCalculator
import java.time.YearMonth
import kotlinx.datetime.TimeZone

/**
 * 月次給与を算出するユースケース。
 */
class CalculateMonthlySalaryUseCase(
    private val shiftRepository: ShiftRepository,
    private val settingsRepository: GameSettingsRepository,
    private val gameResultRepository: GameResultRepository,
    private val advancePaymentRepository: AdvancePaymentRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    data class Result(
        val userId: Long,
        val yearMonth: YearMonth,
        val totalWorkMinutes: Long,
        val totalDayMinutes: Long,
        val totalNightMinutes: Long,
        val baseWageTotal: Double,
        val nightExtraTotal: Double,
        val transportTotal: Long,
        val gameIncomeTotal: Long,
        val advanceAmount: Double,
        val grossSalary: Double,
        val incomeTax: Double,
        val netSalary: Double
    )

    suspend operator fun invoke(userId: Long, yearMonth: YearMonth): Result {
        val settings = settingsRepository.getSettings(userId)
            ?: throw IllegalStateException("Game settings not found for user $userId")
        val shifts = shiftRepository.getMonthlyShifts(userId, yearMonth)
        val minutes = ShiftTimeCalculator.calculateMinutes(shifts, timeZone)
        val dayHours = minutes.dayMinutes / 60.0
        val nightHours = minutes.nightMinutes / 60.0

        val statsRange = yearMonth.toStatsRange(timeZone)
        val gameTotal = gameResultRepository.getTotalIncome(userId, statsRange)
        val transportTotal = settings.transportPerShift?.let { it.toLong() * shifts.size } ?: 0L
        val advanceAmount = advancePaymentRepository.findByUserIdAndYearMonth(userId, yearMonth)?.amount ?: 0.0

        val (baseWageTotal, nightExtraTotal, baseForTax) = when (settings.wageType) {
            WageType.HOURLY -> {
                val base = settings.hourlyWage * (dayHours + nightHours)
                val extraMultiplier = (settings.nightRateMultiplier - 1.0).coerceAtLeast(0.0)
                val nightExtra = settings.hourlyWage * extraMultiplier * nightHours
                Triple(base, nightExtra, base + nightExtra)
            }

            WageType.FIXED -> {
                val fixed = settings.fixedSalary.toDouble()
                Triple(fixed, 0.0, fixed)
            }
        }

        val grossSalary = baseForTax + gameTotal + transportTotal
        val incomeTax = settings.incomeTaxRate?.let { (baseForTax + gameTotal) * it } ?: 0.0
        val netBeforeAdvance = grossSalary - incomeTax
        val finalSalary = netBeforeAdvance - advanceAmount

        return Result(
            userId = userId,
            yearMonth = yearMonth,
            totalWorkMinutes = minutes.totalMinutes,
            totalDayMinutes = minutes.dayMinutes,
            totalNightMinutes = minutes.nightMinutes,
            baseWageTotal = baseWageTotal,
            nightExtraTotal = nightExtraTotal,
            transportTotal = transportTotal,
            gameIncomeTotal = gameTotal,
            advanceAmount = advanceAmount,
            grossSalary = grossSalary,
            incomeTax = incomeTax,
            netSalary = finalSalary
        )
    }
}

