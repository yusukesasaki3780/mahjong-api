package com.example.domain.repository

import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import java.time.YearMonth
import kotlinx.datetime.LocalDate

/**
 * シフトと休憩テーブルへのアクセスを抽象化する。
 */
interface ShiftRepository {

    suspend fun insertShift(shift: Shift): Shift

    suspend fun updateShift(shift: Shift): Shift

    suspend fun deleteShift(shiftId: Long): Boolean

    suspend fun getMonthlyShifts(userId: Long, yearMonth: YearMonth): List<Shift>
    suspend fun getShiftsOnDate(userId: Long, workDate: LocalDate): List<Shift>
    suspend fun getShiftsInRange(userId: Long, startDate: LocalDate, endDate: LocalDate): List<Shift>

    suspend fun getShiftBreaks(shiftId: Long): List<ShiftBreak>

    suspend fun patchShift(userId: Long, shiftId: Long, patch: ShiftPatch): Shift
    suspend fun findById(shiftId: Long): Shift?

    suspend fun deleteAllForUser(userId: Long): Int
}
