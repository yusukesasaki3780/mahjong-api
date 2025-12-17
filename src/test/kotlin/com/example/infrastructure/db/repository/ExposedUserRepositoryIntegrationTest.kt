package com.example.infrastructure.db.repository

import com.example.domain.model.GameType
import com.example.domain.model.StatsPeriod
import com.example.domain.model.User
import com.example.infrastructure.db.tables.GameResultsTable
import com.example.infrastructure.db.tables.UsersTable
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.absoluteValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExposedUserRepositoryIntegrationTest : RepositoryTestBase() {

    private val repository = ExposedUserRepository()

    @Test
    fun `create and fetch user`() = runDbTest {
        val created = repository.createUser(sampleUser(name = "Alice"))
        val fetched = repository.findById(created.id!!)
        assertEquals("Alice", fetched?.name)
        assertEquals(created.id, fetched?.id)
    }

    @Test
    fun `patch user updates only provided fields`() = runDbTest {
        val created = repository.createUser(sampleUser(name = "Bob", nickname = "b", store = "A"))
        val patched = repository.patchUser(
            created.id!!,
            com.example.domain.repository.UserPatch(
                name = "Bobby",
                nickname = null,
                storeName = "NewStore"
            )
        )
        assertEquals("Bobby", patched.name)
        assertEquals("b", patched.nickname) // unchanged
        assertEquals("NewStore", patched.storeName)
    }

    @Test
    fun `delete removes user`() = runDbTest {
        val created = repository.createUser(sampleUser(name = "Charlie"))
        val createdId = created.id!!
        val deleted = repository.deleteUser(createdId)
        assertTrue(deleted)
        val fetched = repository.findById(createdId)
        assertTrue(fetched?.isDeleted == true)
    }

    @Test
    fun `findRanking includes records without playedAt using createdAt`() = runDbTest {
        val start = Instant.parse("2025-01-01T00:00:00Z")
        val end = Instant.parse("2025-02-01T00:00:00Z")
        val period = StatsPeriod("weekly", start, end)
        val userId = transaction {
            UsersTable.insert {
                it[name] = "WeeklyUser"
                it[nickname] = "Weekly"
                it[storeId] = 1
                it[storeName] = "Store"
                it[prefectureCode] = "01"
                it[email] = "weekly@example.com"
                it[zooId] = 123456
                it[UsersTable.isAdmin] = false
                it[UsersTable.isDeleted] = false
                it[createdAt] = start
                it[updatedAt] = start
            } get UsersTable.userId
        }
        transaction {
            GameResultsTable.insert {
                it[GameResultsTable.userId] = userId
                it[gameType] = GameType.YONMA.name
                it[playedAt] = Instant.parse("2025-01-10T09:00:00Z")
                it[place] = 1
                it[baseIncome] = 1500
                it[tipCount] = 0
                it[tipIncome] = 0
                it[otherIncome] = 0
                it[totalIncome] = 1500
                it[note] = null
                it[isFinalIncomeRecord] = false
                it[storeId] = null
                it[simpleBatchId] = null
                it[createdAt] = Instant.parse("2025-01-10T09:00:00Z")
                it[updatedAt] = Instant.parse("2025-01-10T09:00:00Z")
            }
            GameResultsTable.insert {
                it[GameResultsTable.userId] = userId
                it[gameType] = GameType.YONMA.name
                it[playedAt] = null
                it[place] = 2
                it[baseIncome] = -500
                it[tipCount] = 0
                it[tipIncome] = 0
                it[otherIncome] = 0
                it[totalIncome] = -500
                it[note] = null
                it[isFinalIncomeRecord] = true
                it[storeId] = null
                it[simpleBatchId] = UUID.randomUUID()
                it[createdAt] = Instant.parse("2025-01-15T09:00:00Z")
                it[updatedAt] = Instant.parse("2025-01-15T09:00:00Z")
            }
        }

        val result = repository.findRanking(GameType.YONMA, period)
        assertEquals(1, result.size)
        val entry = result.first()
        assertEquals(userId, entry.userId)
        assertEquals(1000, entry.totalIncome)
        assertEquals(2, entry.gameCount)
    }

    private fun sampleUser(
        name: String,
        nickname: String = "nick",
        storeId: Long = 1,
        store: String = "Store",
        prefecture: String = "01",
        email: String = "$name@example.com",
        zooId: Int = ((name.hashCode().absoluteValue % 900000) + 1),
        isAdmin: Boolean = false,
        isDeleted: Boolean = false
    ): User {
        val now = Clock.System.now()
        return User(
            id = null,
            name = name,
            nickname = nickname,
            storeId = storeId,
            storeName = store,
            prefectureCode = prefecture,
            email = email,
            zooId = zooId,
            isAdmin = isAdmin,
            isDeleted = isDeleted,
            createdAt = now,
            updatedAt = now
        )
    }
}
