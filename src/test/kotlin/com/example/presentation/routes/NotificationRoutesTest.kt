package com.example.presentation.routes

import com.example.domain.model.Notification
import com.example.domain.model.NotificationType
import com.example.usecase.notification.DeleteNotificationUseCase
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotificationRoutesTest : RoutesTestBase() {

    @Test
    fun `get notifications defaults to unread only`() = testApplication {
        val now = Clock.System.now()
        coEvery { getNotificationsUseCase(1, true) } returns listOf(
            Notification(
                id = 1,
                targetUserId = 1,
                actorUserId = 2,
                type = NotificationType.SHIFT_UPDATED,
                message = "updated",
                isRead = false,
                createdAt = now,
                relatedShiftId = 99,
                readAt = null
            )
        )
        installRoutes()

        val response = client.get("/users/1/notifications") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getNotificationsUseCase(1, true) }
    }

    @Test
    fun `get notifications can include read`() = testApplication {
        coEvery { getNotificationsUseCase(2, false) } returns emptyList()
        installRoutes()

        val response = client.get("/users/2/notifications?onlyUnread=false") {
            withAuth(userId = 2)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getNotificationsUseCase(2, false) }
    }

    @Test
    fun `global notifications endpoint uses actor id`() = testApplication {
        coEvery { getNotificationsUseCase(9, false) } returns emptyList()
        installRoutes()

        val response = client.get("/notifications") {
            withAuth(userId = 9)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getNotificationsUseCase(9, false) }
    }

    @Test
    fun `global notifications endpoint filters unread`() = testApplication {
        coEvery { getNotificationsUseCase(9, true) } returns emptyList()
        installRoutes()

        val response = client.get("/notifications?status=UNREAD") {
            withAuth(userId = 9)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getNotificationsUseCase(9, true) }
    }

    @Test
    fun `global notification read endpoint updates`() = testApplication {
        coEvery { markNotificationReadUseCase(8, 33) } returns true
        installRoutes()

        val response = client.patch("/notifications/33/read") {
            withAuth(userId = 8)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { markNotificationReadUseCase(8, 33) }
    }

    @Test
    fun `fetch unread count`() = testApplication {
        coEvery { getUnreadNotificationCountUseCase(3) } returns 5
        installRoutes()

        val response = client.get("/users/3/notifications/unread-count") {
            withAuth(userId = 3)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getUnreadNotificationCountUseCase(3) }
    }

    @Test
    fun `mark notification read`() = testApplication {
        coEvery { markNotificationReadUseCase(1, 5) } returns true
        installRoutes()

        val response = client.patch("/users/1/notifications/5/read") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        coVerify { markNotificationReadUseCase(1, 5) }
    }

    @Test
    fun `mark all notifications read`() = testApplication {
        coEvery { markAllNotificationsReadUseCase(1) } returns 3
        installRoutes()

        val response = client.patch("/users/1/notifications/read-all") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { markAllNotificationsReadUseCase(1) }
    }

    @Test
    fun `delete notification`() = testApplication {
        coEvery { deleteNotificationUseCase(4, 10) } returns DeleteNotificationUseCase.Result.Deleted
        installRoutes()

        val response = client.delete("/users/4/notifications/10") {
            withAuth(userId = 4)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        coVerify { deleteNotificationUseCase(4, 10) }
    }

    @Test
    fun `global delete notification`() = testApplication {
        coEvery { deleteNotificationUseCase(6, 44) } returns DeleteNotificationUseCase.Result.Deleted
        installRoutes()

        val response = client.delete("/notifications/44") {
            withAuth(userId = 6)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        coVerify { deleteNotificationUseCase(6, 44) }
    }

    @Test
    fun `delete notification forbidden`() = testApplication {
        coEvery { deleteNotificationUseCase(4, 12) } returns DeleteNotificationUseCase.Result.Forbidden
        installRoutes()

        val response = client.delete("/notifications/12") {
            withAuth(userId = 4)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
