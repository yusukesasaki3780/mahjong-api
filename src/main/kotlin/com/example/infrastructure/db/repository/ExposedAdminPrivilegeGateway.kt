package com.example.infrastructure.db.repository

import com.example.database.DatabaseFactory
import com.example.domain.model.AuditContext
import com.example.domain.model.NotificationType
import com.example.domain.model.User
import com.example.domain.repository.AdminPrivilegeGateway
import com.example.infrastructure.db.tables.AuditLogsTable
import com.example.infrastructure.db.tables.NotificationsTable
import com.example.infrastructure.db.tables.UsersTable
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ExposedAdminPrivilegeGateway : AdminPrivilegeGateway {

    override suspend fun updateAdminFlag(
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
    ): User = DatabaseFactory.dbQuery {
        val updated = UsersTable.update({
            (UsersTable.userId eq targetUserId) and (UsersTable.isDeleted eq false)
        }) { row ->
            row[UsersTable.isAdmin] = isAdmin
            row[UsersTable.updatedAt] = occurredAt
        }
        if (updated == 0) {
            throw IllegalStateException("Failed to update admin flag for user $targetUserId")
        }

        AuditLogsTable.insert { row ->
            row[entityType] = "USER"
            row[entityId] = targetUserId
            row[actionColumn] = action
            row[performedBy] = adminId
            row[performedAt] = occurredAt
            row[path] = auditContext.path
            row[ipAddress] = auditContext.ipAddress
            row[before] = beforeJson
            row[after] = afterJson
        }

        NotificationsTable.insert { row ->
            row[NotificationsTable.targetUserId] = targetUserId
            row[NotificationsTable.actorUserId] = adminId
            row[NotificationsTable.notificationType] = notificationType
            row[NotificationsTable.message] = message
            row[NotificationsTable.isRead] = false
            row[NotificationsTable.readAt] = null
            row[NotificationsTable.relatedShiftId] = null
            row[NotificationsTable.createdAt] = occurredAt
        }

        UsersTable
            .select { UsersTable.userId eq targetUserId }
            .single()
            .toUserModel()
    }

    private val AuditLogsTable.actionColumn get() = action
}
