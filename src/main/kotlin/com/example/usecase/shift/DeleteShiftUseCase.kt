package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - シフトを削除し、削除結果を監査ログへ記録する役割を持つユースケースです。
 * - 存在チェックと削除処理をセットで行い、監査には before 情報を渡します。
 */

import com.example.domain.model.AuditContext
import com.example.domain.repository.ShiftRepository
import com.example.infrastructure.logging.AuditLogger

/**
 * シフト削除と監査・通知処理を一括で実行するユースケース。
 */
class DeleteShiftUseCase(
    private val repository: ShiftRepository,
    private val auditLogger: AuditLogger,
    private val shiftNotificationService: ShiftNotificationService,
    private val contextProvider: ShiftContextProvider,
    private val permissionService: ShiftPermissionService
) {
    /**
     * 削除権限を確認してからシフトを削除し、成功時のみ監査ログと通知を発行する。
     */
    suspend operator fun invoke(actorId: Long, shiftId: Long, auditContext: AuditContext): Boolean {
        val context = contextProvider.forDelete(actorId, shiftId)
        permissionService.ensureCanDelete(context)
        val before = context.shift
        val deleted = repository.deleteShift(shiftId)
        if (deleted) {
            shiftNotificationService.notifyDeleted(
                actorId = auditContext.performedBy,
                targetUserId = before.userId,
                shift = before
            )
            auditLogger.log(
                entityType = "SHIFT",
                entityId = shiftId,
                action = "DELETE",
                context = auditContext,
                before = before,
                after = null
            )
        }
        return deleted
    }
}
