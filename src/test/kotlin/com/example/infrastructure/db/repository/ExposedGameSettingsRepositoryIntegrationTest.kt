package com.example.infrastructure.db.repository

import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import com.example.domain.repository.GameSettingsPatch
import com.example.infrastructure.db.tables.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExposedGameSettingsRepositoryIntegrationTest : RepositoryTestBase() {

    private val repository = ExposedGameSettingsRepository()

    @Test
    fun `insert or update settings`() = runDbTest {
        val userId = seedUser()
        val now = Clock.System.now()
        val saved = repository.updateSettings(
            userId,
            GameSettings(
                userId = userId,
                yonmaGameFee = 400,
                sanmaGameFee = 250,
                sanmaGameFeeBack = 0,
                yonmaTipUnit = 100,
                sanmaTipUnit = 50,
                wageType = WageType.HOURLY,
                hourlyWage = 1200,
                fixedSalary = 300000,
                nightRateMultiplier = 1.25,
                baseMinWage = 1200,
                incomeTaxRate = 0.1,
                transportPerShift = 500,
                createdAt = now,
                updatedAt = now
            )
        )
        assertEquals(400, saved.yonmaGameFee)

        val patched = repository.patchSettings(
            userId,
            GameSettingsPatch(
                sanmaGameFee = 260,
                sanmaGameFeeBack = 20,
                transportPerShift = null,
                incomeTaxRate = null,
                updatedAt = now.plus(1, kotlinx.datetime.DateTimeUnit.MINUTE)
            )
        )
        assertEquals(260, patched.sanmaGameFee)
        assertEquals(20, patched.sanmaGameFeeBack)
        assertEquals(500, patched.transportPerShift)
    }

    private fun seedUser(): Long {
        val now = Clock.System.now()
        return transaction {
            UsersTable.insert {
                it[name] = "SettingsUser"
                it[nickname] = "su"
                it[storeName] = "Store"
                it[prefectureCode] = "01"
                it[email] = "settings-${now.toEpochMilliseconds()}@example.com"
                it[zooId] = (now.toEpochMilliseconds() % 900_000).toInt() + 1
                it[createdAt] = now
                it[updatedAt] = now
            } get UsersTable.userId
        }
    }
}
