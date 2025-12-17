package com.example.usecase

import com.example.TestFixtures
import com.example.at
import com.example.common.error.DomainValidationException
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.SpecialHourlyWageRepository
import com.example.usecase.TestAuditSupport
import com.example.usecase.shift.EditShiftUseCase
import com.example.usecase.shift.ShiftContextProvider
import com.example.usecase.shift.ShiftNotificationService
import com.example.usecase.shift.ShiftPermissionService
import com.example.usecase.shift.ShiftUpdateContext
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
    private val specialRepository = mockk<SpecialHourlyWageRepository>(relaxed = true)
    private val notificationService = mockk<ShiftNotificationService>(relaxed = true)
    private val contextProvider = mockk<ShiftContextProvider>()
    private val permissionService = mockk<ShiftPermissionService>(relaxed = true)
    private val useCase = EditShiftUseCase(
        repository,
        specialRepository,
        TestAuditSupport.auditLogger,
        notificationService,
        contextProvider,
        permissionService
    )

    private val start = LocalDate(2025, 1, 1).at("09:00")
    private val end = LocalDate(2025, 1, 1).at("18:00")
    private val actor = TestFixtures.user(id = 1)
    private val store = TestFixtures.store(id = 1)

    @Test
    fun `updates shift when payload is valid`() = runTest {
        val shift = sampleShift()
        coEvery { contextProvider.forUpdate(any(), any()) } returns ShiftUpdateContext(actor, actor, store, shift)
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns emptyList()
        coEvery { repository.updateShift(any()) } returns shift

        val result = useCase(
            EditShiftUseCase.Command(
                actorId = 1,
                shiftId = 10,
                workDate = LocalDate(2025, 1, 1),
                startTime = start,
                endTime = end,
                memo = "memo",
                breaks = listOf(
                    EditShiftUseCase.BreakCommand(
                        breakStart = LocalDate(2025, 1, 1).at("12:00"),
                        breakEnd = LocalDate(2025, 1, 1).at("12:15")
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
        val shift = sampleShift()
        val overlapping = shift.copy(id = 20)
        coEvery { contextProvider.forUpdate(any(), any()) } returns ShiftUpdateContext(actor, actor, store, shift)
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns listOf(overlapping)

        val error = assertFailsWith<DomainValidationException> {
            useCase(
                EditShiftUseCase.Command(
                    actorId = 1,
                    shiftId = 10,
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
        assertEquals("OVERLAP", error.violations.first { it.code == "OVERLAP" }.code)
    }

    @Test
    fun `throws when end before start`() = runTest {
        val shift = sampleShift()
        coEvery { contextProvider.forUpdate(any(), any()) } returns ShiftUpdateContext(actor, actor, store, shift)
        coEvery { repository.getShiftsOnDate(any(), any()) } returns emptyList()

        assertFailsWith<ConstraintViolationException> {
            useCase(
                EditShiftUseCase.Command(
                    actorId = 1,
                    shiftId = 10,
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
        val breakStart = LocalDate(2025, 1, 1).at("12:00")
        val breakEnd = LocalDate(2025, 1, 1).at("12:15")
        return Shift(
            id = 10,
            userId = 1,
            storeId = 1,
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
