package com.example.usecase.user

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.NotificationType
import com.example.domain.model.User
import com.example.domain.repository.AdminPrivilegeGateway
import com.example.domain.repository.UserRepository
import kotlinx.datetime.Clock

class AdminUpdateUserAdminFlagUseCase(
    private val userRepository: UserRepository,
    private val adminPrivilegeGateway: AdminPrivilegeGateway,
    private val clock: Clock = Clock.System
) {

    suspend operator fun invoke(
        adminId: Long,
        adminName: String?,
        adminStoreId: Long,
        targetUserId: Long,
        isAdmin: Boolean,
        auditContext: AuditContext
    ): User? {
        if (adminId == targetUserId) {
            throw validationError(
                code = "SELF_UPDATE_FORBIDDEN",
                message = "You cannot change your own admin privilege."
            )
        }

        val target = userRepository.findById(targetUserId) ?: return null
        if (target.storeId != adminStoreId) {
            throw validationError(
                code = "DIFFERENT_STORE",
                message = "You can only manage users that belong to your store."
            )
        }
        if (target.isDeleted) {
            throw validationError(
                code = "USER_DELETED",
                message = "Deleted users cannot be updated."
            )
        }

        if (target.isAdmin == isAdmin) {
            return target
        }

        val occurredAt = clock.now()
        val action = if (isAdmin) "USER_ADMIN_GRANTED" else "USER_ADMIN_REVOKED"
        val beforeJson = """{"is_admin":${target.isAdmin}}"""
        val afterJson = """{"is_admin":$isAdmin}"""
        val notificationType = if (isAdmin) {
            NotificationType.ADMIN_ROLE_GRANTED
        } else {
            NotificationType.ADMIN_ROLE_REVOKED
        }
        val message = buildNotificationMessage(adminName, isAdmin)

        return adminPrivilegeGateway.updateAdminFlag(
            adminId = adminId,
            targetUserId = targetUserId,
            isAdmin = isAdmin,
            action = action,
            beforeJson = beforeJson,
            afterJson = afterJson,
            notificationType = notificationType,
            message = message,
            auditContext = auditContext,
            occurredAt = occurredAt
        )
    }

    private fun buildNotificationMessage(adminName: String?, granted: Boolean): String {
        val operatorLabel = adminName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { "管理者 $it" }
            ?: "管理者"
        return if (granted) {
            "${operatorLabel}によって、あなたに管理者権限が付与されました。"
        } else {
            "${operatorLabel}によって、あなたの管理者権限が解除されました。"
        }
    }

    private fun validationError(
        field: String = "userId",
        code: String,
        message: String
    ): DomainValidationException =
        DomainValidationException(
            violations = listOf(FieldError(field = field, code = code, message = message)),
            message = message
        )
}
