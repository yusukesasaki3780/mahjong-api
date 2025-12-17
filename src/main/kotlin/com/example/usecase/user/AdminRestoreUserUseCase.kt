package com.example.usecase.user

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.repository.UserRepository
import com.example.infrastructure.logging.AuditLogger

/**
 * 管理者が論理削除済みユーザーを復元するユースケース。
 * - 自分自身や他店舗のメンバー、管理者は対象外。
 */
class AdminRestoreUserUseCase(
    private val userRepository: UserRepository,
    private val auditLogger: AuditLogger
) {

    suspend operator fun invoke(
        adminId: Long,
        adminStoreId: Long,
        targetUserId: Long,
        auditContext: AuditContext
    ): Boolean {
        if (adminId == targetUserId) {
            throw validationError("userId", "SELF_RESTORE_FORBIDDEN", "自身のアカウントは復元できません。")
        }
        val target = userRepository.findById(targetUserId) ?: return false
        if (target.isAdmin) {
            throw validationError("userId", "ADMIN_RESTORE_FORBIDDEN", "管理者アカウントは復元できません。")
        }
        if (target.storeId != adminStoreId) {
            throw validationError("userId", "DIFFERENT_STORE", "他店舗のメンバーは復元できません。")
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
