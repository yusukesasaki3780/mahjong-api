package com.example.presentation.dto

import com.example.domain.model.Notification
import com.example.domain.model.NotificationType
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: Long,
    val notificationType: NotificationType,
    val message: String,
    val relatedShiftId: Long?,
    val isRead: Boolean,
    val createdAt: String,
    val actorUserId: Long?,
    val readAt: String?
) {
    companion object {
        fun from(model: Notification) = NotificationResponse(
            id = model.id ?: error("notification id missing"),
            notificationType = model.type,
            message = model.message,
            relatedShiftId = model.relatedShiftId,
            isRead = model.isRead,
            createdAt = model.createdAt.toString(),
            actorUserId = model.actorUserId,
            readAt = model.readAt?.toString()
        )
    }
}

@Serializable
data class NotificationBulkUpdateResponse(
    val updatedCount: Int
)

@Serializable
data class NotificationUnreadCountResponse(
    val count: Int
)
