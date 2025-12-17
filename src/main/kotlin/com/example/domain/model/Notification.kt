package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    SHIFT_CREATED,
    SHIFT_UPDATED,
    SHIFT_DELETED
}

@Serializable
data class Notification(
    val id: Long? = null,
    val targetUserId: Long,
    val actorUserId: Long?,
    val type: NotificationType,
    val message: String,
    val isRead: Boolean = false,
    val relatedShiftId: Long? = null,
    val createdAt: Instant,
    val readAt: Instant? = null
)
