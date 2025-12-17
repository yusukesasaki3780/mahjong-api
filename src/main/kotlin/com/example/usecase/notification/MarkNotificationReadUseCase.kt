package com.example.usecase.notification

import com.example.domain.repository.NotificationRepository

/**
 * 指定した通知を既読にするユースケース。
 */
class MarkNotificationReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * 対象ユーザーが所有する通知 ID を既読にし、更新成否を返す。
     */
    suspend operator fun invoke(userId: Long, notificationId: Long): Boolean =
        notificationRepository.markAsRead(notificationId, userId)
}
