package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * シフトと特別時給を1対1で紐づけるための中間テーブルです。
 */
object ShiftSpecialAllowancesTable : Table("shift_special_allowances") {
    val id = long("id").autoIncrement()
    val shiftId = reference(
        name = "shift_id",
        refColumn = ShiftsTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    val specialHourlyWageId = reference(
        name = "special_hourly_wage_id",
        refColumn = SpecialHourlyWagesTable.id,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.RESTRICT
    ).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex("shift_special_allowances_shift_id_uindex", shiftId)
    }

    override val primaryKey = PrimaryKey(id)
}
