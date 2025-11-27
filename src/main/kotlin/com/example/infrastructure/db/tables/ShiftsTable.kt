package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Exposed table that tracks each staff work shift.
 */
object ShiftsTable : Table("shifts") {
    val id = long("id").autoIncrement()
    val userId = reference(
        name = "user_id",
        refColumn = UsersTable.userId,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT
    )
    val workDate = date("work_date")
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time")
    val memo = text("memo").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
