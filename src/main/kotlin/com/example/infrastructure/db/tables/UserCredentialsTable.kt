package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Exposed table that stores hashed login credentials per user.
 */
object UserCredentialsTable : Table("user_credentials") {
    val userId = reference(
        name = "user_id",
        refColumn = UsersTable.userId,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT
    )
    val email = varchar("email", length = 255)
    val passwordHash = varchar("password_hash", length = 255)
    val lastLoginAt = timestamp("last_login_at").nullable()

    override val primaryKey = PrimaryKey(userId)

    init {
        uniqueIndex("uq_user_credentials_email", email)
    }
}
