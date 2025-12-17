package com.example.domain.repository

import com.example.domain.model.AuditContext
import com.example.domain.model.NotificationType
import com.example.domain.model.User
import kotlinx.datetime.Instant

interface AdminPrivilegeGateway {
    suspend fun updateAdminFlag(
        adminId: Long,
        targetUserId: Long,
        isAdmin: Boolean,
        action: String,
        beforeJson: String?,
        afterJson: String?,
        notificationType: NotificationType,
        message: String,
        auditContext: AuditContext,
        occurredAt: Instant
    ): User
}
