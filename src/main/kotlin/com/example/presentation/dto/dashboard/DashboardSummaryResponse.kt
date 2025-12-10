package com.example.presentation.dto.dashboard

import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import kotlinx.serialization.Serializable

/**
 * ダッシュボードサマリーのレスポンス DTO。
 */
@Serializable
data class DashboardSummaryResponse(
    val yearMonth: String,
    val workTotalMinutes: Long,
    val workDayMinutes: Long,
    val workNightMinutes: Long,
    val workShiftCount: Int,
    val salaryBaseWageTotal: Double,
    val salaryNightExtraTotal: Double,
    val salarySpecialAllowanceTotal: Double,
    val salarySpecialAllowances: List<DashboardSpecialAllowanceResponse>,
    val salaryTransportTotal: Long,
    val salaryGameIncomeTotal: Long,
    val salaryAdvanceAmount: Double,
    val salaryGross: Double,
    val salaryNet: Double,
    val salaryIncomeTax: Double,
    val totalGames: Int,
    val yonmaGames: Int,
    val yonmaAvgPlace: Double?,
    val yonmaTotalIncome: Long,
    val sanmaGames: Int,
    val sanmaAvgPlace: Double?,
    val sanmaTotalIncome: Long
) {

    companion object {
        fun from(result: GetDashboardSummaryUseCase.Result): DashboardSummaryResponse {
            return DashboardSummaryResponse(
                yearMonth = result.yearMonth.toString(),
                workTotalMinutes = result.work.totalWorkMinutes,
                workDayMinutes = result.work.totalDayMinutes,
                workNightMinutes = result.work.totalNightMinutes,
                workShiftCount = result.work.totalShifts,
                salaryBaseWageTotal = result.salary.baseWageTotal,
                salaryNightExtraTotal = result.salary.nightExtraTotal,
                salarySpecialAllowanceTotal = result.salary.specialAllowanceTotal,
                salarySpecialAllowances = result.salary.specialAllowances.map {
                    DashboardSpecialAllowanceResponse(
                        type = it.type,
                        label = it.label,
                        unitPrice = it.unitPrice,
                        hours = it.hours,
                        amount = it.amount,
                        specialHourlyWageId = it.specialHourlyWageId
                    )
                },
                salaryTransportTotal = result.salary.transportTotal,
                salaryGameIncomeTotal = result.salary.gameIncomeTotal,
                salaryAdvanceAmount = result.salary.advanceAmount,
                salaryGross = result.salary.grossSalary,
                salaryNet = result.salary.netSalary,
                salaryIncomeTax = result.salary.incomeTax,
                totalGames = result.game.totalGames,
                yonmaGames = result.game.yonma.games,
                yonmaAvgPlace = result.game.yonma.avgPlace,
                yonmaTotalIncome = result.game.yonma.totalIncome,
                sanmaGames = result.game.sanma.games,
                sanmaAvgPlace = result.game.sanma.avgPlace,
                sanmaTotalIncome = result.game.sanma.totalIncome
            )
        }
    }
}

@Serializable
data class DashboardSpecialAllowanceResponse(
    val type: String,
    val label: String,
    val unitPrice: Int,
    val hours: Double,
    val amount: Double,
    val specialHourlyWageId: Long? = null
)
