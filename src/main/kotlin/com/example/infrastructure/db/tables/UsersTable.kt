package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Exposed table that stores core user profile information.
 */
object UsersTable : Table("users") {
    val userId = long("user_id").autoIncrement()
    val name = varchar("name", length = 100)
    val nickname = varchar("nickname", length = 100)
    val storeName = varchar("store_name", length = 150)
    val prefectureCode = varchar("prefecture_code", length = 2)
    val email = varchar("email", length = 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(userId)

    init {
        uniqueIndex("uq_users_email", email)
    }
}
