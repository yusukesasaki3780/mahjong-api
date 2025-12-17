package com.example.usecase.shift

import com.example.common.error.AccessDeniedException
import com.example.common.error.DomainValidationException
import com.example.domain.model.GameType
import com.example.domain.model.RankingEntry
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.model.StatsPeriod
import com.example.domain.model.StoreMaster
import com.example.domain.model.User
import com.example.domain.repository.ShiftPatch
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.StoreMasterRepository
import com.example.domain.repository.UserPatch
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlinx.datetime.LocalDate
import kotlin.test.assertFailsWith

class ShiftContextProviderTest {

    private val now = Clock.System.now()
    private val storeA = StoreMaster(id = 10, storeName = "StoreA", createdAt = now, updatedAt = now)
    private val storeB = StoreMaster(id = 20, storeName = "StoreB", createdAt = now, updatedAt = now)

    private val admin = User(
        id = 1,
        name = "Admin",
        nickname = "Admin",
        storeId = storeA.id,
        storeName = storeA.storeName,
        prefectureCode = "01",
        email = "admin@example.com",
        zooId = 1000,
        isAdmin = true,
        isDeleted = false,
        createdAt = now,
        updatedAt = now
    )
    private val member = admin.copy(id = 2, name = "Member", nickname = "Member", email = "member@example.com", storeId = storeB.id, storeName = storeB.storeName, isAdmin = false)

    private val provider = ShiftContextProvider(
        userRepository = FakeUserRepository(mapOf(admin.id!! to admin, member.id!! to member)),
        shiftRepository = FakeShiftRepository(),
        storeMasterRepository = FakeStoreMasterRepository(mapOf(storeA.id to storeA, storeB.id to storeB))
    )

    @Test
    fun `admin without requested store uses own store for self shifts`() = runTest {
        val context = provider.forCreate(actorId = admin.id!!, targetUserId = admin.id!!, requestedStoreId = null)
        assertEquals(storeA.id, context.targetStore.id)
    }

    @Test
    fun `admin acting on other user without storeId gets required error`() = runTest {
        val error = assertFailsWith<DomainValidationException> {
            provider.forCreate(actorId = admin.id!!, targetUserId = member.id!!, requestedStoreId = null)
        }
        val violation = error.violations.single()
        assertEquals("storeId", violation.field)
        assertEquals("REQUIRED", violation.code)
    }

    @Test
    fun `non admin cannot specify storeId`() = runTest {
        assertFailsWith<AccessDeniedException> {
            provider.forCreate(actorId = member.id!!, targetUserId = member.id!!, requestedStoreId = storeB.id)
        }
    }

    @Test
    fun `admin with requested store uses that store`() = runTest {
        val context = provider.forCreate(actorId = admin.id!!, targetUserId = member.id!!, requestedStoreId = storeB.id)
        assertEquals(storeB.id, context.targetStore.id)
    }
}

private class FakeUserRepository(
    private val users: Map<Long, User>
) : UserRepository {
    override suspend fun findById(userId: Long): User? = users[userId]
    override suspend fun findByEmail(email: String) = notImplemented()
    override suspend fun findByZooId(zooId: Int) = notImplemented()
    override suspend fun createUser(user: User): User = notImplemented()
    override suspend fun updateUser(user: User): User = notImplemented()
    override suspend fun patchUser(userId: Long, patch: UserPatch): User = notImplemented()
    override suspend fun deleteUser(userId: Long): Boolean = notImplemented()
    override suspend fun restoreUser(userId: Long): Boolean = notImplemented()
    override suspend fun listNonAdminUsers(storeId: Long, includeDeleted: Boolean): List<User> = notImplemented()
    override suspend fun findByIds(ids: Collection<Long>): List<User> = ids.mapNotNull(users::get)
    override suspend fun findRanking(
        gameType: GameType,
        period: StatsPeriod
    ): List<RankingEntry> = notImplemented()

    private fun notImplemented(): Nothing = error("Not needed for tests")
}

private class FakeStoreMasterRepository(
    private val stores: Map<Long, StoreMaster>
) : StoreMasterRepository {
    override suspend fun getAll(): List<StoreMaster> = stores.values.toList()
    override suspend fun findById(id: Long): StoreMaster? = stores[id]
    override suspend fun findByIds(ids: Collection<Long>): List<StoreMaster> = ids.mapNotNull(stores::get)
}

private class FakeShiftRepository(
    private val memberships: Map<Long, Set<Long>> = emptyMap()
) : ShiftRepository {
    override suspend fun insertShift(shift: Shift) = notImplemented()
    override suspend fun updateShift(shift: Shift) = notImplemented()
    override suspend fun deleteShift(shiftId: Long): Boolean = notImplemented()
    override suspend fun getMonthlyShifts(userId: Long, yearMonth: YearMonth): List<Shift> = notImplemented()
    override suspend fun getShiftsOnDate(userId: Long, workDate: LocalDate): List<Shift> = notImplemented()
    override suspend fun getShiftsInRange(userId: Long, startDate: LocalDate, endDate: LocalDate): List<Shift> = notImplemented()
    override suspend fun getShiftsByStore(storeId: Long, startDate: LocalDate, endDate: LocalDate): List<Shift> = notImplemented()
    override suspend fun getShiftBreaks(shiftId: Long): List<ShiftBreak> = notImplemented()
    override suspend fun patchShift(userId: Long, shiftId: Long, patch: ShiftPatch): Shift = notImplemented()
    override suspend fun findById(shiftId: Long): Shift? = notImplemented()
    override suspend fun deleteAllForUser(userId: Long): Int = notImplemented()
    override suspend fun userHasShiftInStore(userId: Long, storeId: Long): Boolean = notImplemented()
    override suspend fun getStoreIdsForUser(userId: Long): Set<Long> = memberships[userId] ?: emptySet()

    private fun notImplemented(): Nothing = error("Not needed for tests")
}
