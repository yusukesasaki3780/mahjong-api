package com.example.infrastructure.db.repository

import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import com.example.domain.repository.GameSettingsPatch
import com.example.domain.repository.GameSettingsRepository
import com.example.infrastructure.db.tables.GameSettingsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.math.BigDecimal

/**
 * ゲーム設定テーブルを操作する Exposed 実装。
 */
class ExposedGameSettingsRepository : GameSettingsRepository {

    override suspend fun getSettings(userId: Long): GameSettings? = dbQuery {
        GameSettingsTable
            .select { GameSettingsTable.userId eq userId }
            .singleOrNull()
            ?.let(::toGameSettings)
    }

    override suspend fun updateSettings(userId: Long, settings: GameSettings): GameSettings = dbQuery {
        val updatedRows = GameSettingsTable.update({ GameSettingsTable.userId eq userId }) { row ->
            row.applySettings(settings)
        }

        if (updatedRows == 0) {
            GameSettingsTable.insert { row ->
                row[GameSettingsTable.userId] = userId
                row.applySettings(settings)
            }
        }

        GameSettingsTable
            .select { GameSettingsTable.userId eq userId }
            .single()
            .let(::toGameSettings)
    }

    override suspend fun patchSettings(userId: Long, patch: GameSettingsPatch): GameSettings = dbQuery {
        val updated = GameSettingsTable.update({ GameSettingsTable.userId eq userId }) { row ->
            patch.yonmaGameFee?.let { row[GameSettingsTable.yonmaGameFee] = it }
            patch.sanmaGameFee?.let { row[GameSettingsTable.sanmaGameFee] = it }
            patch.sanmaGameFeeBack?.let { row[GameSettingsTable.sanmaGameFeeBack] = it }
            patch.yonmaTipUnit?.let { row[GameSettingsTable.yonmaTipUnit] = it }
            patch.sanmaTipUnit?.let { row[GameSettingsTable.sanmaTipUnit] = it }
            patch.wageType?.let { row[GameSettingsTable.wageType] = it.name }
            patch.hourlyWage?.let { row[GameSettingsTable.hourlyWage] = it }
            patch.fixedSalary?.let { row[GameSettingsTable.fixedSalary] = it }
            patch.nightRateMultiplier?.let { row[GameSettingsTable.nightRateMultiplier] = it.toBigDecimal() }
            patch.baseMinWage?.let { row[GameSettingsTable.baseMinWage] = it }
            if (patch.incomeTaxRate != null) {
                row[GameSettingsTable.incomeTaxRate] = patch.incomeTaxRate.toBigDecimal()
            }
            if (patch.transportPerShift != null) {
                row[GameSettingsTable.transportPerShift] = patch.transportPerShift
            }
            patch.updatedAt?.let { row[GameSettingsTable.updatedAt] = it }
        }
        if (updated == 0) {
            throw IllegalArgumentException("Game settings not found for user $userId")
        }

        GameSettingsTable
            .select { GameSettingsTable.userId eq userId }
            .single()
            .let(::toGameSettings)
    }

    override suspend fun findById(id: Long): GameSettings? = getSettings(id)

    private fun UpdateBuilder<*>.applySettings(settings: GameSettings) {
        this[GameSettingsTable.yonmaGameFee] = settings.yonmaGameFee
        this[GameSettingsTable.sanmaGameFee] = settings.sanmaGameFee
        this[GameSettingsTable.sanmaGameFeeBack] = settings.sanmaGameFeeBack
        this[GameSettingsTable.yonmaTipUnit] = settings.yonmaTipUnit
        this[GameSettingsTable.sanmaTipUnit] = settings.sanmaTipUnit
        this[GameSettingsTable.wageType] = settings.wageType.name
        this[GameSettingsTable.hourlyWage] = settings.hourlyWage
        this[GameSettingsTable.fixedSalary] = settings.fixedSalary
        this[GameSettingsTable.nightRateMultiplier] = settings.nightRateMultiplier.toBigDecimal()
        this[GameSettingsTable.baseMinWage] = settings.baseMinWage
        this[GameSettingsTable.incomeTaxRate] = settings.incomeTaxRate?.toBigDecimal()
        this[GameSettingsTable.transportPerShift] = settings.transportPerShift
        this[GameSettingsTable.createdAt] = settings.createdAt
        this[GameSettingsTable.updatedAt] = settings.updatedAt
    }

    private fun Double.toBigDecimal(): BigDecimal = BigDecimal.valueOf(this)

    private fun toGameSettings(row: ResultRow): GameSettings =
        GameSettings(
            userId = row[GameSettingsTable.userId],
            yonmaGameFee = row[GameSettingsTable.yonmaGameFee],
            sanmaGameFee = row[GameSettingsTable.sanmaGameFee],
            sanmaGameFeeBack = row[GameSettingsTable.sanmaGameFeeBack],
            yonmaTipUnit = row[GameSettingsTable.yonmaTipUnit],
            sanmaTipUnit = row[GameSettingsTable.sanmaTipUnit],
            wageType = WageType.valueOf(row[GameSettingsTable.wageType]),
            hourlyWage = row[GameSettingsTable.hourlyWage],
            fixedSalary = row[GameSettingsTable.fixedSalary],
            nightRateMultiplier = row[GameSettingsTable.nightRateMultiplier].toDouble(),
            baseMinWage = row[GameSettingsTable.baseMinWage],
            incomeTaxRate = row[GameSettingsTable.incomeTaxRate]?.toDouble(),
            transportPerShift = row[GameSettingsTable.transportPerShift],
            createdAt = row[GameSettingsTable.createdAt],
            updatedAt = row[GameSettingsTable.updatedAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
