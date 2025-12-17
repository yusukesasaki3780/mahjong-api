package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - 特定日付のシフト一覧を取得するための薄いユースケースです。
 * - Route 層から直接リポジトリへアクセスさせず、今後ロジックを足しやすくします。
 */

import com.example.domain.model.Shift
import com.example.domain.repository.ShiftRepository
import kotlinx.datetime.LocalDate

class GetDailyShiftUseCase(
    private val repository: ShiftRepository,
    private val contextProvider: ShiftContextProvider,
    private val permissionService: ShiftPermissionService
) {
    suspend operator fun invoke(actorId: Long, targetUserId: Long, workDate: LocalDate): List<Shift> {
        val context = contextProvider.forUserView(actorId, targetUserId)
        permissionService.ensureCanView(context)
        val userId = context.primaryUser?.id ?: error("Target user missing id.")
        return repository.getShiftsOnDate(userId, workDate)
    }
}
