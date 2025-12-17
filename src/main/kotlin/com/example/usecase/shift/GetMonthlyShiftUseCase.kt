package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - ユーザーと年月をもとに、その月のシフト一覧を取得する薄いユースケースです。
 * - 画面層からは yearMonth だけを渡せばよい形にラップしています。
 */

import com.example.domain.model.Shift
import com.example.domain.repository.ShiftRepository
import java.time.YearMonth

/**
 * 指定年月のシフト一覧を取得するユースケース。
 */
class GetMonthlyShiftUseCase(
    private val repository: ShiftRepository,
    private val contextProvider: ShiftContextProvider,
    private val permissionService: ShiftPermissionService
) {

    suspend operator fun invoke(actorId: Long, targetUserId: Long, yearMonth: YearMonth): List<Shift> {
        val context = contextProvider.forUserView(actorId, targetUserId)
        permissionService.ensureCanView(context)
        val userId = context.primaryUser?.id ?: error("Target user missing id.")
        return repository.getMonthlyShifts(userId, yearMonth)
    }
}
