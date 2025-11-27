package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Exposed table that stores break intervals linked to a shift.
 */
object ShiftBreaksTable : Table("shift_breaks") {
    val id = long("id").autoIncrement()
    val shiftId = reference(
        name = "shift_id",
        refColumn = ShiftsTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT
    )
    val breakStart = timestamp("break_start")
    val breakEnd = timestamp("break_end")

    override val primaryKey = PrimaryKey(id)
}
