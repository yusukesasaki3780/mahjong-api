package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Table that stores per-user special hourly wage presets.
 */
object SpecialHourlyWagesTable : Table("special_hourly_wages") {
    val id = long("id").autoIncrement()
    val userId = reference(
        name = "user_id",
        refColumn = GameSettingsTable.userId,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT
    )
    val label = varchar("label", length = 64)
    val hourlyWage = integer("hourly_wage")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
