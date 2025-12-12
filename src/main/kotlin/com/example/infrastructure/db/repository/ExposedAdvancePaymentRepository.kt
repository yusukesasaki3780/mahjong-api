package com.example.infrastructure.db.repository

import com.example.domain.model.AdvancePayment
import com.example.domain.repository.AdvancePaymentRepository
import com.example.infrastructure.db.tables.AdvancePaymentsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.YearMonth

class ExposedAdvancePaymentRepository : AdvancePaymentRepository {

    override suspend fun findByUserIdAndYearMonth(userId: Long, yearMonth: YearMonth): AdvancePayment? = dbQuery {
        AdvancePaymentsTable
            .select {
                (AdvancePaymentsTable.userId eq userId) and
                    (AdvancePaymentsTable.yearMonth eq yearMonth.toString())
            }
            .singleOrNull()
            ?.let(::toAdvancePayment)
    }

    override suspend fun upsert(userId: Long, yearMonth: YearMonth, amount: Double): AdvancePayment = dbQuery {
        val now = Clock.System.now()
        val updated = AdvancePaymentsTable.update({
            (AdvancePaymentsTable.userId eq userId) and
                (AdvancePaymentsTable.yearMonth eq yearMonth.toString())
        }) { row ->
            row[AdvancePaymentsTable.amount] = amount
            row[AdvancePaymentsTable.updatedAt] = now
        }

        if (updated == 0) {
            AdvancePaymentsTable.insert { row ->
                row[AdvancePaymentsTable.userId] = userId
                row[AdvancePaymentsTable.yearMonth] = yearMonth.toString()
                row[AdvancePaymentsTable.amount] = amount
                row[AdvancePaymentsTable.createdAt] = now
                row[AdvancePaymentsTable.updatedAt] = now
            }
        }

        AdvancePaymentsTable
            .select {
                (AdvancePaymentsTable.userId eq userId) and
                    (AdvancePaymentsTable.yearMonth eq yearMonth.toString())
            }
            .single()
            .let(::toAdvancePayment)
    }

    override suspend fun deleteAllForUser(userId: Long): Int = dbQuery {
        AdvancePaymentsTable.deleteWhere { AdvancePaymentsTable.userId eq userId }
    }

    private fun toAdvancePayment(row: ResultRow): AdvancePayment =
        AdvancePayment(
            userId = row[AdvancePaymentsTable.userId],
            yearMonth = YearMonth.parse(row[AdvancePaymentsTable.yearMonth]),
            amount = row[AdvancePaymentsTable.amount],
            createdAt = row[AdvancePaymentsTable.createdAt],
            updatedAt = row[AdvancePaymentsTable.updatedAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
