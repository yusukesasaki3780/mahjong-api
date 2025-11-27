package com.example.usecase

import com.example.common.error.DomainValidationException
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.usecase.shift.EditShiftUseCase
import com.example.usecase.TestAuditSupport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import org.valiktor.ConstraintViolationException

class EditShiftUseCaseTest {

    private val repository = mockk<ShiftRepository>()
    private val useCase = EditShiftUseCase(repository, TestAuditSupport.auditLogger)

    private val start = Instant.parse("2025-01-01T09:00:00Z")
    private val end = Instant.parse("2025-01-01T18:00:00Z")

    @Test
    fun `updates shift when payload is valid`() = runTest {
        coEvery { repository.findById(10) } returns sampleShift()
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns emptyList()
        coEvery { repository.updateShift(any()) } returns sampleShift()

        val result = useCase(
            EditShiftUseCase.Command(
                shiftId = 10,
                userId = 1,
                workDate = LocalDate(2025, 1, 1),
                startTime = start,
                endTime = end,
                memo = "memo",
                breaks = listOf(
                    EditShiftUseCase.BreakCommand(
                        breakStart = Instant.parse("2025-01-01T12:00:00Z"),
                        breakEnd = Instant.parse("2025-01-01T12:15:00Z")
                    )
                ),
                createdAt = start
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(10, result.id)
        coVerify { repository.updateShift(any()) }
    }

    @Test
    fun `throws when overlaps existing shift`() = runTest {
        val overlapping = sampleShift().copy(id = 20)
        coEvery { repository.findById(10) } returns sampleShift()
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns listOf(overlapping)

        assertFailsWith<DomainValidationException> {
            useCase(
                EditShiftUseCase.Command(
                    shiftId = 10,
                    userId = 1,
                    workDate = LocalDate(2025, 1, 1),
                    startTime = start,
                    endTime = end,
                    memo = null,
                    breaks = emptyList(),
                    createdAt = start
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `throws when end before start`() = runTest {
        coEvery { repository.getShiftsOnDate(any(), any()) } returns emptyList()
        assertFailsWith<ConstraintViolationException> {
            useCase(
                EditShiftUseCase.Command(
                    shiftId = 10,
                    userId = 1,
                    workDate = LocalDate(2025, 1, 1),
                    startTime = end,
                    endTime = start,
                    memo = null,
                    breaks = emptyList(),
                    createdAt = start
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    private fun sampleShift(): Shift {
        val breakStart = Instant.parse("2025-01-01T12:00:00Z")
        val breakEnd = Instant.parse("2025-01-01T12:15:00Z")
        return Shift(
            id = 10,
            userId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = start,
            endTime = end,
            memo = "memo",
            breaks = listOf(
                ShiftBreak(
                    id = 11,
                    shiftId = 10,
                    breakStart = breakStart,
                    breakEnd = breakEnd
                )
            ),
            createdAt = start,
            updatedAt = end
        )
    }
}
