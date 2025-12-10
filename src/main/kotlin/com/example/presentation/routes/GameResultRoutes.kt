package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - ゲーム結果登録・更新・削除・集計を扱う REST エンドポイント群です。
 * - JWT の userId とパスの userId を照合し、UseCase への橋渡し役を担います。
 */

import com.example.presentation.dto.GameResultResponse
import com.example.presentation.dto.PatchGameResultRequest
import com.example.presentation.dto.UpsertGameResultRequest
import com.example.presentation.dto.UserStatsResponse
import com.example.usecase.game.DeleteGameResultUseCase
import com.example.usecase.game.EditGameResultUseCase
import com.example.usecase.game.GetGameResultUseCase
import com.example.usecase.game.GetUserStatsUseCase
import com.example.usecase.game.PatchGameResultUseCase
import com.example.usecase.game.RecordGameResultUseCase
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/**
 * 成績 API。
 */
fun Route.installGameResultRoutes(
    recordGameResultUseCase: RecordGameResultUseCase,
    editGameResultUseCase: EditGameResultUseCase,
    patchGameResultUseCase: PatchGameResultUseCase,
    deleteGameResultUseCase: DeleteGameResultUseCase,
    getGameResultUseCase: GetGameResultUseCase,
    getUserStatsUseCase: GetUserStatsUseCase
) {
    route("/users/{userId}/results") {
        get {
            val logger = call.application.environment.log
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            logger.debug("GET /users/{}/results Authorization={}", call.parameters["userId"], authHeader)
            val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            val jwtUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
            logger.debug("JWT principal userId={}, path userId={}", jwtUserId, userId)
            call.requireUserAccess(userId) ?: run {
                logger.warn("Access denied for /users/{}/results (principal={})", userId, jwtUserId)
                return@get
            }
            val range = call.statsRangeOrNull() ?: return@get call.respondMissingStatsRange()
            logger.debug("Authorized results request userId={} range={}", userId, range)

            val stats = getUserStatsUseCase(
                GetUserStatsUseCase.Command(
                    userId = userId,
                    range = range
                )
            )
            call.respond(UserStatsResponse.from(stats))
        }

        get("/{id}") {
            val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@get
            val resultId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respondValidationError("id", "INVALID_RESULT_ID", "resultId は数値で指定してください。")
            val result = getGameResultUseCase(userId, resultId)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(GameResultResponse.from(result))
        }

        post {
            val userId = call.userIdOrNull() ?: return@post call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@post
            val request = call.receive<UpsertGameResultRequest>()
            val created = recordGameResultUseCase(
                RecordGameResultUseCase.Command(
                    userId = userId,
                    gameType = request.gameType,
                    playedAt = request.playedAt,
                    place = request.place,
                    baseIncome = request.baseIncome,
                    tipCount = request.tipCount,
                    tipIncome = request.tipIncome,
                    otherIncome = request.otherIncome,
                    totalIncome = request.totalIncome,
                    note = request.note
                )
            )
            call.respond(HttpStatusCode.Created, GameResultResponse.from(created))
        }

        put("/{id}") {
            val userId = call.userIdOrNull() ?: return@put call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@put
            val resultId = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respondValidationError("id", "INVALID_RESULT_ID", "resultId は数値で指定してください。")

            val request = call.receive<UpsertGameResultRequest>()
            val zone = TimeZone.currentSystemDefault()
            val updated = editGameResultUseCase(
                EditGameResultUseCase.Command(
                    id = resultId,
                    userId = userId,
                    gameType = request.gameType,
                    playedAt = request.playedAt,
                    place = request.place,
                    baseIncome = request.baseIncome,
                    tipCount = request.tipCount,
                    tipIncome = request.tipIncome,
                    otherIncome = request.otherIncome,
                    totalIncome = request.totalIncome,
                    note = request.note,
                    createdAt = request.createdAt ?: request.playedAt.atStartOfDayIn(zone)
                ),
                auditContext
            )
            call.respond(GameResultResponse.from(updated))
        }

        patch("/{id}") {
            val userId = call.userIdOrNull() ?: return@patch call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@patch
            val resultId = call.parameters["id"]?.toLongOrNull()
                ?: return@patch call.respondValidationError("id", "INVALID_RESULT_ID", "resultId は数値で指定してください。")

            val request = call.receive<PatchGameResultRequest>()
            val updated = patchGameResultUseCase(
                PatchGameResultUseCase.Command(
                    userId = userId,
                    resultId = resultId,
                    gameType = request.gameType,
                    playedAt = request.playedAt,
                    place = request.place,
                    baseIncome = request.baseIncome,
                    tipCount = request.tipCount,
                    tipIncome = request.tipIncome,
                    otherIncome = request.otherIncome,
                    totalIncome = request.totalIncome,
                    note = request.note
                ),
                auditContext
            )
            call.respond(GameResultResponse.from(updated))
        }

        delete("/{id}") {
            val userId = call.userIdOrNull() ?: return@delete call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@delete
            val resultId = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respondValidationError("id", "INVALID_RESULT_ID", "resultId は数値で指定してください。")
            val deleted = deleteGameResultUseCase(resultId, auditContext)
            if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }
}

