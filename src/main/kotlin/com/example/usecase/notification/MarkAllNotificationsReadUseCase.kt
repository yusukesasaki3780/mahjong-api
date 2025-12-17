package com.example.usecase.notification

import com.example.domain.repository.NotificationRepository

class MarkAllNotificationsReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(userId: Long): Int =
        notificationRepository.markAllAsRead(userId)
}
