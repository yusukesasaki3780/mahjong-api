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
    private val repository: ShiftRepository
) {
    suspend operator fun invoke(userId: Long, workDate: LocalDate): List<Shift> =
        repository.getShiftsOnDate(userId, workDate)
}
