package com.example.infrastructure.db.repository

import com.example.domain.model.ShiftRequirement
import com.example.domain.model.ShiftSlotType
import com.example.domain.repository.ShiftRequirementRepository
import com.example.infrastructure.db.tables.ShiftRequirementsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ExposedShiftRequirementRepository : ShiftRequirementRepository {

    override suspend fun findByStoreAndDateRange(
        storeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ShiftRequirement> = dbQuery {
        ShiftRequirementsTable
            .select {
                (ShiftRequirementsTable.storeId eq storeId) and
                    ShiftRequirementsTable.targetDate.between(startDate, endDate)
            }
            .map(::toModel)
    }

    override suspend fun upsert(
        storeId: Long,
        targetDate: LocalDate,
        shiftType: ShiftSlotType,
        startRequired: Int,
        endRequired: Int
    ): ShiftRequirement = dbQuery {
        val now = Clock.System.now()
        val existingId = ShiftRequirementsTable
            .select {
                (ShiftRequirementsTable.storeId eq storeId) and
                    (ShiftRequirementsTable.targetDate eq targetDate) and
                    (ShiftRequirementsTable.shiftType eq shiftType)
            }
            .singleOrNull()
            ?.get(ShiftRequirementsTable.id)

        if (existingId == null) {
            val newId = ShiftRequirementsTable.insert { row ->
                row[ShiftRequirementsTable.storeId] = storeId
                row[ShiftRequirementsTable.targetDate] = targetDate
                row[ShiftRequirementsTable.shiftType] = shiftType
                row[ShiftRequirementsTable.startRequired] = startRequired
                row[ShiftRequirementsTable.endRequired] = endRequired
                row[ShiftRequirementsTable.createdAt] = now
                row[ShiftRequirementsTable.updatedAt] = now
            } get ShiftRequirementsTable.id
            fetchById(newId)
        } else {
            ShiftRequirementsTable.update({ ShiftRequirementsTable.id eq existingId }) { row ->
                row[ShiftRequirementsTable.startRequired] = startRequired
                row[ShiftRequirementsTable.endRequired] = endRequired
                row[ShiftRequirementsTable.updatedAt] = now
            }
            fetchById(existingId)
        }
    }

    override suspend fun existsBefore(storeId: Long, referenceDate: LocalDate): Boolean = dbQuery {
        ShiftRequirementsTable
            .select {
                (ShiftRequirementsTable.storeId eq storeId) and
                    (ShiftRequirementsTable.targetDate less referenceDate)
            }
            .limit(1)
            .any()
    }

    private fun fetchById(id: Long): ShiftRequirement =
        ShiftRequirementsTable
            .select { ShiftRequirementsTable.id eq id }
            .single()
            .let(::toModel)

    private fun toModel(row: ResultRow) =
        ShiftRequirement(
            id = row[ShiftRequirementsTable.id],
            storeId = row[ShiftRequirementsTable.storeId],
            targetDate = row[ShiftRequirementsTable.targetDate],
            shiftType = row[ShiftRequirementsTable.shiftType],
            startRequired = row[ShiftRequirementsTable.startRequired],
            endRequired = row[ShiftRequirementsTable.endRequired],
            createdAt = row[ShiftRequirementsTable.createdAt],
            updatedAt = row[ShiftRequirementsTable.updatedAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
