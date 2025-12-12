package com.example.infrastructure.db.repository

import com.example.domain.model.GameType
import com.example.domain.model.RankingEntry
import com.example.domain.model.StatsPeriod
import com.example.domain.model.User
import com.example.domain.repository.UserPatch
import com.example.domain.repository.UserRepository
import com.example.infrastructure.db.tables.GameResultsTable
import com.example.infrastructure.db.tables.GameSettingsTable
import com.example.infrastructure.db.tables.ShiftBreaksTable
import com.example.infrastructure.db.tables.ShiftsTable
import com.example.infrastructure.db.tables.UsersTable
import com.example.infrastructure.db.tables.UserCredentialsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * UserRepository の Exposed 実装。ユーザ CRUD とランキング取得を司る。
 */
class ExposedUserRepository : UserRepository {

    override suspend fun findById(userId: Long): User? = dbQuery {
        UsersTable
            .select { UsersTable.userId eq userId }
            .singleOrNull()
            ?.let(::toUser)
    }

    override suspend fun createUser(user: User): User = dbQuery {
        val newId = UsersTable.insert { row ->
            row[name] = user.name
            row[nickname] = user.nickname
            row[storeName] = user.storeName
            row[prefectureCode] = user.prefectureCode
            row[email] = user.email
            row[zooId] = user.zooId
            row[UsersTable.isAdmin] = user.isAdmin
            row[createdAt] = user.createdAt
            row[updatedAt] = user.updatedAt
        } get UsersTable.userId

        UsersTable
            .select { UsersTable.userId eq newId }
            .single()
            .let(::toUser)
    }

    override suspend fun updateUser(user: User): User = dbQuery {
        val targetId = user.id ?: error("User id is required for update.")
        UsersTable.update({ UsersTable.userId eq targetId }) { row ->
            row[name] = user.name
            row[nickname] = user.nickname
            row[storeName] = user.storeName
            row[prefectureCode] = user.prefectureCode
            row[email] = user.email
            row[zooId] = user.zooId
            row[UsersTable.isAdmin] = user.isAdmin
            row[createdAt] = user.createdAt
            row[updatedAt] = user.updatedAt
        }

        UsersTable
            .select { UsersTable.userId eq targetId }
            .single()
            .let(::toUser)
    }

    override suspend fun patchUser(userId: Long, patch: UserPatch): User = dbQuery {
        val updatedRows = UsersTable.update({ UsersTable.userId eq userId }) { row ->
            patch.name?.let { row[name] = it }
            patch.nickname?.let { row[nickname] = it }
            patch.storeName?.let { row[storeName] = it }
            patch.prefectureCode?.let { row[prefectureCode] = it }
            patch.email?.let { row[email] = it }
            patch.updatedAt?.let { row[updatedAt] = it }
        }
        if (updatedRows == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        UsersTable
            .select { UsersTable.userId eq userId }
            .single()
            .let(::toUser)
    }

    override suspend fun deleteUser(userId: Long): Boolean = dbQuery {
        GameResultsTable.deleteWhere { GameResultsTable.userId eq userId }
        val shiftIdsQuery = ShiftsTable.slice(ShiftsTable.id).select { ShiftsTable.userId eq userId }
        ShiftBreaksTable.deleteWhere { ShiftBreaksTable.shiftId inSubQuery shiftIdsQuery }
        ShiftsTable.deleteWhere { ShiftsTable.userId eq userId }
        GameSettingsTable.deleteWhere { GameSettingsTable.userId eq userId }
        UserCredentialsTable.deleteWhere { UserCredentialsTable.userId eq userId }
        UsersTable.deleteWhere { UsersTable.userId eq userId } > 0
    }

    override suspend fun listNonAdminUsers(): List<User> = dbQuery {
        UsersTable
            .select { UsersTable.isAdmin eq false }
            .map(::toUser)
    }

    override suspend fun findByEmail(emailValue: String): User? = dbQuery {
        UsersTable
            .select { UsersTable.email eq emailValue }
            .singleOrNull()
            ?.let(::toUser)
    }

    override suspend fun findByZooId(zooIdValue: Int): User? = dbQuery {
        UsersTable
            .select { UsersTable.zooId eq zooIdValue }
            .singleOrNull()
            ?.let(::toUser)
    }

    override suspend fun findRanking(gameType: GameType, period: StatsPeriod): List<RankingEntry> = dbQuery {
        val baseSum = GameResultsTable.baseIncome.sum()
        val tipSum = GameResultsTable.tipIncome.sum()
        val otherSum = GameResultsTable.otherIncome.sum()
        val gameCount = GameResultsTable.id.count()
        val avgPlace = GameResultsTable.place.avg()

        val lowerBound =
            (GameResultsTable.playedAt.isNotNull() and (GameResultsTable.playedAt greaterEq period.start)) or
                (GameResultsTable.playedAt.isNull() and (GameResultsTable.createdAt greaterEq period.start))
        val upperBound =
            (GameResultsTable.playedAt.isNotNull() and (GameResultsTable.playedAt less period.end)) or
                (GameResultsTable.playedAt.isNull() and (GameResultsTable.createdAt less period.end))

        (UsersTable innerJoin GameResultsTable)
            .slice(listOf(UsersTable.userId, UsersTable.name, UsersTable.nickname, UsersTable.zooId, baseSum, tipSum, otherSum, gameCount, avgPlace))
            .select {
                (GameResultsTable.gameType eq gameType.name) and
                    lowerBound and
                    upperBound
            }
            .groupBy(UsersTable.userId, UsersTable.name, UsersTable.nickname, UsersTable.zooId)
            .map { row ->
                val nickname = row[UsersTable.nickname]
                val displayName = nickname.takeIf { it.isNotBlank() } ?: row[UsersTable.name]
                RankingEntry(
                    userId = row[UsersTable.userId],
                    zooId = row[UsersTable.zooId],
                    name = displayName,
                    totalIncome = (row[baseSum]?.toLong() ?: 0L) +
                        (row[tipSum]?.toLong() ?: 0L) +
                        (row[otherSum]?.toLong() ?: 0L),
                    gameCount = row[gameCount]?.toInt() ?: 0,
                    averagePlace = row[avgPlace]?.toDouble()
                )
            }
            .sortedByDescending { it.totalIncome }
    }

    /**
     * ResultRow -> User 変換ヘルパー。
     */
    private fun toUser(row: ResultRow): User =
        User(
            id = row[UsersTable.userId],
            name = row[UsersTable.name],
            nickname = row[UsersTable.nickname],
            storeName = row[UsersTable.storeName],
            prefectureCode = row[UsersTable.prefectureCode].trim(),
            email = row[UsersTable.email],
            zooId = row[UsersTable.zooId],
            isAdmin = row[UsersTable.isAdmin],
            createdAt = row[UsersTable.createdAt],
            updatedAt = row[UsersTable.updatedAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
