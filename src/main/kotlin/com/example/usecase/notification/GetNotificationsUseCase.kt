package com.example.usecase.notification

import com.example.domain.model.Notification
import com.example.domain.repository.NotificationRepository

/**
 * ユーザーの通知一覧を取得するユースケース。
 */
class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * 指定ユーザーの通知を、未読のみかどうかの条件に応じて取得する。
     */
    suspend operator fun invoke(userId: Long, unreadOnly: Boolean): List<Notification> =
        notificationRepository.findByUser(userId, unreadOnly)
}
