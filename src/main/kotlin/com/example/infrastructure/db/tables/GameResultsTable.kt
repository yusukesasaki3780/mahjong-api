package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.UUID

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
    val playedAt = timestamp("played_at").nullable()
    val place = integer("place")
    val baseIncome = integer("base_income")
    val tipCount = integer("tip_count")
    val tipIncome = integer("tip_income")
    val otherIncome = integer("other_income").default(0)
    val totalIncome = integer("total_income")
    val note = text("note").nullable()
    val isFinalIncomeRecord = bool("is_final_income_record").default(false)
    val storeId = reference(
        name = "store_id",
        refColumn = StoreMasterTable.id,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.RESTRICT
    ).nullable()
    val simpleBatchId = uuid("simple_batch_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
