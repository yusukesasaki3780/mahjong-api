package com.example.usecase.notification

import com.example.domain.repository.NotificationRepository

/**
 * ユーザーの未読通知数を取得するユースケース。
 */
class GetUnreadNotificationCountUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * 指定ユーザーの未読通知件数を返す。
     */
    suspend operator fun invoke(userId: Long): Int =
        notificationRepository.countUnread(userId)
}
