package com.example.infrastructure.db.repository

import com.example.infrastructure.db.tables.GameResultsTable
import com.example.infrastructure.db.tables.GameSettingsTable
import com.example.infrastructure.db.tables.ShiftBreaksTable
import com.example.infrastructure.db.tables.ShiftsTable
import com.example.infrastructure.db.tables.UserCredentialsTable
import com.example.infrastructure.db.tables.UsersTable
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.h2.jdbcx.JdbcConnectionPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * H2 上で Exposed Repository を検証するための共通初期化ユーティリティ。
 */
abstract class RepositoryTestBase {

    protected fun runDbTest(testBody: suspend () -> Unit) = runTest {
        val dbName = "test-${UUID.randomUUID()}"
        val dataSource = JdbcConnectionPool.create(
            "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        )
        val database = Database.connect(dataSource)
        org.jetbrains.exposed.sql.transactions.TransactionManager.defaultDatabase = database
        transaction {
            SchemaUtils.create(
                UsersTable,
                UserCredentialsTable,
                GameSettingsTable,
                GameResultsTable,
                ShiftsTable,
                ShiftBreaksTable
            )
        }

        testBody()
    }
}
