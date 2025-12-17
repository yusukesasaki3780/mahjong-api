package com.example.presentation.routes

import com.example.presentation.dto.NotificationBulkUpdateResponse
import com.example.presentation.dto.NotificationResponse
import com.example.presentation.dto.NotificationUnreadCountResponse
import com.example.usecase.notification.DeleteNotificationUseCase
import com.example.usecase.notification.GetNotificationsUseCase
import com.example.usecase.notification.GetUnreadNotificationCountUseCase
import com.example.usecase.notification.MarkAllNotificationsReadUseCase
import com.example.usecase.notification.MarkNotificationReadUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

fun Route.installNotificationRoutes(
    getNotificationsUseCase: GetNotificationsUseCase,
    markNotificationReadUseCase: MarkNotificationReadUseCase,
    markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    deleteNotificationUseCase: DeleteNotificationUseCase,
    getUnreadNotificationCountUseCase: GetUnreadNotificationCountUseCase
) {
    route("/notifications") {
        get {
            val actorId = call.userId()
            val status = call.request.queryParameters["status"]
            val onlyUnread = status?.equals("UNREAD", ignoreCase = true) ?: false
            val notifications = getNotificationsUseCase(actorId, onlyUnread)
            call.respond(notifications.map(NotificationResponse::from))
        }

        patch("/{notificationId}/read") {
            val actorId = call.userId()
            val notificationId = call.parseNotificationId() ?: return@patch
            val updated = markNotificationReadUseCase(actorId, notificationId)
            if (updated) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{notificationId}") {
            val actorId = call.userId()
            val notificationId = call.parseNotificationId() ?: return@delete
            when (deleteNotificationUseCase(actorId, notificationId)) {
                DeleteNotificationUseCase.Result.Deleted -> call.respond(HttpStatusCode.NoContent)
                DeleteNotificationUseCase.Result.NotFound -> call.respond(HttpStatusCode.NotFound)
                DeleteNotificationUseCase.Result.Forbidden -> call.respondForbidden()
            }
        }
    }

    route("/users/{userId}/notifications") {
        get {
            val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@get
            val onlyUnread = call.request.queryParameters["onlyUnread"]?.toBooleanStrictOrNull() ?: true
            val notifications = getNotificationsUseCase(userId, onlyUnread)
            call.respond(notifications.map(NotificationResponse::from))
        }

        get("/unread-count") {
            val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@get
            val count = getUnreadNotificationCountUseCase(userId)
            call.respond(NotificationUnreadCountResponse(count = count))
        }

        patch("/read-all") {
            val userId = call.userIdOrNull() ?: return@patch call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@patch
            val count = markAllNotificationsReadUseCase(userId)
            call.respond(HttpStatusCode.OK, NotificationBulkUpdateResponse(updatedCount = count))
        }

        patch("/{notificationId}/read") {
            val userId = call.userIdOrNull() ?: return@patch call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@patch
            val notificationId = call.parseNotificationId() ?: return@patch
            val updated = markNotificationReadUseCase(userId, notificationId)
            if (updated) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{notificationId}") {
            val userId = call.userIdOrNull() ?: return@delete call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@delete
            val notificationId = call.parseNotificationId() ?: return@delete
            when (deleteNotificationUseCase(userId, notificationId)) {
                DeleteNotificationUseCase.Result.Deleted -> call.respond(HttpStatusCode.NoContent)
                DeleteNotificationUseCase.Result.NotFound -> call.respond(HttpStatusCode.NotFound)
                DeleteNotificationUseCase.Result.Forbidden -> call.respondForbidden()
            }
        }
    }
}

private suspend fun ApplicationCall.parseNotificationId(): Long? {
    val id = parameters["notificationId"]?.toLongOrNull()
    if (id == null) {
        respondValidationError(
            field = "notificationId",
            code = "INVALID_NOTIFICATION_ID",
            message = "notificationId は数値で指定してください。"
        )
        return null
    }
    return id
}
