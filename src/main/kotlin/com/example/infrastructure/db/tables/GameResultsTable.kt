package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Exposed table that records each mahjong game result for a user.
 */
object GameResultsTable : Table("game_results") {
    val id = long("id").autoIncrement()
    val userId = reference(
        name = "user_id",
        refColumn = UsersTable.userId,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT
    )
    val gameType = varchar("game_type", length = 5)
    val playedAt = timestamp("played_at")
    val place = integer("place")
    val baseIncome = integer("base_income")
    val tipCount = integer("tip_count")
    val tipIncome = integer("tip_income")
    val totalIncome = integer("total_income")
    val note = text("note").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
