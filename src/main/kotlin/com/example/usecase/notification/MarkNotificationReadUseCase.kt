package com.example.usecase.notification

import com.example.domain.repository.NotificationRepository

class MarkNotificationReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(userId: Long, notificationId: Long): Boolean =
        notificationRepository.markAsRead(notificationId, userId)
}
