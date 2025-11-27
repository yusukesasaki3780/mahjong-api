package com.example.infrastructure.db.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.domain.repository.UserCredentialRepository
import com.example.infrastructure.db.tables.UserCredentialsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * ユーザ認証情報を管理する Exposed 実装。
 */
class ExposedUserCredentialRepository : UserCredentialRepository {

    private val verifier = BCrypt.verifyer()

    override suspend fun createCredentials(userId: Long, email: String, passwordHash: String) {
        dbQuery {
            UserCredentialsTable.insert { row ->
                row[UserCredentialsTable.userId] = userId
                row[UserCredentialsTable.email] = email
                row[UserCredentialsTable.passwordHash] = passwordHash
                row[UserCredentialsTable.lastLoginAt] = null
            }
        }
    }

    override suspend fun verifyPassword(userId: Long, password: String): Boolean = dbQuery {
        val storedHash = UserCredentialsTable
            .slice(UserCredentialsTable.passwordHash)
            .select { UserCredentialsTable.userId eq userId }
            .singleOrNull()
            ?.get(UserCredentialsTable.passwordHash)
            ?: return@dbQuery false

        val verified = verifier.verify(password.toCharArray(), storedHash.toCharArray()).verified
        if (verified) {
            UserCredentialsTable.update({ UserCredentialsTable.userId eq userId }) { row ->
                row[lastLoginAt] = Clock.System.now()
            }
        }
        verified
    }

    override suspend fun updatePassword(userId: Long, newPassword: String) {
        dbQuery {
            UserCredentialsTable.update({ UserCredentialsTable.userId eq userId }) { row ->
                row[UserCredentialsTable.passwordHash] = newPassword
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
