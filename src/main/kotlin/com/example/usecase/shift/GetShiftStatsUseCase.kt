package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - シフト情報から月次の勤務統計（総勤務分数・勤務日数・日中/夜間シフト数）を算出するユースケースです。
 * - 画面の勤務サマリー表示に必要な軽量データのみを返し、給与計算よりも簡易な統計を提供します。
 */

import com.example.domain.repository.ShiftRepository
import java.time.YearMonth
import kotlinx.datetime.TimeZone

class GetShiftStatsUseCase(
    private val shiftRepository: ShiftRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val contextProvider: ShiftContextProvider,
    private val permissionService: ShiftPermissionService
    ) {

    data class Result(
        val userId: Long,
        val yearMonth: YearMonth,
        val totalMinutes: Long,
        val nightMinutes: Long,
        val workDays: Int,
        val shiftCount: Int
    )

    suspend operator fun invoke(actorId: Long, targetUserId: Long, yearMonth: YearMonth): Result {
        val context = contextProvider.forUserView(actorId, targetUserId)
        permissionService.ensureCanView(context)
        val userId = context.primaryUser?.id ?: error("Target user missing id.")
        val shifts = shiftRepository.getMonthlyShifts(userId, yearMonth)
        val minutes = ShiftTimeCalculator.calculateMinutes(shifts, timeZone)
        val workDays = shifts.map { it.workDate }.toSet().size
        val shiftCount = shifts.size

        return Result(
            userId = userId,
            yearMonth = yearMonth,
            totalMinutes = minutes.totalMinutes,
            nightMinutes = minutes.nightMinutes,
            workDays = workDays,
            shiftCount = shiftCount
        )
    }
}
