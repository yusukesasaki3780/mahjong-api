package com.example.usecase.shift

import com.example.TestFixtures
import com.example.common.error.AccessDeniedException
import com.example.domain.model.Shift
import com.example.domain.model.ShiftRequirement
import com.example.domain.model.ShiftSlotType
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.ShiftRequirementRepository
import com.example.domain.repository.UserRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class GetShiftBoardUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val shiftRepository = mockk<ShiftRepository>()
    private val requirementRepository = mockk<ShiftRequirementRepository>()
    private val contextProvider = mockk<ShiftContextProvider>()
    private val permissionService = mockk<ShiftPermissionService>()
    private val timeZone = TimeZone.UTC
    private val useCase = GetShiftBoardUseCase(
        userRepository,
        shiftRepository,
        requirementRepository,
        contextProvider,
        permissionService,
        timeZone = timeZone
    )

    private val store = TestFixtures.store(id = 10)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            userRepository,
            shiftRepository,
            requirementRepository,
            contextProvider,
            permissionService,
            answers = true
        )
    }

    @Test
    fun `returns board data for store`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 1, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(1, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns listOf(
            TestFixtures.user(id = 2, storeId = 10)
        )
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        coEvery {
            shiftRepository.getShiftsByStore(10, any(), any())
        } returns listOf(
            TestFixtures.shift(id = 5, userId = 2, storeId = 10)
        )
        coEvery {
            requirementRepository.findByStoreAndDateRange(10, any(), any())
        } returns listOf(
            ShiftRequirement(
                id = 100,
                storeId = 10,
                targetDate = LocalDate.parse("2025-01-01"),
                shiftType = ShiftSlotType.EARLY,
                startRequired = 2,
                endRequired = 1,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        )

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 1,
                storeId = 10,
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-07"),
                includeDeletedUsers = true
            )
        )

        assertEquals(2, result.users.size)
        val sortedUsers = result.users.sortedBy { it.id }
        assertEquals(listOf(1L, 2L), sortedUsers.map { it.id })
        assertEquals(listOf(100001, 100002), sortedUsers.map { it.zooId })
        assertEquals(1, result.shifts.size)
        val dayCount = result.startDate.daysUntil(result.endDate) + 1
        assertEquals(dayCount * ShiftSlotType.values().size, result.requirements.size)
        assertEquals(true, result.editable)
    }

    @Test
    fun `non admin board is not editable`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 3, storeId = 10, isAdmin = false)
        coEvery { contextProvider.forStoreView(3, 10) } returns storeContext(actor, editable = false, canIncludeDeleted = false)
        every { permissionService.ensureCanView(any()) } returns Unit
        coEvery { userRepository.listUsers(10, includeDeleted = false, includeAdmins = false) } returns listOf(
            TestFixtures.user(id = 3, storeId = 10, isAdmin = false)
        )
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns emptyList()
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 3,
                storeId = 10,
                startDate = LocalDate.parse("2025-02-01"),
                endDate = LocalDate.parse("2025-02-07"),
                includeDeletedUsers = false
            )
        )

        assertEquals(false, result.editable)
        assertEquals(1, result.users.size)
        assertEquals(actor.zooId, result.users.first().zooId)
    }

    @Test
    fun `admin can view other store but cannot edit`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 4, storeId = 1, isAdmin = true)
        coEvery { contextProvider.forStoreView(4, 10) } returns storeContext(actor, editable = false, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns emptyList()
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns emptyList()
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 4,
                storeId = 10,
                startDate = LocalDate.parse("2025-03-01"),
                endDate = LocalDate.parse("2025-03-07"),
                includeDeletedUsers = true
            )
        )

        assertEquals(false, result.editable)
        assertEquals(0, result.users.size)
    }

    @Test
    fun `non admin without shift cannot view other store`() = kotlinx.coroutines.test.runTest {
        coEvery { contextProvider.forStoreView(1, 10) } returns storeContext(
            actor = TestFixtures.user(id = 1, storeId = 1, isAdmin = false),
            store = store.copy(id = 10),
            editable = false,
            canIncludeDeleted = false,
            accessibleStores = emptyList()
        )
        every { permissionService.ensureCanView(any()) } throws AccessDeniedException("forbidden")

        assertFailsWith<AccessDeniedException> {
            useCase(
                GetShiftBoardUseCase.Command(
                    actorId = 1,
                    storeId = 10,
                    startDate = LocalDate.parse("2025-01-01"),
                    endDate = LocalDate.parse("2025-01-07"),
                    includeDeletedUsers = false
                )
            )
        }
    }

    @Test
    fun `helper user is included when shift exists`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 6, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(6, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns emptyList()
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns listOf(
            TestFixtures.shift(id = 70, userId = 99, storeId = 10)
        )
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()
        coEvery { userRepository.findByIds(setOf(99L)) } returns listOf(
            TestFixtures.user(id = 99, storeId = 55, isAdmin = false)
        )

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 6,
                storeId = 10,
                startDate = LocalDate.parse("2025-06-01"),
                endDate = LocalDate.parse("2025-06-07"),
                includeDeletedUsers = true
            )
        )

        assertEquals(true, result.users.any { it.id == 99L })
    }

    @Test
    fun `shift starting before ten but mostly daytime is EARLY`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 20, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(20, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        val member = TestFixtures.user(id = 21, storeId = 10)
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns listOf(member)
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns listOf(
            customShift(
                id = 500,
                userId = member.id!!,
                start = "2025-01-01T09:00:00Z",
                end = "2025-01-01T18:00:00Z"
            )
        )
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 20,
                storeId = 10,
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-07"),
                includeDeletedUsers = true
            )
        )

        assertEquals(ShiftSlotType.EARLY, result.shifts.single().shiftType)
    }

    @Test
    fun `overnight tie prefers EARLY`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 30, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(30, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        val member = TestFixtures.user(id = 31, storeId = 10)
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns listOf(member)
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns listOf(
            customShift(
                id = 600,
                userId = member.id!!,
                start = "2025-01-01T18:00:00Z",
                end = "2025-01-02T02:00:00Z"
            )
        )
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 30,
                storeId = 10,
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-07"),
                includeDeletedUsers = true
            )
        )

        assertEquals(ShiftSlotType.EARLY, result.shifts.single().shiftType)
    }

    @Test
    fun `early morning shift is LATE`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 40, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(40, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        val member = TestFixtures.user(id = 41, storeId = 10)
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns listOf(member)
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns listOf(
            customShift(
                id = 700,
                userId = member.id!!,
                start = "2025-01-01T05:00:00Z",
                end = "2025-01-01T10:00:00Z"
            )
        )
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 40,
                storeId = 10,
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-07"),
                includeDeletedUsers = true
            )
        )

        assertEquals(ShiftSlotType.LATE, result.shifts.single().shiftType)
    }

    @Test
    fun `late night heavy shift is LATE`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 50, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(50, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        val member = TestFixtures.user(id = 51, storeId = 10)
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns listOf(member)
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns listOf(
            customShift(
                id = 800,
                userId = member.id!!,
                start = "2025-01-01T21:00:00Z",
                end = "2025-01-02T01:00:00Z"
            )
        )
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 50,
                storeId = 10,
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-07"),
                includeDeletedUsers = true
            )
        )

        assertEquals(ShiftSlotType.LATE, result.shifts.single().shiftType)
    }

    @Test
    fun `fills missing requirements for every day and slot`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 70, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(70, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        val member = TestFixtures.user(id = 71, storeId = 10)
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns listOf(member)
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        val startDate = LocalDate.parse("2025-01-01")
        val endDate = startDate.plus(1, DateTimeUnit.DAY)
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns listOf(
            customShift(
                id = 910,
                userId = member.id!!,
                start = "2025-01-01T10:00:00Z",
                end = "2025-01-01T22:00:00Z"
            )
        )
        val now = Clock.System.now()
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns listOf(
            ShiftRequirement(
                id = 2000,
                storeId = 10,
                targetDate = startDate,
                shiftType = ShiftSlotType.EARLY,
                startRequired = 4,
                endRequired = 4,
                createdAt = now,
                updatedAt = now
            )
        )

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 70,
                storeId = 10,
                startDate = startDate,
                endDate = endDate,
                includeDeletedUsers = true
            )
        )

        assertEquals(4, result.requirements.size)
        val day1Early = result.requirements.first { it.targetDate == startDate && it.shiftType == ShiftSlotType.EARLY }
        assertEquals(4, day1Early.startRequired)
        assertEquals(1, day1Early.startActual)
        assertEquals(1, day1Early.endActual)

        val day1Late = result.requirements.first { it.targetDate == startDate && it.shiftType == ShiftSlotType.LATE }
        assertEquals(4, day1Late.startRequired)
        assertEquals(3, day1Late.endRequired)
        assertEquals(0, day1Late.startActual)

        val day2Late = result.requirements.first { it.targetDate == endDate && it.shiftType == ShiftSlotType.LATE }
        assertEquals(4, day2Late.startRequired)
        assertEquals(0, day2Late.startActual)
    }

    @Test
    fun `calculates requirement actual counts for checkpoints`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 60, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(60, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        val members = (1L..6L).map { memberId ->
            TestFixtures.user(id = 100 + memberId, storeId = 10)
        }
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns members
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        val shifts = listOf(
            customShift(
                id = 900,
                userId = members[0].id!!,
                start = "2025-01-01T09:00:00Z",
                end = "2025-01-01T18:00:00Z"
            ),
            customShift(
                id = 901,
                userId = members[1].id!!,
                start = "2025-01-01T10:00:00Z",
                end = "2025-01-01T22:00:00Z"
            ),
            customShift(
                id = 902,
                userId = members[2].id!!,
                start = "2025-01-01T12:00:00Z",
                end = "2025-01-01T20:00:00Z"
            ),
            customShift(
                id = 903,
                userId = members[3].id!!,
                start = "2025-01-01T22:00:00Z",
                end = "2025-01-02T10:00:00Z"
            ),
            customShift(
                id = 904,
                userId = members[4].id!!,
                start = "2025-01-01T20:00:00Z",
                end = "2025-01-02T04:00:00Z"
            ),
            customShift(
                id = 905,
                userId = members[5].id!!,
                start = "2025-01-01T23:00:00Z",
                end = "2025-01-02T05:00:00Z"
            )
        )
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns shifts
        val now = Clock.System.now()
        val targetDate = LocalDate.parse("2025-01-01")
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns listOf(
            ShiftRequirement(
                id = 1000,
                storeId = 10,
                targetDate = targetDate,
                shiftType = ShiftSlotType.EARLY,
                startRequired = 3,
                endRequired = 2,
                createdAt = now,
                updatedAt = now
            ),
            ShiftRequirement(
                id = 1001,
                storeId = 10,
                targetDate = targetDate,
                shiftType = ShiftSlotType.LATE,
                startRequired = 2,
                endRequired = 2,
                createdAt = now,
                updatedAt = now
            )
        )

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 60,
                storeId = 10,
                startDate = targetDate,
                endDate = targetDate,
                includeDeletedUsers = true
            )
        )

        val early = result.requirements.first { it.shiftType == ShiftSlotType.EARLY }
        assertEquals(2, early.startActual)
        assertEquals(1, early.endActual)

        val late = result.requirements.first { it.shiftType == ShiftSlotType.LATE }
        assertEquals(2, late.startActual)
        assertEquals(1, late.endActual)
    }

    @Test
    fun `applies weekday-based default requirements when db record missing`() = kotlinx.coroutines.test.runTest {
        val actor = TestFixtures.user(id = 80, storeId = 10, isAdmin = true)
        coEvery { contextProvider.forStoreView(80, 10) } returns storeContext(actor, editable = true, canIncludeDeleted = true)
        every { permissionService.ensureCanView(any()) } returns Unit
        coEvery { userRepository.listUsers(10, includeDeleted = true, includeAdmins = false) } returns emptyList()
        coEvery { userRepository.findByIds(any()) } returns emptyList()
        val saturday = LocalDate.parse("2025-01-04") // Saturday
        val sunday = saturday.plus(1, DateTimeUnit.DAY)
        coEvery { shiftRepository.getShiftsByStore(10, any(), any()) } returns emptyList()
        coEvery { requirementRepository.findByStoreAndDateRange(10, any(), any()) } returns emptyList()

        val result = useCase(
            GetShiftBoardUseCase.Command(
                actorId = 80,
                storeId = 10,
                startDate = saturday,
                endDate = sunday,
                includeDeletedUsers = true
            )
        )

        val satEarly = result.requirements.first { it.targetDate == saturday && it.shiftType == ShiftSlotType.EARLY }
        assertEquals(4, satEarly.startRequired)
        assertEquals(4, satEarly.endRequired)

        val satLate = result.requirements.first { it.targetDate == saturday && it.shiftType == ShiftSlotType.LATE }
        assertEquals(5, satLate.startRequired)
        assertEquals(4, satLate.endRequired)

        val sunLate = result.requirements.first { it.targetDate == sunday && it.shiftType == ShiftSlotType.LATE }
        assertEquals(4, sunLate.startRequired)
        assertEquals(3, sunLate.endRequired)
    }

    private fun storeContext(
        actor: com.example.domain.model.User,
        store: com.example.domain.model.StoreMaster = this.store,
        editable: Boolean,
        canIncludeDeleted: Boolean,
        accessibleStores: List<com.example.domain.model.StoreMaster> = listOf(store)
    ): ShiftViewContext = ShiftViewContext(
        actor = actor,
        targetUsers = emptyList(),
        targetStores = accessibleStores,
        requestedUser = null,
        requestedStore = store,
        scope = ShiftViewContext.Scope.STORE,
        editable = editable,
        canIncludeDeletedUsers = canIncludeDeleted,
        viewableStoreIds = accessibleStores.mapNotNull { it.id }.toSet(),
        viewableUserIds = if (actor.isAdmin) emptySet() else setOfNotNull(actor.id)
    )

    private fun customShift(
        id: Long,
        userId: Long,
        start: String,
        end: String,
        storeId: Long = 10
    ): Shift {
        val startInstant = Instant.parse(start)
        val endInstant = Instant.parse(end)
        val workDate = startInstant.toLocalDateTime(timeZone).date
        return Shift(
            id = id,
            userId = userId,
            storeId = storeId,
            workDate = workDate,
            startTime = startInstant,
            endTime = endInstant,
            memo = null,
            specialHourlyWage = null,
            breaks = emptyList(),
            createdAt = startInstant,
            updatedAt = startInstant
        )
    }
}
