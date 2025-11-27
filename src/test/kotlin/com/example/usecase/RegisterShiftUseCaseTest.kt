package com.example.usecase

import com.example.common.error.DomainValidationException
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.usecase.shift.RegisterShiftUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RegisterShiftUseCaseTest {

    private val repository = mockk<ShiftRepository>()
    private val useCase = RegisterShiftUseCase(repository)

    private val start = Instant.parse("2025-01-01T09:00:00Z")
    private val end = Instant.parse("2025-01-01T18:00:00Z")

    @Test
    fun `creates shift when payload is valid`() = runTest {
        val shift = Shift(
            id = 1,
            userId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = start,
            endTime = end,
            memo = "memo",
            breaks = listOf(
                ShiftBreak(
                    id = 10,
                    shiftId = 1,
                    breakStart = Instant.parse("2025-01-01T12:00:00Z"),
                    breakEnd = Instant.parse("2025-01-01T12:15:00Z")
                )
            ),
            createdAt = start,
            updatedAt = start
        )
        coEvery { repository.getShiftsOnDate(any(), any()) } returns emptyList()
        coEvery { repository.insertShift(any()) } returns shift

        val result = useCase(
            RegisterShiftUseCase.Command(
                userId = 1,
                workDate = LocalDate(2025, 1, 1),
                startTime = start,
                endTime = end,
                memo = "memo",
                breaks = listOf(
                    RegisterShiftUseCase.BreakCommand(
                        breakStart = Instant.parse("2025-01-01T12:00:00Z"),
                        breakEnd = Instant.parse("2025-01-01T12:15:00Z")
                    )
                )
            )
        )

        assertEquals(1, result.id)
        coVerify { repository.insertShift(any()) }
    }

    @Test
    fun `throws when break overlaps shift start`() = runTest {
        assertFailsWith<DomainValidationException> {
            coEvery { repository.getShiftsOnDate(any(), any()) } returns emptyList()
            useCase(
                RegisterShiftUseCase.Command(
                    userId = 1,
                    workDate = LocalDate(2025, 1, 1),
                    startTime = start,
                    endTime = end,
                    memo = null,
                    breaks = listOf(
                        RegisterShiftUseCase.BreakCommand(
                            breakStart = Instant.parse("2025-01-01T08:00:00Z"),
                            breakEnd = Instant.parse("2025-01-01T09:30:00Z")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `throws when shift overlaps existing`() = runTest {
        val existing = Shift(
            id = 2,
            userId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = start,
            endTime = end,
            memo = null,
            breaks = emptyList(),
            createdAt = start,
            updatedAt = end
        )
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns listOf(existing)

        assertFailsWith<DomainValidationException> {
            useCase(
                RegisterShiftUseCase.Command(
                    userId = 1,
                    workDate = LocalDate(2025, 1, 1),
                    startTime = Instant.parse("2025-01-01T17:00:00Z"),
                    endTime = Instant.parse("2025-01-01T20:00:00Z"),
                    memo = null,
                    breaks = emptyList()
                )
            )
        }
    }
}
