package com.example.infrastructure.db.repository

import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftBreakPatch
import com.example.domain.repository.ShiftPatch
import com.example.infrastructure.db.tables.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExposedShiftRepositoryIntegrationTest : RepositoryTestBase() {

    private val repository = ExposedShiftRepository()

    @Test
    fun `patch shift updates memo and breaks`() = runDbTest {
        val userId = seedUser()
        val now = Clock.System.now()
        val created = repository.insertShift(
            Shift(
                id = null,
                userId = userId,
                storeId = 1,
                workDate = LocalDate(2025, 1, 1),
                startTime = now,
                endTime = now.plus(8, DateTimeUnit.HOUR),
                memo = "original",
                breaks = listOf(
                    ShiftBreak(
                        id = null,
                        shiftId = null,
                        breakStart = now.plus(2, DateTimeUnit.HOUR),
                        breakEnd = now.plus(2, DateTimeUnit.HOUR).plus(30, DateTimeUnit.MINUTE)
                    )
                ),
                createdAt = now,
                updatedAt = now
            )
        )

        val existingBreakId = created.breaks.first().id!!
        val patched = repository.patchShift(
            userId = userId,
            shiftId = created.id!!,
            patch = ShiftPatch(
                memo = "patched",
                breakPatches = listOf(
                    ShiftBreakPatch(
                        id = existingBreakId,
                        breakStart = created.breaks.first().breakStart,
                        breakEnd = created.breaks.first().breakEnd.plus(10, DateTimeUnit.MINUTE)
                    ),
                    ShiftBreakPatch(
                        id = null,
                        breakStart = created.startTime.plus(4, DateTimeUnit.HOUR),
                        breakEnd = created.startTime.plus(4, DateTimeUnit.HOUR).plus(15, DateTimeUnit.MINUTE)
                    )
                )
            )
        )

        assertEquals("patched", patched.memo)
        assertEquals(2, patched.breaks.size)
    }

    @Test
    fun `delete shift cascades breaks`() = runDbTest {
        val userId = seedUser()
        val now = Clock.System.now()
        val created = repository.insertShift(
            Shift(
                id = null,
                userId = userId,
                storeId = 1,
                workDate = LocalDate(2025, 2, 1),
                startTime = now,
                endTime = now.plus(6, DateTimeUnit.HOUR),
                memo = null,
                breaks = listOf(
                    ShiftBreak(
                        id = null,
                        shiftId = null,
                        breakStart = now.plus(1, DateTimeUnit.HOUR),
                        breakEnd = now.plus(1, DateTimeUnit.HOUR).plus(15, DateTimeUnit.MINUTE)
                    )
                ),
                createdAt = now,
                updatedAt = now
            )
        )

        assertTrue(repository.deleteShift(created.id!!))
    }

    private fun seedUser(): Long {
        val now = Clock.System.now()
        return transaction {
            UsersTable.insert {
                it[name] = "ShiftUser"
                it[nickname] = "su"
                it[storeId] = 1
                it[storeName] = "Store"
                it[prefectureCode] = "01"
                it[email] = "shift-${now.toEpochMilliseconds()}@example.com"
                it[zooId] = (now.toEpochMilliseconds() % 900_000).toInt() + 1
                it[UsersTable.isAdmin] = false
                it[UsersTable.isDeleted] = false
                it[createdAt] = now
                it[updatedAt] = now
            } get UsersTable.userId
        }
    }
}
