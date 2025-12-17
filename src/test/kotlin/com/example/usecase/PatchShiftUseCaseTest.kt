package com.example.usecase

import com.example.TestFixtures
import com.example.at
import com.example.common.error.DomainValidationException
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.SpecialHourlyWageRepository
import com.example.usecase.TestAuditSupport
import com.example.usecase.shift.PatchShiftUseCase
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

class PatchShiftUseCaseTest {

    private val repository = mockk<ShiftRepository>()
    private val specialRepository = mockk<SpecialHourlyWageRepository>(relaxed = true)
    private val notificationService = mockk<ShiftNotificationService>(relaxed = true)
    private val contextProvider = mockk<ShiftContextProvider>()
    private val permissionService = mockk<ShiftPermissionService>(relaxed = true)
    private val useCase = PatchShiftUseCase(
        repository,
        specialRepository,
        TestAuditSupport.auditLogger,
        notificationService,
        contextProvider,
        permissionService
    )

    private val actor = TestFixtures.user(id = 1)
    private val store = TestFixtures.store(id = 1)

    @Test
    fun `forwards patch when payload is valid`() = runTest {
        val before = sampleShift("old")
        val after = before.copy(memo = "patched")
        stubContext(before)
        coEvery { repository.getShiftsOnDate(1, before.workDate) } returns emptyList()
        coEvery { repository.patchShift(eq(1), eq(10), any()) } returns after

        val result = useCase(
            PatchShiftUseCase.Command(
                actorId = 1,
                shiftId = 10,
                memo = "patched",
                startTime = LocalDate(2025, 1, 1).at("09:00"),
                endTime = LocalDate(2025, 1, 1).at("18:00"),
                breaks = listOf(
                    PatchShiftUseCase.BreakPatchCommand(
                        id = null,
                        breakStart = LocalDate(2025, 1, 1).at("11:00"),
                        breakEnd = LocalDate(2025, 1, 1).at("11:15")
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
        val before = sampleShift("old")
        stubContext(before)
        coEvery { repository.getShiftsOnDate(1, before.workDate) } returns emptyList()

        assertFailsWith<DomainValidationException> {
            useCase(
                PatchShiftUseCase.Command(
                    actorId = 1,
                    shiftId = 10,
                    startTime = LocalDate(2025, 1, 1).at("09:00")
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `throws when new breaks overlap each other`() = runTest {
        val before = sampleShift("old")
        stubContext(before)
        coEvery { repository.getShiftsOnDate(1, before.workDate) } returns emptyList()

        assertFailsWith<DomainValidationException> {
            useCase(
                PatchShiftUseCase.Command(
                    actorId = 1,
                    shiftId = 10,
                    breaks = listOf(
                        PatchShiftUseCase.BreakPatchCommand(
                            id = null,
                            breakStart = LocalDate(2025, 1, 1).at("11:05"),
                            breakEnd = LocalDate(2025, 1, 1).at("11:20")
                        ),
                        PatchShiftUseCase.BreakPatchCommand(
                            id = null,
                            breakStart = LocalDate(2025, 1, 1).at("11:10"),
                            breakEnd = LocalDate(2025, 1, 1).at("11:25")
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
        stubContext(before)
        coEvery { repository.getShiftsOnDate(1, before.workDate) } returns emptyList()
        coEvery { repository.patchShift(eq(1), eq(10), any()) } returns before.copy(memo = "updated")

        val result = useCase(
            PatchShiftUseCase.Command(
                actorId = 1,
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
            startTime = LocalDate(2025, 11, 26).at("13:00"),
            endTime = LocalDate(2025, 11, 27).at("01:00"),
            breaks = listOf(
                ShiftBreak(
                    id = 50,
                    shiftId = 10,
                    breakStart = LocalDate(2025, 11, 26).at("14:00"),
                    breakEnd = LocalDate(2025, 11, 26).at("14:30")
                )
            )
        )
        stubContext(before)
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 11, 26)) } returns listOf(before)
        coEvery { repository.patchShift(eq(1), eq(10), any()) } returns before.copy(memo = "patched")

        val result = useCase(
            PatchShiftUseCase.Command(
                actorId = 1,
                shiftId = 10,
                workDate = LocalDate(2025, 11, 26),
                startTime = LocalDate(2025, 11, 26).at("14:00"),
                endTime = LocalDate(2025, 11, 27).at("01:00"),
                memo = "patched",
                breaks = listOf(
                    PatchShiftUseCase.BreakPatchCommand(
                        id = null,
                        breakStart = LocalDate(2025, 11, 26).at("15:30"),
                        breakEnd = LocalDate(2025, 11, 26).at("16:00")
                    )
                )
            ),
            TestAuditSupport.auditContext
        )

        assertEquals("patched", result.memo)
    }

    @Test
    fun `rejects time patch that overlaps existing shift`() = runTest {
        val before = sampleShift("old")
        val conflicting = before.copy(
            id = 99,
            startTime = LocalDate(2025, 1, 1).at("07:00"),
            endTime = LocalDate(2025, 1, 1).at("10:00")
        )
        stubContext(before)
        coEvery { repository.getShiftsOnDate(1, before.workDate) } returns listOf(conflicting)

        val error = assertFailsWith<DomainValidationException> {
            useCase(
                PatchShiftUseCase.Command(
                    actorId = 1,
                    shiftId = 10,
                    startTime = LocalDate(2025, 1, 1).at("08:00"),
                    endTime = LocalDate(2025, 1, 1).at("11:00")
                ),
                TestAuditSupport.auditContext
            )
        }
        assertEquals("OVERLAP", error.violations.first { it.code == "OVERLAP" }.code)
    }

    private fun stubContext(shift: Shift) {
        coEvery { contextProvider.forUpdate(any(), any()) } returns ShiftUpdateContext(actor, actor, store, shift)
    }

    private fun sampleShift(memo: String): Shift = Shift(
        id = 10,
        userId = 1,
        storeId = 1,
        workDate = LocalDate(2025, 1, 1),
        startTime = LocalDate(2025, 1, 1).at("09:00"),
        endTime = LocalDate(2025, 1, 1).at("18:00"),
        memo = memo,
        breaks = listOf(
            ShiftBreak(
                id = 5,
                shiftId = 10,
                breakStart = LocalDate(2025, 1, 1).at("13:00"),
                breakEnd = LocalDate(2025, 1, 1).at("13:15")
        )
        ),
        createdAt = LocalDate(2025, 1, 1).at("00:00"),
        updatedAt = LocalDate(2025, 1, 1).at("00:00")
    )
}
