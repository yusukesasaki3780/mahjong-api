package com.example.infrastructure.db.tables

import com.example.domain.model.NotificationType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object NotificationsTable : Table("notifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val targetUserId: Column<Long> = long("target_user_id")
    val actorUserId: Column<Long?> = long("actor_user_id").nullable()
    val notificationType =
        enumerationByName("notification_type", length = 50, klass = NotificationType::class)
    val message = text("message")
    val isRead = bool("is_read").default(false)
    val readAt = timestamp("read_at").nullable()
    val relatedShiftId: Column<Long?> = long("related_shift_id").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
