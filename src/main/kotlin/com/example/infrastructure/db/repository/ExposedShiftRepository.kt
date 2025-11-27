package com.example.infrastructure.db.repository

import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftBreakPatch
import com.example.domain.repository.ShiftPatch
import com.example.domain.repository.ShiftRepository
import com.example.infrastructure.db.tables.ShiftBreaksTable
import com.example.infrastructure.db.tables.ShiftsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.YearMonth

/**
 * シフト情報を Exposed で管理する実装。
 */
class ExposedShiftRepository : ShiftRepository {

    override suspend fun insertShift(shift: Shift): Shift = dbQuery {
        val shiftId = ShiftsTable.insert { row ->
            row[userId] = shift.userId
            row[workDate] = shift.workDate
            row[startTime] = shift.startTime
            row[endTime] = shift.endTime
            row[memo] = shift.memo
            row[createdAt] = shift.createdAt
            row[updatedAt] = shift.updatedAt
        } get ShiftsTable.id

        upsertBreaks(shiftId, shift.breaks)

        fetchShift(shiftId)
    }

    override suspend fun updateShift(shift: Shift): Shift = dbQuery {
        val targetId = shift.id ?: error("Shift id is required for update.")
        ShiftsTable.update({ ShiftsTable.id eq targetId }) { row ->
            row[userId] = shift.userId
            row[workDate] = shift.workDate
            row[startTime] = shift.startTime
            row[endTime] = shift.endTime
            row[memo] = shift.memo
            row[createdAt] = shift.createdAt
            row[updatedAt] = shift.updatedAt
        }

        ShiftBreaksTable.deleteWhere { ShiftBreaksTable.shiftId eq targetId }
        upsertBreaks(targetId, shift.breaks)

        fetchShift(targetId)
    }

    override suspend fun deleteShift(shiftId: Long): Boolean = dbQuery {
        ShiftBreaksTable.deleteWhere { ShiftBreaksTable.shiftId eq shiftId }
        ShiftsTable.deleteWhere { ShiftsTable.id eq shiftId } > 0
    }

    override suspend fun findById(shiftId: Long): Shift? = dbQuery {
        fetchShiftOrNull(shiftId)
    }

    override suspend fun getMonthlyShifts(userId: Long, yearMonth: YearMonth): List<Shift> = dbQuery {
        val firstDay = LocalDate(yearMonth.year, yearMonth.monthValue, 1)
        val lastDay = LocalDate(yearMonth.year, yearMonth.monthValue, yearMonth.lengthOfMonth())
        loadShiftsInRange(userId, firstDay, lastDay)
    }

    override suspend fun getShiftsOnDate(userId: Long, workDate: LocalDate): List<Shift> = dbQuery {
        loadShiftsInRange(userId, workDate, workDate)
    }

    override suspend fun getShiftsInRange(userId: Long, startDate: LocalDate, endDate: LocalDate): List<Shift> = dbQuery {
        loadShiftsInRange(userId, startDate, endDate)
    }

    override suspend fun getShiftBreaks(shiftId: Long): List<ShiftBreak> = dbQuery {
        ShiftBreaksTable
            .select { ShiftBreaksTable.shiftId eq shiftId }
            .map(::toShiftBreak)
    }

    override suspend fun patchShift(userId: Long, shiftId: Long, patch: ShiftPatch): Shift = dbQuery {
        ensureShiftOwnedByUser(shiftId, userId)

        if (patch.workDate != null || patch.startTime != null || patch.endTime != null || patch.memo != null || patch.updatedAt != null) {
            ShiftsTable.update({ (ShiftsTable.id eq shiftId) and (ShiftsTable.userId eq userId) }) { row ->
                patch.workDate?.let { row[workDate] = it }
                patch.startTime?.let { row[startTime] = it }
                patch.endTime?.let { row[endTime] = it }
                patch.memo?.let { row[memo] = it }
                patch.updatedAt?.let { row[updatedAt] = it }
            }
        }

        patch.breakPatches?.forEach { brPatch ->
            handleBreakPatch(shiftId, brPatch)
        }

        fetchShift(shiftId)
    }

    private fun loadShiftsInRange(userId: Long, start: LocalDate, end: LocalDate): List<Shift> {
        val shifts = ShiftsTable
            .select {
                (ShiftsTable.userId eq userId) and
                    (ShiftsTable.workDate greaterEq start) and
                    (ShiftsTable.workDate lessEq end)
            }
            .map(::toShift)

        val breakMap = fetchBreaksByShiftIds(shifts.mapNotNull { it.id })
        return shifts.map { shift ->
            shift.copy(breaks = breakMap[shift.id] ?: emptyList())
        }
    }

    private fun fetchShift(id: Long): Shift =
        fetchShiftOrNull(id) ?: error("Shift not found: $id")

    private fun fetchShiftOrNull(id: Long): Shift? {
        val shift = ShiftsTable
            .select { ShiftsTable.id eq id }
            .map(::toShift)
            .singleOrNull()
            ?: return null
        val breaks = fetchBreaksByShiftIds(listOf(id))[id].orEmpty()
        return shift.copy(breaks = breaks)
    }

    private fun ensureShiftOwnedByUser(shiftId: Long, userId: Long) {
        val exists = ShiftsTable
            .select { (ShiftsTable.id eq shiftId) and (ShiftsTable.userId eq userId) }
            .limit(1)
            .any()
        if (!exists) {
            throw IllegalArgumentException("Shift not found or not owned by user.")
        }
    }

    private fun handleBreakPatch(shiftId: Long, patch: ShiftBreakPatch) {
        val breakId = patch.id
        when {
            breakId == null -> {
                if (patch.delete) return
                require(patch.breakStart != null && patch.breakEnd != null) {
                    "New break requires both start and end."
                }
                ShiftBreaksTable.insert { row ->
                    row[ShiftBreaksTable.shiftId] = shiftId
                    row[breakStart] = patch.breakStart
                    row[breakEnd] = patch.breakEnd
                }
            }
            patch.delete -> {
                ShiftBreaksTable.deleteWhere {
                    (ShiftBreaksTable.id eq breakId) and (ShiftBreaksTable.shiftId eq shiftId)
                }
            }
            else -> {
                ShiftBreaksTable.update({
                    (ShiftBreaksTable.id eq breakId) and (ShiftBreaksTable.shiftId eq shiftId)
                }) { row ->
                    patch.breakStart?.let { row[breakStart] = it }
                    patch.breakEnd?.let { row[breakEnd] = it }
                }
            }
        }
    }

    private fun upsertBreaks(shiftId: Long, breaks: List<ShiftBreak>) {
        breaks.forEach { br ->
            ShiftBreaksTable.insert { row ->
                row[ShiftBreaksTable.shiftId] = shiftId
                row[breakStart] = br.breakStart
                row[breakEnd] = br.breakEnd
            }
        }
    }

    private fun fetchBreaksByShiftIds(ids: List<Long>): Map<Long, List<ShiftBreak>> =
        if (ids.isEmpty()) {
            emptyMap()
        } else {
            ShiftBreaksTable
                .select { ShiftBreaksTable.shiftId inList ids }
                .map(::toShiftBreak)
                .groupBy { it.shiftId ?: error("shiftId is null in break record.") }
        }

    private fun toShift(row: ResultRow): Shift =
        Shift(
            id = row[ShiftsTable.id],
            userId = row[ShiftsTable.userId],
            workDate = row[ShiftsTable.workDate],
            startTime = row[ShiftsTable.startTime],
            endTime = row[ShiftsTable.endTime],
            memo = row[ShiftsTable.memo],
            createdAt = row[ShiftsTable.createdAt],
            updatedAt = row[ShiftsTable.updatedAt]
        )

    private fun toShiftBreak(row: ResultRow): ShiftBreak =
        ShiftBreak(
            id = row[ShiftBreaksTable.id],
            shiftId = row[ShiftBreaksTable.shiftId],
            breakStart = row[ShiftBreaksTable.breakStart],
            breakEnd = row[ShiftBreaksTable.breakEnd]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
