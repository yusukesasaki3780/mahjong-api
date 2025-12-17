package com.example.infrastructure.db.repository

import com.example.domain.model.ShiftSlotType
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExposedShiftRequirementRepositoryIntegrationTest : RepositoryTestBase() {

    private val repository = ExposedShiftRequirementRepository()

    @Test
    fun `upsert creates and updates requirements`() = runDbTest {
        val created = repository.upsert(
            storeId = 1,
            targetDate = LocalDate(2025, 1, 1),
            shiftType = ShiftSlotType.EARLY,
            startRequired = 2,
            endRequired = 1
        )
        assertEquals(2, created.startRequired)
        assertEquals(1, created.endRequired)

        val updated = repository.upsert(
            storeId = 1,
            targetDate = LocalDate(2025, 1, 1),
            shiftType = ShiftSlotType.EARLY,
            startRequired = 3,
            endRequired = 2
        )
        assertEquals(created.id, updated.id)
        assertEquals(3, updated.startRequired)
        assertEquals(2, updated.endRequired)

        val range = repository.findByStoreAndDateRange(
            storeId = 1,
            startDate = LocalDate(2025, 1, 1),
            endDate = LocalDate(2025, 1, 5)
        )
        assertEquals(1, range.size)
        assertEquals(updated.startRequired, range.first().startRequired)
    }

    @Test
    fun `existsBefore returns true when earlier record present`() = runDbTest {
        repository.upsert(
            storeId = 1,
            targetDate = LocalDate(2025, 1, 1),
            shiftType = ShiftSlotType.LATE,
            startRequired = 1,
            endRequired = 1
        )

        assertTrue(repository.existsBefore(storeId = 1, referenceDate = LocalDate(2025, 2, 1)))
    }
}
