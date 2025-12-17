package com.example.usecase.user

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.repository.UserRepository
import com.example.infrastructure.logging.AuditLogger

/**
 * 管理者が削除済みユーザーを復元するユースケース。
 */
class AdminRestoreUserUseCase(
    private val userRepository: UserRepository,
    private val auditLogger: AuditLogger
) {

    /**
     * 自店舗所属の一般ユーザーのみ復元を許可し、成功時は監査ログを残す。
     */
    suspend operator fun invoke(
        adminId: Long,
        adminStoreId: Long,
        targetUserId: Long,
        auditContext: AuditContext
    ): Boolean {
        if (adminId == targetUserId) {
            throw validationError("userId", "SELF_RESTORE_FORBIDDEN", "自分自身は復元できません。")
        }
        val target = userRepository.findById(targetUserId) ?: return false
        if (target.isAdmin) {
            throw validationError("userId", "ADMIN_RESTORE_FORBIDDEN", "管理者を復元対象にすることはできません。")
        }
        if (target.storeId != adminStoreId) {
            throw validationError("userId", "DIFFERENT_STORE", "別店舗のユーザーは操作できません。")
        }
        if (!target.isDeleted) {
            return false
        }

        val restored = userRepository.restoreUser(targetUserId)
        if (restored) {
            auditLogger.log(
                entityType = "USER",
                entityId = targetUserId,
                action = "RESTORE",
                context = auditContext,
                before = target,
                after = target.copy(isDeleted = false)
            )
        }
        return restored
    }

    private fun validationError(field: String, code: String, message: String): DomainValidationException =
        DomainValidationException(
            violations = listOf(FieldError(field = field, code = code, message = message)),
            message = message
        )
}
