package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - 管理者のみが利用できるユーザー管理 API を定義します。
 */

import com.example.common.error.DomainValidationException
import com.example.domain.model.User
import com.example.presentation.dto.AdminPasswordResetRequest
import com.example.presentation.dto.AdminPasswordResetResponse
import com.example.presentation.dto.AdminUpdateAdminFlagRequest
import com.example.presentation.dto.AdminUserSummaryResponse
import com.example.presentation.util.ValidationMessageResolver
import com.example.usecase.user.AdminDeleteUserUseCase
import com.example.usecase.user.AdminResetUserPasswordUseCase
import com.example.usecase.user.AdminRestoreUserUseCase
import com.example.usecase.user.AdminUpdateUserAdminFlagUseCase
import com.example.usecase.user.GetUserUseCase
import com.example.usecase.user.ListGeneralUsersUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.installAdminUserRoutes(
    getUserUseCase: GetUserUseCase,
    listGeneralUsersUseCase: ListGeneralUsersUseCase,
    adminDeleteUserUseCase: AdminDeleteUserUseCase,
    adminResetUserPasswordUseCase: AdminResetUserPasswordUseCase,
    adminRestoreUserUseCase: AdminRestoreUserUseCase,
    adminUpdateUserAdminFlagUseCase: AdminUpdateUserAdminFlagUseCase
) {
    route("/admin/users") {
        get {
            val admin = call.requireAdmin(getUserUseCase) ?: return@get
            val users = listGeneralUsersUseCase(
                storeId = admin.storeId,
                includeDeleted = true,
                includeAdmins = true
            )
            call.respond(users.map(AdminUserSummaryResponse::from))
        }

        delete("/{userId}") {
            val admin = call.requireAdmin(getUserUseCase) ?: return@delete
            val adminId = admin.id ?: return@delete call.respondInvalidUserId()
            val targetId = call.parameters["userId"]?.toLongOrNull()
                ?: return@delete call.respondInvalidUserId()
            val auditContext = call.buildAuditContext(adminId)
            try {
                val deleted = adminDeleteUserUseCase(adminId, admin.storeId, targetId, auditContext)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (ex: DomainValidationException) {
                call.respondValidationErrors(ex.violations, ex.message ?: ValidationMessageResolver.defaultMessage())
            }
        }

        post("/{userId}/password-reset") {
            val admin = call.requireAdmin(getUserUseCase) ?: return@post
            val targetId = call.parameters["userId"]?.toLongOrNull()
                ?: return@post call.respondInvalidUserId()
            val request = call.receive<AdminPasswordResetRequest>()
            try {
                val updated = adminResetUserPasswordUseCase(
                    adminStoreId = admin.storeId,
                    targetUserId = targetId,
                    newPassword = request.newPassword
                )
                if (updated) {
                    call.respond(HttpStatusCode.OK, AdminPasswordResetResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (ex: DomainValidationException) {
                call.respondValidationErrors(ex.violations, ex.message ?: ValidationMessageResolver.defaultMessage())
            }
        }

        patch("/{userId}/admin") {
            val admin = call.requireAdmin(getUserUseCase) ?: return@patch
            val adminId = admin.id ?: return@patch call.respondInvalidUserId()
            val targetId = call.parameters["userId"]?.toLongOrNull()
                ?: return@patch call.respondInvalidUserId()
            val request = call.receive<AdminUpdateAdminFlagRequest>()
            val auditContext = call.buildAuditContext(adminId)
            try {
                val updated = adminUpdateUserAdminFlagUseCase(
                    adminId = adminId,
                    adminName = admin.name,
                    adminStoreId = admin.storeId,
                    targetUserId = targetId,
                    isAdmin = request.isAdmin,
                    auditContext = auditContext
                ) ?: return@patch call.respond(HttpStatusCode.NotFound)
                call.respond(AdminUserSummaryResponse.from(updated))
            } catch (ex: DomainValidationException) {
                call.respondValidationErrors(ex.violations, ex.message ?: ValidationMessageResolver.defaultMessage())
            }
        }

        patch("/{userId}/restore") {
            val admin = call.requireAdmin(getUserUseCase) ?: return@patch
            val adminId = admin.id ?: return@patch call.respondInvalidUserId()
            val targetId = call.parameters["userId"]?.toLongOrNull()
                ?: return@patch call.respondInvalidUserId()
            val auditContext = call.buildAuditContext(adminId)
            try {
                val restored = adminRestoreUserUseCase(adminId, admin.storeId, targetId, auditContext)
                if (restored) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (ex: DomainValidationException) {
                call.respondValidationErrors(ex.violations, ex.message ?: ValidationMessageResolver.defaultMessage())
            }
        }
    }
}

private suspend fun ApplicationCall.requireAdmin(
    getUserUseCase: GetUserUseCase
): User? {
    val actorId = userId()
    val actor = getUserUseCase(actorId)
    return if (actor?.isAdmin == true) {
        actor
    } else {
        respondForbidden()
        null
    }
}
