package com.example.infrastructure.db.repository

import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.repository.GameResultPatch
import com.example.infrastructure.db.tables.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExposedGameResultRepositoryIntegrationTest : RepositoryTestBase() {

    private val repository = ExposedGameResultRepository()

    @Test
    fun `insert and patch game result`() = runDbTest {
        val userId = seedUser()
        val now = Clock.System.now()
        val created = repository.insertGameResult(
            GameResult(
                id = null,
                userId = userId,
                gameType = GameType.SANMA,
                playedAt = now,
                place = 1,
                baseIncome = 1000,
                tipCount = 2,
                tipIncome = 200,
                otherIncome = 0,
                totalIncome = 1200,
                isFinalIncomeRecord = false,
                createdAt = now,
                updatedAt = now
            )
        )

        val patched = repository.patchGameResult(
            userId = userId,
            resultId = created.id!!,
            patch = GameResultPatch(
                place = 2,
                totalIncome = 900,
                updatedAt = now.plus(1, kotlinx.datetime.DateTimeUnit.MINUTE)
            )
        )
        assertEquals(2, patched.place)
        assertEquals(900, patched.totalIncome)
        assertEquals(GameType.SANMA, patched.gameType)
    }

    @Test
    fun `delete game result cascades`() = runDbTest {
        val userId = seedUser()
        val now = Clock.System.now()
        val created = repository.insertGameResult(
            GameResult(
                id = null,
                userId = userId,
                gameType = GameType.YONMA,
                playedAt = now,
                place = 3,
                baseIncome = 800,
                tipCount = 1,
                tipIncome = 100,
                otherIncome = 0,
                totalIncome = 900,
                isFinalIncomeRecord = false,
                createdAt = now,
                updatedAt = now
            )
        )

        val deleted = repository.deleteGameResult(created.id!!)
        assertTrue(deleted)
    }

    private fun seedUser(): Long {
        val now = Clock.System.now()
        return transaction {
            UsersTable.insert {
                it[name] = "Player"
                it[nickname] = "p"
                it[storeName] = "Store"
                it[prefectureCode] = "01"
                it[email] = "player-${now.toEpochMilliseconds()}@example.com"
                it[zooId] = (now.toEpochMilliseconds() % 900_000).toInt() + 1
                it[createdAt] = now
                it[updatedAt] = now
            } get UsersTable.userId
        }
    }
}
