package com.example.infrastructure.db.repository

import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.model.StatsRange
import com.example.domain.repository.GameResultPatch
import com.example.domain.repository.GameResultRepository
import com.example.infrastructure.db.tables.GameResultsTable
import com.example.infrastructure.db.tables.StoreMasterTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * GameResultRepository �� Exposed �Ŏ������A���т� CRUD �Ɠ��v��S���B
 */
class ExposedGameResultRepository : GameResultRepository {

    override suspend fun insertGameResult(result: GameResult): GameResult = dbQuery {
        val insertedId = GameResultsTable.insert { row ->
            row[GameResultsTable.userId] = result.userId
            row[GameResultsTable.gameType] = result.gameType.name
            row[GameResultsTable.playedAt] = result.playedAt
            row[GameResultsTable.place] = result.place
            row[GameResultsTable.baseIncome] = result.baseIncome.toInt()
            row[GameResultsTable.tipCount] = result.tipCount
            row[GameResultsTable.tipIncome] = result.tipIncome.toInt()
            row[GameResultsTable.otherIncome] = result.otherIncome.toInt()
            row[GameResultsTable.storeId] = result.storeId
            row[GameResultsTable.simpleBatchId] = result.simpleBatchId
            row[GameResultsTable.totalIncome] = result.totalIncome.toInt()
            row[GameResultsTable.note] = result.note
            row[GameResultsTable.isFinalIncomeRecord] = result.isFinalIncomeRecord
            row[GameResultsTable.createdAt] = result.createdAt
            row[GameResultsTable.updatedAt] = result.updatedAt
        } get GameResultsTable.id

        fetchById(insertedId)
    }

    override suspend fun updateGameResult(result: GameResult): GameResult = dbQuery {
        val targetId = result.id ?: error("GameResult id is required for update.")
        GameResultsTable.update({ GameResultsTable.id eq targetId }) { row ->
            row[GameResultsTable.userId] = result.userId
            row[GameResultsTable.gameType] = result.gameType.name
            row[GameResultsTable.playedAt] = result.playedAt
            row[GameResultsTable.place] = result.place
            row[GameResultsTable.baseIncome] = result.baseIncome.toInt()
            row[GameResultsTable.tipCount] = result.tipCount
            row[GameResultsTable.tipIncome] = result.tipIncome.toInt()
            row[GameResultsTable.otherIncome] = result.otherIncome.toInt()
            row[GameResultsTable.storeId] = result.storeId
            row[GameResultsTable.simpleBatchId] = result.simpleBatchId
            row[GameResultsTable.totalIncome] = result.totalIncome.toInt()
            row[GameResultsTable.note] = result.note
            row[GameResultsTable.isFinalIncomeRecord] = result.isFinalIncomeRecord
            row[GameResultsTable.createdAt] = result.createdAt
            row[GameResultsTable.updatedAt] = result.updatedAt
        }

        fetchById(targetId)
    }

    override suspend fun patchGameResult(userId: Long, resultId: Long, patch: GameResultPatch): GameResult = dbQuery {
        val updated = GameResultsTable.update({
            (GameResultsTable.id eq resultId) and (GameResultsTable.userId eq userId)
        }) { row ->
            patch.gameType?.let { row[GameResultsTable.gameType] = it.name }
            patch.playedAt?.let { row[GameResultsTable.playedAt] = it }
            patch.place?.let { row[GameResultsTable.place] = it }
            patch.baseIncome?.let { row[GameResultsTable.baseIncome] = it.toInt() }
            patch.tipCount?.let { row[GameResultsTable.tipCount] = it }
            patch.tipIncome?.let { row[GameResultsTable.tipIncome] = it.toInt() }
            patch.otherIncome?.let { row[GameResultsTable.otherIncome] = it.toInt() }
            patch.totalIncome?.let { row[GameResultsTable.totalIncome] = it.toInt() }
            patch.note?.let { row[GameResultsTable.note] = it }
            patch.storeId?.let { row[GameResultsTable.storeId] = it }
            patch.simpleBatchId?.let { row[GameResultsTable.simpleBatchId] = it }
            patch.isFinalIncomeRecord?.let { row[GameResultsTable.isFinalIncomeRecord] = it }
            patch.updatedAt?.let { row[GameResultsTable.updatedAt] = it }
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
        resultsWithStore()
            .select { GameResultsTable.id eq resultId }
            .map(::toGameResult)
            .singleOrNull()
    }

    override suspend fun getUserResults(userId: Long, range: StatsRange): List<GameResult> = dbQuery {
        val rangeCondition =
            (GameResultsTable.playedAt greaterEq range.start) and (GameResultsTable.playedAt less range.end)
        resultsWithStore()
            .select {
                (GameResultsTable.userId eq userId) and
                    (rangeCondition or (GameResultsTable.isFinalIncomeRecord eq true))
            }
            .orderBy(GameResultsTable.playedAt to SortOrder.ASC, GameResultsTable.id to SortOrder.ASC)
            .map(::toGameResult)
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

    override suspend fun insertGameResults(results: List<GameResult>): List<GameResult> = dbQuery {
        if (results.isEmpty()) return@dbQuery emptyList()
        val ids = results.map { result ->
            GameResultsTable.insert { row ->
                row[GameResultsTable.userId] = result.userId
                row[GameResultsTable.gameType] = result.gameType.name
                row[GameResultsTable.playedAt] = result.playedAt
                row[GameResultsTable.place] = result.place
                row[GameResultsTable.baseIncome] = result.baseIncome.toInt()
                row[GameResultsTable.tipCount] = result.tipCount
                row[GameResultsTable.tipIncome] = result.tipIncome.toInt()
                row[GameResultsTable.otherIncome] = result.otherIncome.toInt()
                row[GameResultsTable.storeId] = result.storeId
                row[GameResultsTable.simpleBatchId] = result.simpleBatchId
                row[GameResultsTable.totalIncome] = result.totalIncome.toInt()
                row[GameResultsTable.note] = result.note
                row[GameResultsTable.isFinalIncomeRecord] = result.isFinalIncomeRecord
                row[GameResultsTable.createdAt] = result.createdAt
                row[GameResultsTable.updatedAt] = result.updatedAt
            } get GameResultsTable.id
        }
        ids.map(::fetchById)
    }

    override suspend fun findLatestBySimpleBatch(userId: Long, simpleBatchId: UUID): GameResult? = dbQuery {
        resultsWithStore()
            .select {
                (GameResultsTable.userId eq userId) and
                    (GameResultsTable.simpleBatchId eq simpleBatchId)
            }
            .orderBy(GameResultsTable.playedAt to SortOrder.DESC, GameResultsTable.id to SortOrder.DESC)
            .limit(1)
            .map(::toGameResult)
            .singleOrNull()
    }

    override suspend fun deleteBySimpleBatch(userId: Long, simpleBatchId: UUID): Int = dbQuery {
        GameResultsTable.deleteWhere {
            (GameResultsTable.userId eq userId) and
                (GameResultsTable.simpleBatchId eq simpleBatchId)
        }
    }

    private fun fetchById(id: Long): GameResult =
        resultsWithStore()
            .select { GameResultsTable.id eq id }
            .single()
            .let(::toGameResult)

    private fun resultsWithStore() =
        GameResultsTable.leftJoin(
            otherTable = StoreMasterTable,
            onColumn = { GameResultsTable.storeId },
            otherColumn = { StoreMasterTable.id }
        )

    /**
     * ResultRow -> GameResult �ϊ��B
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
            otherIncome = row[GameResultsTable.otherIncome].toLong(),
            totalIncome = row[GameResultsTable.totalIncome].toLong(),
            note = row[GameResultsTable.note],
            storeId = row[GameResultsTable.storeId],
            storeName = row[GameResultsTable.storeId]?.let { row[StoreMasterTable.storeName] },
            isFinalIncomeRecord = row[GameResultsTable.isFinalIncomeRecord],
            simpleBatchId = row[GameResultsTable.simpleBatchId],
            createdAt = row[GameResultsTable.createdAt],
            updatedAt = row[GameResultsTable.updatedAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
