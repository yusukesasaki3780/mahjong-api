package com.example.infrastructure.db.tables

import java.math.BigDecimal
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Exposed table that stores per-user defaults for game fees, wages, and other settings.
 */
object GameSettingsTable : Table("game_settings") {
    val userId = reference(
        name = "user_id",
        refColumn = UsersTable.userId,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT
    )

    val yonmaGameFee = integer("yonma_game_fee").default(400)
    val sanmaGameFee = integer("sanma_game_fee").default(250)
    val sanmaGameFeeBack = integer("game_fee_back").default(0)

    val yonmaTipUnit = integer("yonma_tip_unit").default(100)
    val sanmaTipUnit = integer("sanma_tip_unit").default(50)

    val wageType = varchar("wage_type", length = 16).default("HOURLY")
    val hourlyWage = integer("hourly_wage").default(1226)
    val fixedSalary = integer("fixed_salary").default(350_000)
    val nightRateMultiplier = decimal("night_rate_multiplier", precision = 5, scale = 2)
        .default(BigDecimal("1.25"))
    val baseMinWage = integer("base_min_wage").default(1226)

    val incomeTaxRate = decimal("income_tax_rate", precision = 5, scale = 4).nullable()
    val transportPerShift = integer("transport_per_shift").nullable()

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
