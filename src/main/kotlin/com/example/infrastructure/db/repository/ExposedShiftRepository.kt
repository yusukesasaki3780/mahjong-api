package com.example.infrastructure.db.repository

import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.model.SpecialHourlyWage
import com.example.domain.repository.ShiftBreakPatch
import com.example.domain.repository.ShiftPatch
import com.example.domain.repository.ShiftRepository
import com.example.infrastructure.db.tables.ShiftBreaksTable
import com.example.infrastructure.db.tables.ShiftSpecialAllowancesTable
import com.example.infrastructure.db.tables.ShiftsTable
import com.example.infrastructure.db.tables.SpecialHourlyWagesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
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
            row[storeId] = shift.storeId
            row[workDate] = shift.workDate
            row[startTime] = shift.startTime
            row[endTime] = shift.endTime
            row[memo] = shift.memo
            row[createdAt] = shift.createdAt
            row[updatedAt] = shift.updatedAt
        } get ShiftsTable.id

        upsertBreaks(shiftId, shift.breaks)
        upsertSpecialAllowance(shiftId, shift.specialHourlyWageId)

        fetchShift(shiftId)
    }

    override suspend fun updateShift(shift: Shift): Shift = dbQuery {
        val targetId = shift.id ?: error("Shift id is required for update.")
        ShiftsTable.update({ ShiftsTable.id eq targetId }) { row ->
            row[userId] = shift.userId
            row[storeId] = shift.storeId
            row[workDate] = shift.workDate
            row[startTime] = shift.startTime
            row[endTime] = shift.endTime
            row[memo] = shift.memo
            row[createdAt] = shift.createdAt
            row[updatedAt] = shift.updatedAt
        }

        ShiftBreaksTable.deleteWhere { ShiftBreaksTable.shiftId eq targetId }
        upsertBreaks(targetId, shift.breaks)
        upsertSpecialAllowance(targetId, shift.specialHourlyWageId)

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

    override suspend fun getShiftsByStore(storeId: Long, startDate: LocalDate, endDate: LocalDate): List<Shift> = dbQuery {
        loadShiftsByStore(storeId, startDate, endDate)
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

        if (patch.specialHourlyWageIdSet) {
            upsertSpecialAllowance(shiftId, patch.specialHourlyWageId)
        }

        patch.breakPatches?.forEach { brPatch ->
            handleBreakPatch(shiftId, brPatch)
        }

        fetchShift(shiftId)
    }

    private fun loadShiftsInRange(userId: Long, start: LocalDate, end: LocalDate): List<Shift> {
        val rows = ShiftsTable
            .select {
                (ShiftsTable.userId eq userId) and
                    (ShiftsTable.workDate greaterEq start) and
                    (ShiftsTable.workDate lessEq end)
            }
            .toList()

        val specialAssignments = fetchSpecialAssignments(rows.map { it[ShiftsTable.id] })
        val breakMap = fetchBreaksByShiftIds(rows.map { it[ShiftsTable.id] })
        return rows.map { row ->
            val assignment = specialAssignments[row[ShiftsTable.id]]
            val shift = toShift(row, assignment?.special, assignment?.specialId)
            shift.copy(breaks = breakMap[shift.id] ?: emptyList())
        }
    }

    private fun loadShiftsByStore(storeId: Long, start: LocalDate, end: LocalDate): List<Shift> {
        val rows = ShiftsTable
            .select {
                (ShiftsTable.storeId eq storeId) and
                    (ShiftsTable.workDate greaterEq start) and
                    (ShiftsTable.workDate lessEq end)
            }
            .toList()
        val specialAssignments = fetchSpecialAssignments(rows.map { it[ShiftsTable.id] })
        val breakMap = fetchBreaksByShiftIds(rows.map { it[ShiftsTable.id] })
        return rows.map { row ->
            val assignment = specialAssignments[row[ShiftsTable.id]]
            val shift = toShift(row, assignment?.special, assignment?.specialId)
            shift.copy(breaks = breakMap[shift.id] ?: emptyList())
        }
    }

    private fun fetchShift(id: Long): Shift =
        fetchShiftOrNull(id) ?: error("Shift not found: $id")

    private fun fetchShiftOrNull(id: Long): Shift? {
        val row = ShiftsTable
            .select { ShiftsTable.id eq id }
            .singleOrNull()
            ?: return null
        val assignment = fetchSpecialAssignments(listOf(id))[id]
        val shift = toShift(row, assignment?.special, assignment?.specialId)
        val breaks = fetchBreaksByShiftIds(listOf(id))[id].orEmpty()
        return shift.copy(breaks = breaks)
    }

    override suspend fun deleteAllForUser(userId: Long): Int = dbQuery {
        ShiftsTable.deleteWhere { ShiftsTable.userId eq userId }
    }

    override suspend fun userHasShiftInStore(userId: Long, storeId: Long): Boolean = dbQuery {
        ShiftsTable
            .select {
                (ShiftsTable.userId eq userId) and (ShiftsTable.storeId eq storeId)
            }
            .limit(1)
            .any()
    }

    override suspend fun getStoreIdsForUser(userId: Long): Set<Long> = dbQuery {
        ShiftsTable
            .slice(ShiftsTable.storeId)
            .select { ShiftsTable.userId eq userId }
            .map { it[ShiftsTable.storeId] }
            .toSet()
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

    private fun toShift(row: ResultRow, special: SpecialHourlyWage?, specialId: Long?): Shift =
        Shift(
            id = row[ShiftsTable.id],
            userId = row[ShiftsTable.userId],
            storeId = row[ShiftsTable.storeId],
            workDate = row[ShiftsTable.workDate],
            startTime = row[ShiftsTable.startTime],
            endTime = row[ShiftsTable.endTime],
            memo = row[ShiftsTable.memo],
            specialHourlyWage = special,
            specialHourlyWageId = specialId ?: special?.id,
            createdAt = row[ShiftsTable.createdAt],
            updatedAt = row[ShiftsTable.updatedAt]
        )

    private fun fetchSpecialAssignments(ids: List<Long>): Map<Long, SpecialAssignment> {
        if (ids.isEmpty()) return emptyMap()
        val allowances = ShiftSpecialAllowancesTable
            .select { ShiftSpecialAllowancesTable.shiftId inList ids }
            .associate { row ->
                row[ShiftSpecialAllowancesTable.shiftId] to row[ShiftSpecialAllowancesTable.specialHourlyWageId]
            }
        if (allowances.isEmpty()) return emptyMap()
        val specialIds = allowances.values.filterNotNull()
        val specials = if (specialIds.isEmpty()) {
            emptyMap()
        } else {
            SpecialHourlyWagesTable
                .select { SpecialHourlyWagesTable.id inList specialIds }
                .associate { row ->
                    row[SpecialHourlyWagesTable.id] to row.toSpecialHourlyWage()
                }
        }
        return allowances.mapValues { (_, specialId) ->
            SpecialAssignment(
                specialId = specialId,
                special = specialId?.let { specials[it] }
            )
        }
    }

    private fun ResultRow.toSpecialHourlyWage(): SpecialHourlyWage =
        SpecialHourlyWage(
            id = this[SpecialHourlyWagesTable.id],
            userId = this[SpecialHourlyWagesTable.userId],
            label = this[SpecialHourlyWagesTable.label],
            hourlyWage = this[SpecialHourlyWagesTable.hourlyWage],
            createdAt = this[SpecialHourlyWagesTable.createdAt],
            updatedAt = this[SpecialHourlyWagesTable.updatedAt]
        )

    private data class SpecialAssignment(
        val specialId: Long?,
        val special: SpecialHourlyWage?
    )

    private fun toShiftBreak(row: ResultRow): ShiftBreak =
        ShiftBreak(
            id = row[ShiftBreaksTable.id],
            shiftId = row[ShiftBreaksTable.shiftId],
            breakStart = row[ShiftBreaksTable.breakStart],
            breakEnd = row[ShiftBreaksTable.breakEnd]
        )

    private fun upsertSpecialAllowance(shiftId: Long, specialId: Long?) {
        ShiftSpecialAllowancesTable.deleteWhere { ShiftSpecialAllowancesTable.shiftId eq shiftId }
        if (specialId != null) {
            val now = Clock.System.now()
            ShiftSpecialAllowancesTable.insert { row ->
                row[ShiftSpecialAllowancesTable.shiftId] = shiftId
                row[ShiftSpecialAllowancesTable.specialHourlyWageId] = specialId
                row[ShiftSpecialAllowancesTable.createdAt] = now
                row[ShiftSpecialAllowancesTable.updatedAt] = now
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
