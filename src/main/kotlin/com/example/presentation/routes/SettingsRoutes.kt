package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - ゲーム設定の取得・更新・部分更新を扱うルート群です。
 * - JWT から取得したユーザーのみが自身の設定を操作できるように制御しています。
 */

import com.example.presentation.dto.GameSettingsResponse
import com.example.presentation.dto.PatchGameSettingsRequest
import com.example.presentation.dto.SpecialHourlyWageCreateRequest
import com.example.presentation.dto.SpecialHourlyWageResponse
import com.example.presentation.dto.UpdateGameSettingsRequest
import com.example.usecase.settings.GetGameSettingsUseCase
import com.example.usecase.settings.PatchGameSettingsUseCase
import com.example.usecase.settings.UpdateGameSettingsUseCase
import com.example.usecase.settings.CreateSpecialHourlyWageUseCase
import com.example.usecase.settings.DeleteSpecialHourlyWageUseCase
import com.example.usecase.settings.ListSpecialHourlyWagesUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * ゲーム設定 API。
 */
fun Route.installSettingsRoutes(
    getGameSettingsUseCase: GetGameSettingsUseCase,
    updateGameSettingsUseCase: UpdateGameSettingsUseCase,
    patchGameSettingsUseCase: PatchGameSettingsUseCase,
    listSpecialHourlyWagesUseCase: ListSpecialHourlyWagesUseCase,
    createSpecialHourlyWageUseCase: CreateSpecialHourlyWageUseCase,
    deleteSpecialHourlyWageUseCase: DeleteSpecialHourlyWageUseCase
) {
    route("/users/{userId}/settings") {
        get {
            val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@get
            val settings = getGameSettingsUseCase(userId) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(GameSettingsResponse.from(settings))
        }

        put {
            val userId = call.userIdOrNull() ?: return@put call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@put
            val request = call.receive<UpdateGameSettingsRequest>()
            val updated = updateGameSettingsUseCase(
                UpdateGameSettingsUseCase.Command(
                    userId = userId,
                    yonmaGameFee = request.yonmaGameFee,
                    sanmaGameFee = request.sanmaGameFee,
                    sanmaGameFeeBack = request.sanmaGameFeeBack,
                    yonmaTipUnit = request.yonmaTipUnit,
                    sanmaTipUnit = request.sanmaTipUnit,
                    wageType = request.wageType,
                    hourlyWage = request.hourlyWage,
                    fixedSalary = request.fixedSalary,
                    nightRateMultiplier = request.nightRateMultiplier,
                    baseMinWage = request.baseMinWage,
                    incomeTaxRate = request.incomeTaxRate,
                    transportPerShift = request.transportPerShift
                ),
                auditContext
            )
            call.respond(GameSettingsResponse.from(updated))
        }

        patch {
            val userId = call.userIdOrNull() ?: return@patch call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@patch
            val request = call.receive<PatchGameSettingsRequest>()
            val updated = patchGameSettingsUseCase(
                PatchGameSettingsUseCase.Command(
                    userId = userId,
                    yonmaGameFee = request.yonmaGameFee,
                    sanmaGameFee = request.sanmaGameFee,
                    sanmaGameFeeBack = request.sanmaGameFeeBack,
                    yonmaTipUnit = request.yonmaTipUnit,
                    sanmaTipUnit = request.sanmaTipUnit,
                    wageType = request.wageType,
                    hourlyWage = request.hourlyWage,
                    fixedSalary = request.fixedSalary,
                    nightRateMultiplier = request.nightRateMultiplier,
                    baseMinWage = request.baseMinWage,
                    incomeTaxRate = request.incomeTaxRate,
                    transportPerShift = request.transportPerShift
                ),
                auditContext
            )
            call.respond(GameSettingsResponse.from(updated))
        }

    }

    route("/settings/special-wages") {
        get {
            val userId = call.userId()
            val items = listSpecialHourlyWagesUseCase(userId)
                .map { SpecialHourlyWageResponse.from(it) }
            call.respond(HttpStatusCode.OK, items)
        }
        post {
            val userId = call.userId()
            val request = call.receive<SpecialHourlyWageCreateRequest>()
            val created = createSpecialHourlyWageUseCase(
                CreateSpecialHourlyWageUseCase.Command(
                    userId = userId,
                    label = request.label,
                    hourlyWage = request.hourlyWage
                )
            )
            call.respond(HttpStatusCode.Created, SpecialHourlyWageResponse.from(created))
        }
        delete("/{id}") {
            val userId = call.userId()
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respondValidationError("id", "INVALID_ID", "id は数値で指定してください。")
            val deleted = deleteSpecialHourlyWageUseCase(userId, id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

