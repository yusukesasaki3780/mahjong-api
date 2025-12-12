package com.example.presentation.dto

import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import kotlinx.serialization.Serializable

/**
 * 給与計算結果を返す DTO。
 */
@Serializable
data class SalaryResponse(
    val userId: Long,
    val yearMonth: String,
    val totalWorkMinutes: Long,
    val totalDayMinutes: Long,
    val totalNightMinutes: Long,
    val baseWageTotal: Double,
    val nightExtraTotal: Double,
    val specialAllowanceTotal: Double,
    val specialAllowance: SpecialAllowanceAggregateResponse,
    val specialAllowanceBreakdown: List<SpecialAllowanceBreakdownResponse>,
    val gameIncomeTotal: Long,
    val transportTotal: Long,
    val advanceAmount: Double,
    val grossSalary: Double,
    val incomeTax: Double,
    val netSalary: Double
) {
    companion object {
        fun from(result: CalculateMonthlySalaryUseCase.Result) = SalaryResponse(
            userId = result.userId,
            yearMonth = result.yearMonth.toString(),
            totalWorkMinutes = result.totalWorkMinutes,
            totalDayMinutes = result.totalDayMinutes,
            totalNightMinutes = result.totalNightMinutes,
            baseWageTotal = result.baseWageTotal,
            nightExtraTotal = result.nightExtraTotal,
            specialAllowanceTotal = result.specialAllowanceTotal,
            specialAllowance = SpecialAllowanceAggregateResponse(
                regular = result.specialAllowanceRegularTotal,
                lateNight = result.specialAllowanceLateNightTotal,
                total = result.specialAllowanceTotal
            ),
            specialAllowanceBreakdown = result.specialAllowances.map {
                SpecialAllowanceBreakdownResponse(
                    type = it.type,
                    label = it.label,
                    unit = it.unitPrice,
                    rate = it.rate,
                    hours = it.hours,
                    amount = it.amount,
                    specialHourlyWageId = it.specialHourlyWageId
                )
            },
            gameIncomeTotal = result.gameIncomeTotal,
            transportTotal = result.transportTotal,
            advanceAmount = result.advanceAmount,
            grossSalary = result.grossSalary,
            incomeTax = result.incomeTax,
            netSalary = result.netSalary
        )
    }
}

@Serializable
    data class SpecialAllowanceAggregateResponse(
        val regular: Double,
        val lateNight: Double,
        val total: Double
    )

@Serializable
data class SpecialAllowanceBreakdownResponse(
    val type: String,
    val label: String,
    val unit: Double,
    val rate: Double? = null,
    val hours: Double,
    val amount: Double,
    val specialHourlyWageId: Long? = null
)
