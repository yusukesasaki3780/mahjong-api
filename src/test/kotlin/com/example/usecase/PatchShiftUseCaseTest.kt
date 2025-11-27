package com.example.usecase

import com.example.common.error.DomainValidationException
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.usecase.shift.PatchShiftUseCase
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

class PatchShiftUseCaseTest {

    private val repository = mockk<ShiftRepository>()
    private val useCase = PatchShiftUseCase(repository, TestAuditSupport.auditLogger)

    @Test
    fun `forwards patch when payload is valid`() = runTest {
        val shift = sampleShift(memo = "patched")
        coEvery { repository.findById(10) } returns sampleShift("old")
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns emptyList()
        coEvery { repository.patchShift(eq(1), eq(10), any()) } returns shift

        val result = useCase(
            PatchShiftUseCase.Command(
                userId = 1,
                shiftId = 10,
                memo = "patched",
                startTime = Instant.parse("2025-01-01T09:00:00Z"),
                endTime = Instant.parse("2025-01-01T18:00:00Z"),
                breaks = listOf(
                    PatchShiftUseCase.BreakPatchCommand(
                        id = null,
                        breakStart = Instant.parse("2025-01-01T11:00:00Z"),
                        breakEnd = Instant.parse("2025-01-01T11:15:00Z")
                    ),
                    PatchShiftUseCase.BreakPatchCommand(
                        id = 5,
                        delete = true
                    )
                )
            ),
            TestAuditSupport.auditContext
        )

        assertEquals("patched", result.memo)
        coVerify { repository.patchShift(eq(1), eq(10), any()) }
    }

    @Test
    fun `throws when only start time is provided`() = runTest {
        coEvery { repository.findById(10) } returns sampleShift("old")
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns emptyList()
        assertFailsWith<DomainValidationException> {
            useCase(
                PatchShiftUseCase.Command(
                    userId = 1,
                    shiftId = 10,
                    startTime = Instant.parse("2025-01-01T09:00:00Z")
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `throws when new breaks overlap each other`() = runTest {
        coEvery { repository.findById(10) } returns sampleShift("old")
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns emptyList()

        assertFailsWith<DomainValidationException> {
            useCase(
                PatchShiftUseCase.Command(
                    userId = 1,
                    shiftId = 10,
                    breaks = listOf(
                        PatchShiftUseCase.BreakPatchCommand(
                            id = null,
                            breakStart = Instant.parse("2025-01-01T11:05:00Z"),
                            breakEnd = Instant.parse("2025-01-01T11:20:00Z")
                        ),
                        PatchShiftUseCase.BreakPatchCommand(
                            id = null,
                            breakStart = Instant.parse("2025-01-01T11:10:00Z"),
                            breakEnd = Instant.parse("2025-01-01T11:25:00Z")
                        )
                    )
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `allows memo only patch`() = runTest {
        val before = sampleShift("old")
        coEvery { repository.findById(10) } returns before
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns emptyList()
        coEvery { repository.patchShift(eq(1), eq(10), any()) } returns before.copy(memo = "updated")

        val result = useCase(
            PatchShiftUseCase.Command(
                userId = 1,
                shiftId = 10,
                memo = "updated"
            ),
            TestAuditSupport.auditContext
        )

        assertEquals("updated", result.memo)
    }

    @Test
    fun `allows cross midnight shift update with breaks`() = runTest {
        val before = sampleShift("old").copy(
            workDate = LocalDate(2025, 11, 26),
            startTime = Instant.parse("2025-11-26T13:00:00Z"), // 22:00+09
            endTime = Instant.parse("2025-11-27T01:00:00Z"),   // 10:00+09
            breaks = listOf(
                ShiftBreak(
                    id = 50,
                    shiftId = 10,
                    breakStart = Instant.parse("2025-11-26T14:00:00Z"),
                    breakEnd = Instant.parse("2025-11-26T14:30:00Z")
                )
            )
        )
        coEvery { repository.findById(10) } returns before
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 11, 26)) } returns listOf(before)
        coEvery { repository.patchShift(eq(1), eq(10), any()) } returns before.copy(memo = "patched")

        val result = useCase(
            PatchShiftUseCase.Command(
                userId = 1,
                shiftId = 10,
                workDate = LocalDate(2025, 11, 26),
                startTime = Instant.parse("2025-11-26T14:00:00Z"), // 23:00+09
                endTime = Instant.parse("2025-11-27T01:00:00Z"),   // 10:00+09
                memo = "patched",
                breaks = listOf(
                    PatchShiftUseCase.BreakPatchCommand(
                        id = null,
                        breakStart = Instant.parse("2025-11-26T15:30:00Z"),
                        breakEnd = Instant.parse("2025-11-26T16:00:00Z")
                    ),
                    PatchShiftUseCase.BreakPatchCommand(
                        id = null,
                        breakStart = Instant.parse("2025-11-26T20:30:00Z"),
                        breakEnd = Instant.parse("2025-11-26T21:00:00Z")
                    )
                )
            ),
            TestAuditSupport.auditContext
        )

        assertEquals("patched", result.memo)
    }

    @Test
    fun `replaces existing breaks when ids are omitted`() = runTest {
        val before = sampleShift("old").copy(
            startTime = Instant.parse("2025-11-26T12:00:00Z"), // 21:00+09
            endTime = Instant.parse("2025-11-27T00:00:00Z"),   // 09:00+09
            breaks = listOf(
                ShiftBreak(
                    id = 70,
                    shiftId = 10,
                    breakStart = Instant.parse("2025-11-26T12:30:00Z"),
                    breakEnd = Instant.parse("2025-11-26T12:45:00Z")
                )
            )
        )
        coEvery { repository.findById(10) } returns before
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns emptyList()
        coEvery { repository.patchShift(eq(1), eq(10), any()) } returns before.copy(memo = "new breaks")

        val result = useCase(
            PatchShiftUseCase.Command(
                userId = 1,
                shiftId = 10,
                startTime = Instant.parse("2025-11-26T13:00:00Z"), // 22:00+09
                endTime = Instant.parse("2025-11-27T00:00:00Z"),
                breaks = listOf(
                    PatchShiftUseCase.BreakPatchCommand(
                        id = null,
                        breakStart = Instant.parse("2025-11-26T14:00:00Z"),
                        breakEnd = Instant.parse("2025-11-26T14:30:00Z")
                    )
                )
            ),
            TestAuditSupport.auditContext
        )

        assertEquals("new breaks", result.memo)
    }

    private fun sampleShift(memo: String): Shift {
        val start = Instant.parse("2025-01-01T09:00:00Z")
        val end = Instant.parse("2025-01-01T18:00:00Z")
        val breakStart = Instant.parse("2025-01-01T11:00:00Z")
        val breakEnd = Instant.parse("2025-01-01T11:15:00Z")
        return Shift(
            id = 10,
            userId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = start,
            endTime = end,
            memo = memo,
            breaks = listOf(
                ShiftBreak(
                    id = 5,
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
