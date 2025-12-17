package com.example.domain.repository

import com.example.domain.model.Notification

interface NotificationRepository {
    suspend fun create(notification: Notification): Notification
    suspend fun findByUser(userId: Long, unreadOnly: Boolean = false): List<Notification>
    suspend fun findById(notificationId: Long): Notification?
    suspend fun markAsRead(notificationId: Long, userId: Long): Boolean
    suspend fun markAllAsRead(userId: Long): Int
    suspend fun delete(notificationId: Long, userId: Long): Boolean
    suspend fun countUnread(userId: Long): Int
}
