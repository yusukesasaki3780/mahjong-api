package com.example.usecase.notification

import com.example.domain.repository.NotificationRepository

/**
 * ユーザーの通知を一括で既読にするユースケース。
 */
class MarkAllNotificationsReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * 指定ユーザーの未読通知をすべて既読化し、更新件数を返す。
     */
    suspend operator fun invoke(userId: Long): Int =
        notificationRepository.markAllAsRead(userId)
}
