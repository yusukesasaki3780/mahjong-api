package com.example.usecase.shift

import com.example.domain.model.Notification
import com.example.domain.model.NotificationType
import com.example.domain.model.Shift
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ShiftNotificationService(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    suspend fun notifyCreated(actorId: Long, targetUserId: Long, shift: Shift) {
        publish(actorId, targetUserId, shift, NotificationType.SHIFT_CREATED) { actorName, date ->
            "管理者 $actorName によって、あなたの${date}のシフトが登録されました。"
        }
    }

    suspend fun notifyUpdated(actorId: Long, targetUserId: Long, shift: Shift) {
        publish(actorId, targetUserId, shift, NotificationType.SHIFT_UPDATED) { actorName, date ->
            "管理者 $actorName によって、あなたの${date}のシフトが変更されました。"
        }
    }

    suspend fun notifyDeleted(actorId: Long, targetUserId: Long, shift: Shift) {
        publish(actorId, targetUserId, shift, NotificationType.SHIFT_DELETED) { actorName, date ->
            "管理者 $actorName によって、あなたの${date}のシフトが削除されました。"
        }
    }

    private suspend fun publish(
        actorId: Long,
        targetUserId: Long,
        shift: Shift,
        type: NotificationType,
        messageBuilder: (String, String) -> String
    ) {
        if (actorId == targetUserId) return
        val actor = userRepository.findById(actorId) ?: return
        if (!actor.isAdmin) return
        val target = userRepository.findById(targetUserId) ?: return
        if (actor.storeId != target.storeId) return

        val today = Clock.System.now().toLocalDateTime(timeZone).date
        if (shift.workDate < today) return

        val actorName = actor.nickname.takeIf { it.isNotBlank() } ?: actor.name
        val message = messageBuilder(actorName, shift.workDate.toString())
        val now = Clock.System.now()
        notificationRepository.create(
            Notification(
                targetUserId = targetUserId,
                actorUserId = actorId,
                type = type,
                message = message,
                isRead = false,
                relatedShiftId = shift.id,
                createdAt = now
            )
        )
    }
}
