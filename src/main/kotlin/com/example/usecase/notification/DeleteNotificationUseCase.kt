package com.example.usecase.notification

import com.example.domain.repository.NotificationRepository

class DeleteNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {

    sealed class Result {
        object Deleted : Result()
        object NotFound : Result()
        object Forbidden : Result()
    }

    suspend operator fun invoke(userId: Long, notificationId: Long): Result {
        val notification = notificationRepository.findById(notificationId) ?: return Result.NotFound
        if (notification.targetUserId != userId) {
            return Result.Forbidden
        }
        val deleted = notificationRepository.delete(notificationId, userId)
        return if (deleted) Result.Deleted else Result.NotFound
    }
}
