package com.example.usecase

import com.example.TestFixtures
import com.example.at
import com.example.common.error.DomainValidationException
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.SpecialHourlyWageRepository
import com.example.usecase.TestAuditSupport
import com.example.usecase.shift.RegisterShiftUseCase
import com.example.usecase.shift.ShiftContextProvider
import com.example.usecase.shift.ShiftCreateContext
import com.example.usecase.shift.ShiftNotificationService
import com.example.usecase.shift.ShiftPermissionService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RegisterShiftUseCaseTest {

    private val repository = mockk<ShiftRepository>()
    private val specialRepository = mockk<SpecialHourlyWageRepository>(relaxed = true)
    private val contextProvider = mockk<ShiftContextProvider>()
    private val notificationService = mockk<ShiftNotificationService>(relaxed = true)
    private val permissionService = mockk<ShiftPermissionService>(relaxed = true)
    private val useCase = RegisterShiftUseCase(
        repository,
        specialRepository,
        contextProvider,
        notificationService,
        permissionService
    )

    private val start = LocalDate(2025, 1, 1).at("09:00")
    private val end = LocalDate(2025, 1, 1).at("18:00")
    private val actor = TestFixtures.user(id = 1)
    private val store = TestFixtures.store(id = 1)
    private val createContext = ShiftCreateContext(actor = actor, targetUser = actor, targetStore = store)

    @Test
    fun `creates shift when payload is valid`() = runTest {
        val shift = Shift(
            id = 1,
            userId = 1,
            storeId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = start,
            endTime = end,
            memo = "memo",
            breaks = listOf(
                ShiftBreak(
                    id = 10,
                    shiftId = 1,
                    breakStart = LocalDate(2025, 1, 1).at("12:00"),
                    breakEnd = LocalDate(2025, 1, 1).at("12:15")
                )
            ),
            createdAt = start,
            updatedAt = start
        )
        coEvery { contextProvider.forCreate(any(), any(), any()) } returns createContext
        every { permissionService.ensureCanCreate(createContext) } returns Unit
        coEvery { repository.getShiftsOnDate(any(), any()) } returns emptyList()
        coEvery { repository.insertShift(any()) } returns shift

        val result = useCase(
            RegisterShiftUseCase.Command(
                actorId = 1,
                targetUserId = 1,
                requestedStoreId = null,
                workDate = LocalDate(2025, 1, 1),
                startTime = start,
                endTime = end,
                memo = "memo",
                breaks = listOf(
                    RegisterShiftUseCase.BreakCommand(
                    breakStart = LocalDate(2025, 1, 1).at("12:00"),
                    breakEnd = LocalDate(2025, 1, 1).at("12:15")
                    )
                )
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(1, result.id)
        coVerify { repository.insertShift(any()) }
    }

    @Test
    fun `uses context store when storeId is omitted`() = runTest {
        val inserted = slot<Shift>()
        coEvery { contextProvider.forCreate(1, 1, null) } returns createContext
        every { permissionService.ensureCanCreate(createContext) } returns Unit
        coEvery { repository.getShiftsOnDate(any(), any()) } returns emptyList()
        coEvery { repository.insertShift(capture(inserted)) } answers { inserted.captured.copy(id = 2) }

        useCase(
            RegisterShiftUseCase.Command(
                actorId = 1,
                targetUserId = 1,
                requestedStoreId = null,
                workDate = LocalDate(2025, 1, 2),
                startTime = start,
                endTime = end,
                memo = "memo",
                breaks = emptyList()
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(createContext.targetStore.id, inserted.captured.storeId)
    }

    @Test
    fun `throws when break overlaps shift start`() = runTest {
        coEvery { contextProvider.forCreate(any(), any(), any()) } returns createContext
        every { permissionService.ensureCanCreate(createContext) } returns Unit
        coEvery { repository.getShiftsOnDate(any(), any()) } returns emptyList()

        assertFailsWith<DomainValidationException> {
            useCase(
                RegisterShiftUseCase.Command(
                    actorId = 1,
                    targetUserId = 1,
                    requestedStoreId = null,
                    workDate = LocalDate(2025, 1, 1),
                    startTime = start,
                    endTime = end,
                    memo = null,
                    breaks = listOf(
                        RegisterShiftUseCase.BreakCommand(
                        breakStart = LocalDate(2025, 1, 1).at("08:00"),
                        breakEnd = LocalDate(2025, 1, 1).at("09:30")
                        )
                    )
                ),
                TestAuditSupport.auditContext
            )
        }
    }

    @Test
    fun `throws when shift overlaps existing`() = runTest {
        val existing = Shift(
            id = 2,
            userId = 1,
            storeId = 1,
            workDate = LocalDate(2025, 1, 1),
            startTime = start,
            endTime = end,
            memo = null,
            breaks = emptyList(),
            createdAt = start,
            updatedAt = end
        )
        coEvery { contextProvider.forCreate(any(), any(), any()) } returns createContext
        every { permissionService.ensureCanCreate(createContext) } returns Unit
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 1, 1)) } returns listOf(existing)

        val error = assertFailsWith<DomainValidationException> {
            useCase(
                RegisterShiftUseCase.Command(
                    actorId = 1,
                    targetUserId = 1,
                    requestedStoreId = null,
                    workDate = LocalDate(2025, 1, 1),
                    startTime = LocalDate(2025, 1, 1).at("17:00"),
                    endTime = LocalDate(2025, 1, 1).at("20:00"),
                    memo = null,
                    breaks = emptyList()
                ),
                TestAuditSupport.auditContext
            )
        }
        val violation = error.violations.first { it.code == "OVERLAP" }
        assertEquals("OVERLAP", violation.code)
        assertEquals("timeRange", violation.field)
    }

    @Test
    fun `allows back to back shifts without overlap`() = runTest {
        val existing = Shift(
            id = 2,
            userId = 1,
            storeId = 1,
            workDate = LocalDate(2025, 2, 1),
            startTime = LocalDate(2025, 2, 1).at("00:00"),
            endTime = LocalDate(2025, 2, 1).at("03:00"),
            memo = null,
            breaks = emptyList(),
            createdAt = start,
            updatedAt = end
        )
        val created = existing.copy(
            id = 3,
            startTime = LocalDate(2025, 2, 1).at("03:00"),
            endTime = LocalDate(2025, 2, 1).at("06:00")
        )
        coEvery { contextProvider.forCreate(any(), any(), any()) } returns createContext
        every { permissionService.ensureCanCreate(createContext) } returns Unit
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 2, 1)) } returns listOf(existing)
        coEvery { repository.insertShift(any()) } returns created

        val result = useCase(
            RegisterShiftUseCase.Command(
                actorId = 1,
                targetUserId = 1,
                requestedStoreId = null,
                workDate = LocalDate(2025, 2, 1),
                startTime = LocalDate(2025, 2, 1).at("03:00"),
                endTime = LocalDate(2025, 2, 1).at("06:00"),
                memo = null,
                breaks = emptyList()
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(3, result.id)
    }

    @Test
    fun `detects overlap when existing shift crosses midnight`() = runTest {
        val existing = Shift(
            id = 4,
            userId = 1,
            storeId = 1,
            workDate = LocalDate(2025, 3, 1),
            startTime = LocalDate(2025, 3, 1).at("18:00"),
            endTime = LocalDate(2025, 3, 2).at("02:00"),
            memo = null,
            breaks = emptyList(),
            createdAt = start,
            updatedAt = end
        )
        coEvery { contextProvider.forCreate(any(), any(), any()) } returns createContext
        every { permissionService.ensureCanCreate(createContext) } returns Unit
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 3, 1)) } returns listOf(existing)

        val error = assertFailsWith<DomainValidationException> {
            useCase(
                RegisterShiftUseCase.Command(
                    actorId = 1,
                    targetUserId = 1,
                    requestedStoreId = null,
                    workDate = LocalDate(2025, 3, 1),
                    startTime = LocalDate(2025, 3, 1).at("23:00"),
                    endTime = LocalDate(2025, 3, 2).at("04:00"),
                    memo = null,
                    breaks = emptyList()
                ),
                TestAuditSupport.auditContext
            )
        }
        assertEquals("OVERLAP", error.violations.first { it.code == "OVERLAP" }.code)
    }

    @Test
    fun `allows shift that starts exactly when existing one ends even across midnight`() = runTest {
        val existing = Shift(
            id = 5,
            userId = 1,
            storeId = 1,
            workDate = LocalDate(2025, 4, 1),
            startTime = LocalDate(2025, 4, 1).at("10:00"),
            endTime = LocalDate(2025, 4, 1).at("22:00"),
            memo = null,
            breaks = emptyList(),
            createdAt = start,
            updatedAt = end
        )
        val created = existing.copy(
            id = 6,
            startTime = LocalDate(2025, 4, 1).at("22:00"),
            endTime = LocalDate(2025, 4, 2).at("02:00")
        )
        coEvery { contextProvider.forCreate(any(), any(), any()) } returns createContext
        every { permissionService.ensureCanCreate(createContext) } returns Unit
        coEvery { repository.getShiftsOnDate(1, LocalDate(2025, 4, 1)) } returns listOf(existing)
        coEvery { repository.insertShift(any()) } returns created

        val result = useCase(
            RegisterShiftUseCase.Command(
                actorId = 1,
                targetUserId = 1,
                requestedStoreId = null,
                workDate = LocalDate(2025, 4, 1),
                startTime = LocalDate(2025, 4, 1).at("22:00"),
                endTime = LocalDate(2025, 4, 2).at("02:00"),
                memo = null,
                breaks = emptyList()
            ),
            TestAuditSupport.auditContext
        )

        assertEquals(6, result.id)
    }

}
