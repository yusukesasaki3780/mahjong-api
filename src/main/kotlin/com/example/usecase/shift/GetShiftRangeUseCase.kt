package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - 任意の開始日と終了日でシフト一覧を取得するユースケースです。
 * - 週次表示やカスタムレンジ向けに利用します。
 */

import com.example.domain.model.Shift
import com.example.domain.repository.ShiftRepository
import kotlinx.datetime.LocalDate

class GetShiftRangeUseCase(
    private val repository: ShiftRepository
) {
    suspend operator fun invoke(userId: Long, startDate: LocalDate, endDate: LocalDate): List<Shift> {
        require(!startDate.isAfter(endDate)) { "startDate must be before or equal to endDate" }
        return repository.getShiftsInRange(userId, startDate, endDate)
    }

    private fun LocalDate.isAfter(other: LocalDate): Boolean =
        this > other
}
