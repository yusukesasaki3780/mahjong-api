package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - 任意の開始日と終了日でシフト一覧を取得するユースケースです。
 * - 週次表示やカスタムレンジ向けに利用します。
 */

import com.example.domain.model.Shift
import com.example.domain.repository.ShiftRepository
import kotlinx.datetime.LocalDate

/**
 * 任意の期間でシフト一覧を取得するユースケース。
 */
class GetShiftRangeUseCase(
    private val repository: ShiftRepository,
    private val contextProvider: ShiftContextProvider,
    private val permissionService: ShiftPermissionService
) {
    /**
     * 開始日〜終了日の検証と権限チェックを行い、対象ユーザーのシフトを取得する。
     */
    suspend operator fun invoke(actorId: Long, targetUserId: Long, startDate: LocalDate, endDate: LocalDate): List<Shift> {
        require(!startDate.isAfter(endDate)) { "startDate must be before or equal to endDate" }
        val context = contextProvider.forUserView(actorId, targetUserId)
        permissionService.ensureCanView(context)
        val userId = context.primaryUser?.id ?: error("Target user missing id.")
        return repository.getShiftsInRange(userId, startDate, endDate)
    }

    private fun LocalDate.isAfter(other: LocalDate): Boolean =
        this > other
}
