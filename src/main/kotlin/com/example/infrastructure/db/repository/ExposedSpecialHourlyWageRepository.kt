package com.example.infrastructure.db.repository

import com.example.domain.model.SpecialHourlyWage
import com.example.domain.repository.SpecialHourlyWageRepository
import com.example.infrastructure.db.tables.ShiftSpecialAllowancesTable
import com.example.infrastructure.db.tables.SpecialHourlyWagesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ExposedSpecialHourlyWageRepository : SpecialHourlyWageRepository {

    override suspend fun listByUser(userId: Long): List<SpecialHourlyWage> = dbQuery {
        SpecialHourlyWagesTable
            .select { SpecialHourlyWagesTable.userId eq userId }
            .orderBy(SpecialHourlyWagesTable.id)
            .map(::toModel)
    }

    override suspend fun findById(id: Long): SpecialHourlyWage? = dbQuery {
        SpecialHourlyWagesTable
            .select { SpecialHourlyWagesTable.id eq id }
            .map(::toModel)
            .singleOrNull()
    }

    override suspend fun insert(userId: Long, label: String, hourlyWage: Int): SpecialHourlyWage = dbQuery {
        val now = currentInstant()
        val newId = SpecialHourlyWagesTable.insert { row ->
            row[SpecialHourlyWagesTable.userId] = userId
            row[SpecialHourlyWagesTable.label] = label
            row[SpecialHourlyWagesTable.hourlyWage] = hourlyWage
            row[createdAt] = now
            row[updatedAt] = now
        } get SpecialHourlyWagesTable.id

        fetchById(newId)
    }

    override suspend fun delete(userId: Long, id: Long): Boolean = dbQuery {
        SpecialHourlyWagesTable.deleteWhere {
            (SpecialHourlyWagesTable.id eq id) and (SpecialHourlyWagesTable.userId eq userId)
        } > 0
    }

    override suspend fun existsLabel(userId: Long, label: String): Boolean = dbQuery {
        SpecialHourlyWagesTable
            .select {
                (SpecialHourlyWagesTable.userId eq userId) and
                    (SpecialHourlyWagesTable.label eq label)
            }
            .limit(1)
            .any()
    }

    override suspend fun detachFromShifts(specialWageId: Long) {
        dbQuery {
            ShiftSpecialAllowancesTable.deleteWhere {
                ShiftSpecialAllowancesTable.specialHourlyWageId eq specialWageId
            }
        }
    }

    private fun toModel(row: ResultRow) = SpecialHourlyWage(
        id = row[SpecialHourlyWagesTable.id],
        userId = row[SpecialHourlyWagesTable.userId],
        label = row[SpecialHourlyWagesTable.label],
        hourlyWage = row[SpecialHourlyWagesTable.hourlyWage],
        createdAt = row[SpecialHourlyWagesTable.createdAt],
        updatedAt = row[SpecialHourlyWagesTable.updatedAt]
    )

    private fun currentInstant() = Clock.System.now()

    private fun fetchById(id: Long): SpecialHourlyWage =
        SpecialHourlyWagesTable
            .select { SpecialHourlyWagesTable.id eq id }
            .map(::toModel)
            .single()

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
