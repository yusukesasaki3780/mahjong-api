package com.example.database

import com.example.infrastructure.db.tables.*
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import com.typesafe.config.ConfigFactory

object DatabaseFactory {

    fun init(environment: ApplicationEnvironment) {

        // application.conf を安全に読む（なくても動く）
        val conf = try {
            ConfigFactory.load().getConfig("ktor.database")
        } catch (e: Exception) {
            null
        }

        // application.conf のローカルデフォルト
        val localHost = conf?.getString("host") ?: "localhost"
        val localPort = conf?.getString("port") ?: "5432"
        val localName = conf?.getString("name") ?: "budget_db"
        val localUser = conf?.getString("user") ?: "budget_user"
        val localPass = conf?.getString("password") ?: "password"

        // ECS の環境変数があれば優先
        val host = System.getenv("DB_HOST") ?: localHost
        val port = System.getenv("DB_PORT") ?: localPort
        val name = System.getenv("DB_NAME") ?: localName
        val user = System.getenv("DB_USER") ?: localUser
        val pass = System.getenv("DB_PASSWORD") ?: localPass

        val url = "jdbc:postgresql://$host:$port/$name"

        println("========== DATABASE CONFIG ==========")
        println("url    = $url")
        println("user   = $user")
        println("=====================================")

        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = user,
            password = pass
        )

        transaction {
            SchemaUtils.create(
                UsersTable,
                UserCredentialsTable,
                GameSettingsTable,
                GameResultsTable,
                SpecialHourlyWagesTable,
                ShiftsTable,
                ShiftBreaksTable,
                ShiftSpecialAllowancesTable,
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
