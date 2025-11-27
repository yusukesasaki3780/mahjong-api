package com.example.database

import com.example.infrastructure.db.tables.GameResultsTable
import com.example.infrastructure.db.tables.GameSettingsTable
import com.example.infrastructure.db.tables.ShiftBreaksTable
import com.example.infrastructure.db.tables.ShiftsTable
import com.example.infrastructure.db.tables.UserCredentialsTable
import com.example.infrastructure.db.tables.UsersTable
import com.example.infrastructure.db.tables.RefreshTokensTable
import com.example.infrastructure.db.tables.AuditLogsTable
import com.example.infrastructure.db.tables.StoreMasterTable
import com.example.infrastructure.db.tables.PrefecturesTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(environment: ApplicationEnvironment) {
        val config = environment.config.config("ktor.database")
        val url = config.property("url").getString()
        val driver = config.property("driver").getString()
        val user = config.property("user").getString()
        val password = config.property("password").getString()

        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )

        transaction {
            SchemaUtils.create(
                UsersTable,
                UserCredentialsTable,
                GameSettingsTable,
                GameResultsTable,
                ShiftsTable,
                ShiftBreaksTable,
                RefreshTokensTable,
                AuditLogsTable,
                StoreMasterTable,
                PrefecturesTable
            )
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
