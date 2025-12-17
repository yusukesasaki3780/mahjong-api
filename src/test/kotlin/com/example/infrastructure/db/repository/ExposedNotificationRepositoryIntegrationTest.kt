package com.example.infrastructure.db.repository

import com.example.domain.model.Notification
import com.example.domain.model.NotificationType
import com.example.infrastructure.db.tables.UsersTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExposedNotificationRepositoryIntegrationTest : RepositoryTestBase() {

    private val repository = ExposedNotificationRepository()

    @Test
    fun `create and fetch notifications`() = runDbTest {
        val userId = seedUser()
        val created = repository.create(
            Notification(
                id = null,
                targetUserId = userId,
                actorUserId = null,
                type = NotificationType.SHIFT_CREATED,
                message = "シフトが追加されました",
                createdAt = Clock.System.now(),
                relatedShiftId = null
            )
        )
        assertEquals(NotificationType.SHIFT_CREATED, created.type)

        val unread = repository.findByUser(userId, unreadOnly = true)
        assertEquals(1, unread.size)
        assertEquals(created.id, unread.first().id)
    }

    @Test
    fun `mark notifications as read`() = runDbTest {
        val userId = seedUser()
        val n1 = repository.create(
            Notification(
                id = null,
                targetUserId = userId,
                actorUserId = null,
                type = NotificationType.SHIFT_UPDATED,
                message = "シフトが更新されました",
                createdAt = Clock.System.now(),
                relatedShiftId = null
            )
        )
        val n2 = repository.create(
            Notification(
                id = null,
                targetUserId = userId,
                actorUserId = null,
                type = NotificationType.SHIFT_DELETED,
                message = "シフトが削除されました",
                createdAt = Clock.System.now(),
                relatedShiftId = null
            )
        )

        val updated = repository.markAsRead(n1.id!!, userId)
        assertTrue(updated)
        val bulkCount = repository.markAllAsRead(userId)
        assertEquals(1, bulkCount) // only n2 remained unread
        val unread = repository.findByUser(userId, unreadOnly = true)
        assertTrue(unread.isEmpty())
    }

    @Test
    fun `delete notifications and count unread`() = runDbTest {
        val userId = seedUser()
        val n1 = repository.create(
            Notification(
                id = null,
                targetUserId = userId,
                actorUserId = null,
                type = NotificationType.SHIFT_CREATED,
                message = "new shift",
                createdAt = Clock.System.now(),
                relatedShiftId = null
            )
        )
        repository.create(
            Notification(
                id = null,
                targetUserId = userId,
                actorUserId = null,
                type = NotificationType.SHIFT_UPDATED,
                message = "updated shift",
                createdAt = Clock.System.now(),
                relatedShiftId = null
            )
        )

        assertEquals(2, repository.countUnread(userId))
        val deleted = repository.delete(n1.id!!, userId)
        assertTrue(deleted)
        assertEquals(1, repository.countUnread(userId))
    }

    private fun seedUser(): Long {
        val now = Clock.System.now()
        return transaction {
            UsersTable.insert {
                it[name] = "NotifyUser"
                it[nickname] = "notify"
                it[storeId] = 1
                it[storeName] = "Store"
                it[prefectureCode] = "01"
                it[email] = "notify-${now.toEpochMilliseconds()}@example.com"
                it[zooId] = (now.toEpochMilliseconds() % 900_000).toInt() + 1
                it[UsersTable.isAdmin] = false
                it[UsersTable.isDeleted] = false
                it[createdAt] = now
                it[updatedAt] = now
            } get UsersTable.userId
        }
    }
}
