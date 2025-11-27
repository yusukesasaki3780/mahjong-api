package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RefreshTokensTable : Table("refresh_tokens") {
    val id = long("id").autoIncrement()
    val userId = reference("user_id", UsersTable.userId, onDelete = ReferenceOption.CASCADE)
    val tokenHash = text("token_hash")
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
