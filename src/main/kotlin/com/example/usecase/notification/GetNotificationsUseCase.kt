package com.example.usecase.notification

import com.example.domain.model.Notification
import com.example.domain.repository.NotificationRepository

class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(userId: Long, unreadOnly: Boolean): List<Notification> =
        notificationRepository.findByUser(userId, unreadOnly)
}
