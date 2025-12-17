package com.example.infrastructure.db.repository

import com.example.domain.model.Notification
import com.example.domain.repository.NotificationRepository
import com.example.infrastructure.db.tables.NotificationsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ExposedNotificationRepository : NotificationRepository {

    override suspend fun create(notification: Notification): Notification = dbQuery {
        val id = NotificationsTable.insert { row ->
            row[targetUserId] = notification.targetUserId
            row[actorUserId] = notification.actorUserId
            row[notificationType] = notification.type
            row[message] = notification.message
            row[isRead] = notification.isRead
            row[readAt] = notification.readAt
            row[relatedShiftId] = notification.relatedShiftId
            row[createdAt] = notification.createdAt
        } get NotificationsTable.id
        fetchById(id)
    }

    override suspend fun findByUser(userId: Long, unreadOnly: Boolean): List<Notification> = dbQuery {
        val baseQuery = NotificationsTable
            .select { NotificationsTable.targetUserId eq userId }
        if (unreadOnly) {
            baseQuery.andWhere { NotificationsTable.isRead eq false }
        }
        baseQuery
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
            .toList()
            .map(::toModel)
    }

    override suspend fun findById(notificationId: Long): Notification? = dbQuery {
        NotificationsTable
            .select { NotificationsTable.id eq notificationId }
            .singleOrNull()
            ?.let(::toModel)
    }

    override suspend fun markAsRead(notificationId: Long, userId: Long): Boolean = dbQuery {
        val now = Clock.System.now()
        val baseCondition = (NotificationsTable.id eq notificationId)
            .and(NotificationsTable.targetUserId eq userId)
        val updated = NotificationsTable.update({
            baseCondition.and(NotificationsTable.isRead eq false)
        }) { row ->
            row[NotificationsTable.isRead] = true
            row[NotificationsTable.readAt] = now
        }
        if (updated > 0) {
            true
        } else {
            NotificationsTable
                .select { baseCondition }
                .empty()
                .not()
        }
    }

    override suspend fun markAllAsRead(userId: Long): Int = dbQuery {
        val now = Clock.System.now()
        val condition = (NotificationsTable.targetUserId eq userId)
            .and(NotificationsTable.isRead eq false)

        NotificationsTable.update({ condition }) { row ->
            row[NotificationsTable.isRead] = true
            row[NotificationsTable.readAt] = now
        }
    }

    override suspend fun delete(notificationId: Long, userId: Long): Boolean = dbQuery {
        NotificationsTable.deleteWhere {
            (NotificationsTable.id eq notificationId) and (NotificationsTable.targetUserId eq userId)
        } > 0
    }

    override suspend fun countUnread(userId: Long): Int = dbQuery {
        NotificationsTable
            .select {
                (NotificationsTable.targetUserId eq userId) and (NotificationsTable.isRead eq false)
            }
            .count()
            .toInt()
    }

    private fun fetchById(id: Long): Notification =
        NotificationsTable
            .select { NotificationsTable.id eq id }
            .single()
            .let(::toModel)

    private fun toModel(row: ResultRow) =
        Notification(
            id = row[NotificationsTable.id],
            targetUserId = row[NotificationsTable.targetUserId],
            actorUserId = row[NotificationsTable.actorUserId],
            type = row[NotificationsTable.notificationType],
            message = row[NotificationsTable.message],
            isRead = row[NotificationsTable.isRead],
            relatedShiftId = row[NotificationsTable.relatedShiftId],
            createdAt = row[NotificationsTable.createdAt],
            readAt = row[NotificationsTable.readAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
