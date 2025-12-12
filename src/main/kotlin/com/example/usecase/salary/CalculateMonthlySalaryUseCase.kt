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
        val specialAllowanceRegularTotal: Double,
        val specialAllowanceLateNightTotal: Double,
        val specialAllowanceTotal: Double,
        val specialAllowances: List<SpecialAllowanceSummary>,
        val transportTotal: Long,
        val gameIncomeTotal: Long,
        val advanceAmount: Double,
        val grossSalary: Double,
        val incomeTax: Double,
        val netSalary: Double
    )

    data class SpecialAllowanceSummary(
        val type: String,
        val label: String,
        val unitPrice: Double,
        val rate: Double? = null,
        val hours: Double,
        val amount: Double,
        val specialHourlyWageId: Long? = null
    )

    suspend operator fun invoke(userId: Long, yearMonth: YearMonth): Result {
        val settings = settingsRepository.getSettings(userId)
            ?: throw IllegalStateException("Game settings not found for user $userId")
        val shifts = shiftRepository.getMonthlyShifts(userId, yearMonth)
        val (specialShifts, regularShifts) = shifts.partition { it.specialHourlyWage != null }
        val regularMinutes = ShiftTimeCalculator.calculateMinutes(regularShifts, timeZone)
        var specialDayMinutes = 0L
        var specialNightMinutes = 0L
        val allowanceAccumulators = mutableMapOf<Long, SpecialAllowanceAccumulator>()
        specialShifts.forEach { shift ->
            val special = shift.specialHourlyWage ?: return@forEach
            val minutes = ShiftTimeCalculator.calculateMinutesForShift(shift, timeZone)
            specialDayMinutes += minutes.dayMinutes
            specialNightMinutes += minutes.nightMinutes
            val accumulator = allowanceAccumulators.getOrPut(special.id) {
                SpecialAllowanceAccumulator(
                    label = special.label,
                    unitPrice = special.hourlyWage,
                    specialHourlyWageId = special.id
                )
            }
            accumulator.addMinutes(minutes)
        }
        val regularDayHours = regularMinutes.dayMinutes / 60.0
        val regularNightHours = regularMinutes.nightMinutes / 60.0
        val regularTotalHours = regularDayHours + regularNightHours
        val nightPremiumRate = resolveNightPremiumRate(settings.nightRateMultiplier)
        val specialAllowances = allowanceAccumulators.values
            .flatMap { it.toBreakdown(nightPremiumRate) }
            .sortedWith(
                compareBy(
                    { it.specialHourlyWageId ?: Long.MAX_VALUE },
                    { it.type },
                    { it.label }
                )
            )
        val specialAllowanceRegularTotal = specialAllowances
            .filter { it.type == SPECIAL_ALLOWANCE_TYPE_REGULAR }
            .sumOf { it.amount }
        val specialAllowanceLateNightTotal = specialAllowances
            .filter { it.type == SPECIAL_ALLOWANCE_TYPE_LATE_NIGHT }
            .sumOf { it.amount }
        val specialAllowanceTotal = specialAllowanceRegularTotal + specialAllowanceLateNightTotal
        val totalDayMinutes = regularMinutes.dayMinutes + specialDayMinutes
        val totalNightMinutes = regularMinutes.nightMinutes + specialNightMinutes
        val totalWorkMinutes = totalDayMinutes + totalNightMinutes

        val statsRange = yearMonth.toStatsRange(timeZone)
        val gameTotal = gameResultRepository.getTotalIncome(userId, statsRange)
        val transportTotal = settings.transportPerShift?.let { it.toLong() * shifts.size } ?: 0L
        val advanceAmount = advancePaymentRepository.findByUserIdAndYearMonth(userId, yearMonth)?.amount ?: 0.0

        val (baseWageTotal, nightExtraTotal) = when (settings.wageType) {
            WageType.HOURLY -> {
                val base = settings.hourlyWage * regularTotalHours
                val nightUnitPrice = settings.hourlyWage * nightPremiumRate
                val nightExtra = nightUnitPrice * regularNightHours
                Pair(base, nightExtra)
            }

            WageType.FIXED -> {
                val fixed = settings.fixedSalary.toDouble()
                Pair(fixed, 0.0)
            }
        }

        val grossSalary = baseWageTotal + nightExtraTotal + specialAllowanceTotal + gameTotal + transportTotal
        val taxableBase = baseWageTotal + nightExtraTotal + specialAllowanceTotal + gameTotal
        val incomeTax = settings.incomeTaxRate?.let { taxableBase * it } ?: 0.0
        val netBeforeAdvance = grossSalary - incomeTax
        val finalSalary = netBeforeAdvance - advanceAmount

        return Result(
            userId = userId,
            yearMonth = yearMonth,
            totalWorkMinutes = totalWorkMinutes,
            totalDayMinutes = totalDayMinutes,
            totalNightMinutes = totalNightMinutes,
            baseWageTotal = baseWageTotal,
            nightExtraTotal = nightExtraTotal,
            specialAllowanceRegularTotal = specialAllowanceRegularTotal,
            specialAllowanceLateNightTotal = specialAllowanceLateNightTotal,
            specialAllowanceTotal = specialAllowanceTotal,
            specialAllowances = specialAllowances,
            transportTotal = transportTotal,
            gameIncomeTotal = gameTotal,
            advanceAmount = advanceAmount,
            grossSalary = grossSalary,
            incomeTax = incomeTax,
            netSalary = finalSalary
        )
    }

    private data class SpecialAllowanceAccumulator(
        val label: String,
        val unitPrice: Int,
        val specialHourlyWageId: Long,
        var dayMinutes: Long = 0,
        var nightMinutes: Long = 0
    ) {
        fun addMinutes(minutes: ShiftTimeCalculator.Minutes) {
            dayMinutes += minutes.dayMinutes
            nightMinutes += minutes.nightMinutes
        }

        fun toBreakdown(nightPremiumRate: Double): List<SpecialAllowanceSummary> {
            val summaries = mutableListOf<SpecialAllowanceSummary>()
            val unit = unitPrice.toDouble()
            val totalMinutes = dayMinutes + nightMinutes
            val totalHours = totalMinutes / 60.0
            if (totalHours > 0) {
                summaries += SpecialAllowanceSummary(
                    type = SPECIAL_ALLOWANCE_TYPE_REGULAR,
                    label = label,
                    unitPrice = unit,
                    rate = null,
                    hours = totalHours,
                    amount = unit * totalHours,
                    specialHourlyWageId = specialHourlyWageId
                )
            }
            val nightHours = nightMinutes / 60.0
            if (nightHours > 0 && nightPremiumRate > 0.0) {
                summaries += SpecialAllowanceSummary(
                    type = SPECIAL_ALLOWANCE_TYPE_LATE_NIGHT,
                    label = "${label}（深夜）",
                    unitPrice = unit,
                    rate = nightPremiumRate,
                    hours = nightHours,
                    amount = unit * nightPremiumRate * nightHours,
                    specialHourlyWageId = specialHourlyWageId
                )
            }
            return summaries
        }
    }

    private fun resolveNightPremiumRate(multiplier: Double): Double =
        when {
            multiplier <= 0.0 -> 0.0
            multiplier > 1.0 -> multiplier - 1.0
            else -> multiplier
        }

    private companion object {
        const val SPECIAL_ALLOWANCE_TYPE_REGULAR = "special_regular"
        const val SPECIAL_ALLOWANCE_TYPE_LATE_NIGHT = "special_late_night"
    }
}

