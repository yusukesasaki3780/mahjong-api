package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - ユーザー情報の取得・更新・削除 API を定義したルーティングファイルです。
 * - path の userId と JWT の userId を比較し、本人のみ操作できるようにチェックします。
 */

import com.example.presentation.dto.PatchUserRequest
import com.example.presentation.dto.UpdateUserRequest
import com.example.presentation.dto.UserResponse
import com.example.usecase.user.DeleteUserUseCase
import com.example.usecase.user.GetUserUseCase
import com.example.usecase.user.PatchUserUseCase
import com.example.usecase.user.UpdateUserUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * ユーザ情報 API。
 */
fun Route.installUserRoutes(
    getUserUseCase: GetUserUseCase,
    updateUserUseCase: UpdateUserUseCase,
    patchUserUseCase: PatchUserUseCase,
    deleteUserUseCase: DeleteUserUseCase
) {
    route("/users/{userId}") {
        get {
            val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@get
            val user = getUserUseCase(userId) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(UserResponse.from(user))
        }

        put {
            val userId = call.userIdOrNull() ?: return@put call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@put
            val request = call.receive<UpdateUserRequest>()
            val updated = updateUserUseCase(
                UpdateUserUseCase.Command(
                    userId = userId,
                    name = request.name,
                    nickname = request.nickname,
                    storeName = request.storeName,
                    prefectureCode = request.prefectureCode,
                    email = request.email
                ),
                auditContext
            )
            call.respond(UserResponse.from(updated))
        }

        patch {
            val userId = call.userIdOrNull() ?: return@patch call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@patch
            val request = call.receive<PatchUserRequest>()
            val updated = patchUserUseCase(
                PatchUserUseCase.Command(
                    userId = userId,
                    name = request.name,
                    nickname = request.nickname,
                    storeName = request.storeName,
                    prefectureCode = request.prefectureCode,
                    email = request.email
                ),
                auditContext
            )
            call.respond(UserResponse.from(updated))
        }

        delete {
            val userId = call.userIdOrNull() ?: return@delete call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@delete
            val deleted = deleteUserUseCase(userId, auditContext)
            if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}

