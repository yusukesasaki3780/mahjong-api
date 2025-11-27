package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AdvancePaymentsTable : Table("advance_payments") {
    val userId = long("user_id")
    val yearMonth = varchar("year_month", 7)
    val amount = double("amount")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(userId, yearMonth)
}
