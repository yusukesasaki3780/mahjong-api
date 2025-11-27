package com.example.infrastructure.db.repository

import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.model.StatsRange
import com.example.domain.repository.GameResultPatch
import com.example.domain.repository.GameResultRepository
import com.example.infrastructure.db.tables.GameResultsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.avg

/**
 * GameResultRepository を Exposed で実装し、成績の CRUD と統計を担う。
 */
class ExposedGameResultRepository : GameResultRepository {

    override suspend fun insertGameResult(result: GameResult): GameResult = dbQuery {
        val insertedId = GameResultsTable.insert { row ->
            row[userId] = result.userId
            row[gameType] = result.gameType.name
            row[playedAt] = result.playedAt
            row[place] = result.place
            row[baseIncome] = result.baseIncome.toInt()
            row[tipCount] = result.tipCount
            row[tipIncome] = result.tipIncome.toInt()
            row[totalIncome] = result.totalIncome.toInt()
            row[note] = result.note
            row[createdAt] = result.createdAt
            row[updatedAt] = result.updatedAt
        } get GameResultsTable.id

        fetchById(insertedId)
    }

    override suspend fun updateGameResult(result: GameResult): GameResult = dbQuery {
        val targetId = result.id ?: error("GameResult id is required for update.")
        GameResultsTable.update({ GameResultsTable.id eq targetId }) { row ->
            row[userId] = result.userId
            row[gameType] = result.gameType.name
            row[playedAt] = result.playedAt
            row[place] = result.place
            row[baseIncome] = result.baseIncome.toInt()
            row[tipCount] = result.tipCount
            row[tipIncome] = result.tipIncome.toInt()
            row[totalIncome] = result.totalIncome.toInt()
            row[note] = result.note
            row[createdAt] = result.createdAt
            row[updatedAt] = result.updatedAt
        }

        fetchById(targetId)
    }

    override suspend fun patchGameResult(userId: Long, resultId: Long, patch: GameResultPatch): GameResult = dbQuery {
        val updated = GameResultsTable.update({
            (GameResultsTable.id eq resultId) and (GameResultsTable.userId eq userId)
        }) { row ->
            patch.gameType?.let { row[gameType] = it.name }
            patch.playedAt?.let { row[playedAt] = it }
            patch.place?.let { row[place] = it }
            patch.baseIncome?.let { row[baseIncome] = it.toInt() }
            patch.tipCount?.let { row[tipCount] = it }
            patch.tipIncome?.let { row[tipIncome] = it.toInt() }
            patch.totalIncome?.let { row[totalIncome] = it.toInt() }
            patch.note?.let { row[note] = it }
            patch.updatedAt?.let { row[updatedAt] = it }
        }
        if (updated == 0) {
            throw IllegalArgumentException("Game result not found or not owned by user.")
        }
        fetchById(resultId)
    }

    override suspend fun deleteGameResult(resultId: Long): Boolean = dbQuery {
        GameResultsTable.deleteWhere { GameResultsTable.id eq resultId } > 0
    }

    override suspend fun findById(resultId: Long): GameResult? = dbQuery {
        GameResultsTable
            .select { GameResultsTable.id eq resultId }
            .map(::toGameResult)
            .singleOrNull()
    }

    override suspend fun getUserResults(userId: Long, range: StatsRange): List<GameResult> = dbQuery {
        GameResultsTable
            .select {
                (GameResultsTable.userId eq userId) and
                    (GameResultsTable.playedAt greaterEq range.start) and
                    (GameResultsTable.playedAt less range.end)
            }
            .map(::toGameResult)
            .sortedBy { it.playedAt }
    }

    override suspend fun getAveragePlace(userId: Long, range: StatsRange): Double? = dbQuery {
        val avgPlace = GameResultsTable.place.avg()
        GameResultsTable
            .slice(avgPlace)
            .select {
                (GameResultsTable.userId eq userId) and
                    (GameResultsTable.playedAt greaterEq range.start) and
                    (GameResultsTable.playedAt less range.end)
            }
            .singleOrNull()
            ?.get(avgPlace)
            ?.toDouble()
    }

    override suspend fun getTotalIncome(userId: Long, range: StatsRange): Long = dbQuery {
        GameResultsTable
            .slice(GameResultsTable.totalIncome.sum())
            .select {
                (GameResultsTable.userId eq userId) and
                    (GameResultsTable.playedAt greaterEq range.start) and
                    (GameResultsTable.playedAt less range.end)
            }
            .singleOrNull()
            ?.get(GameResultsTable.totalIncome.sum())
            ?.toLong()
            ?: 0L
    }

    private fun fetchById(id: Long): GameResult =
        GameResultsTable
            .select { GameResultsTable.id eq id }
            .single()
            .let(::toGameResult)

    /**
     * ResultRow -> GameResult 変換。
     */
    private fun toGameResult(row: ResultRow): GameResult =
        GameResult(
            id = row[GameResultsTable.id],
            userId = row[GameResultsTable.userId],
            gameType = GameType.valueOf(row[GameResultsTable.gameType]),
            playedAt = row[GameResultsTable.playedAt],
            place = row[GameResultsTable.place],
            baseIncome = row[GameResultsTable.baseIncome].toLong(),
            tipCount = row[GameResultsTable.tipCount],
            tipIncome = row[GameResultsTable.tipIncome].toLong(),
            totalIncome = row[GameResultsTable.totalIncome].toLong(),
            note = row[GameResultsTable.note],
            createdAt = row[GameResultsTable.createdAt],
            updatedAt = row[GameResultsTable.updatedAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
