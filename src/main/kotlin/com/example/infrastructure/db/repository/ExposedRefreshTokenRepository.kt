package com.example.infrastructure.db.repository

import com.example.domain.model.RefreshToken
import com.example.domain.repository.RefreshTokenRepository
import com.example.infrastructure.db.tables.RefreshTokensTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder

class ExposedRefreshTokenRepository : RefreshTokenRepository {

    override suspend fun create(userId: Long, tokenHash: String, expiresAt: Instant): RefreshToken = dbQuery {
        val now = Clock.System.now()
        val generatedId = RefreshTokensTable.insert { row ->
            row[RefreshTokensTable.userId] = userId
            row[RefreshTokensTable.tokenHash] = tokenHash
            row[RefreshTokensTable.expiresAt] = expiresAt
            row[RefreshTokensTable.createdAt] = now
        } get RefreshTokensTable.id

        RefreshToken(
            id = generatedId,
            userId = userId,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
            createdAt = now
        )
    }

    override suspend fun findValidTokens(userId: Long, now: Instant): List<RefreshToken> = dbQuery {
        RefreshTokensTable
            .select {
                (RefreshTokensTable.userId eq userId) and (RefreshTokensTable.expiresAt greaterEq now)
            }
            .map { it.toRefreshToken() }
    }

    override suspend fun delete(tokenId: Long) {
        dbQuery {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.id eq tokenId }
        }
    }

    override suspend fun deleteAllForUser(userId: Long) {
        dbQuery {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        }
    }

    override suspend fun deleteExpired(now: Instant) {
        dbQuery {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.expiresAt less now }
        }
    }

    private fun ResultRow.toRefreshToken() = RefreshToken(
        id = this[RefreshTokensTable.id],
        userId = this[RefreshTokensTable.userId],
        tokenHash = this[RefreshTokensTable.tokenHash],
        expiresAt = this[RefreshTokensTable.expiresAt],
        createdAt = this[RefreshTokensTable.createdAt]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
