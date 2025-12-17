package com.example.presentation.routes

import com.example.presentation.dto.PatchShiftBreakRequest
import com.example.presentation.dto.PatchShiftRequest
import com.example.presentation.dto.ShiftBoardResponseDto
import com.example.presentation.dto.ShiftBreakRequest
import com.example.presentation.dto.ShiftRequest
import com.example.presentation.dto.ShiftRequirementResponse
import com.example.presentation.dto.ShiftRequirementUpsertRequest
import com.example.presentation.dto.ShiftResponse
import com.example.presentation.dto.ShiftStatsResponse
import com.example.presentation.util.ShiftTimeCodec
import com.example.common.error.DomainValidationException
import com.example.usecase.shift.DeleteShiftUseCase
import com.example.usecase.shift.EditShiftUseCase
import com.example.usecase.shift.GetDailyShiftUseCase
import com.example.usecase.shift.GetMonthlyShiftUseCase
import com.example.usecase.shift.GetShiftBoardUseCase
import com.example.usecase.shift.GetShiftRangeUseCase
import com.example.usecase.shift.GetShiftStatsUseCase
import com.example.usecase.shift.PatchShiftUseCase
import com.example.usecase.shift.RegisterShiftUseCase
import com.example.usecase.shift.UpsertShiftRequirementUseCase
import com.example.usecase.user.GetUserUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

fun Route.installShiftRoutes(
    getUserUseCase: GetUserUseCase,
    getMonthlyShiftUseCase: GetMonthlyShiftUseCase,
    getDailyShiftUseCase: GetDailyShiftUseCase,
    getShiftRangeUseCase: GetShiftRangeUseCase,
    getShiftStatsUseCase: GetShiftStatsUseCase,
    getShiftBoardUseCase: GetShiftBoardUseCase,
    registerShiftUseCase: RegisterShiftUseCase,
    editShiftUseCase: EditShiftUseCase,
    patchShiftUseCase: PatchShiftUseCase,
    deleteShiftUseCase: DeleteShiftUseCase,
    upsertShiftRequirementUseCase: UpsertShiftRequirementUseCase
) {
    route("/users/{userId}/shifts") {
        get {
            val targetUserId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            val actorId = call.userId()
            val rangeType = call.request.queryParameters["rangeType"]?.lowercase() ?: "month"
            val shifts = when (rangeType) {
                "month" -> {
                    val yearMonth = call.queryYearMonth()
                        ?: return@get call.respondMissingYearMonth()
                    getMonthlyShiftUseCase(actorId, targetUserId, yearMonth)
                }

                "week" -> {
                    val range = call.requireLocalDateRange("start", "end") ?: return@get
                    getShiftRangeUseCase(actorId, targetUserId, range.first, range.second)
                }

                "day" -> {
                    val date = call.requireLocalDateParam("date") ?: return@get
                    getDailyShiftUseCase(actorId, targetUserId, date)
                }

                else -> {
                    call.respondValidationError(
                        field = "rangeType",
                        code = "INVALID_RANGE_TYPE",
                        message = "rangeType は month / week / day のいずれかを指定してください。"
                    )
                    return@get
                }
            }
            call.respond(shifts.map(ShiftResponse::from))
        }

        get("/stats") {
            val targetUserId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            val actorId = call.userId()
            val yearMonth = call.queryYearMonth() ?: return@get call.respondMissingYearMonth()
            val stats = getShiftStatsUseCase(actorId, targetUserId, yearMonth)
            val totalHours = stats.totalMinutes.toDouble().div(60.0).roundToTenths()
            val nightHours = stats.nightMinutes.toDouble().div(60.0).roundToTenths()
            val count = stats.shiftCount
            val avgHours = if (count > 0) (totalHours / count).roundToTenths() else 0.0
            call.respond(
                ShiftStatsResponse(
                    totalHours = totalHours,
                    nightHours = nightHours,
                    avgHours = avgHours,
                    count = count
                )
            )
        }

        post {
            val targetUserId = call.userIdOrNull() ?: return@post call.respondInvalidUserId()
            val actorId = call.userId()
            val auditContext = call.buildAuditContext(actorId)
            val request = call.receive<ShiftRequest>()
            val workDate = LocalDate.parse(request.workDate)
            val shiftTimes = call.parseShiftTimes(workDate, request.startTime, request.endTime) ?: return@post
            val breakWindows = call.parseBreakWindows(workDate, request.breaks, shiftTimes) ?: return@post
            val requestedStoreId = call.request.queryParameters["storeId"]
                ?.takeUnless { it.isBlank() }
                ?.let { raw ->
                    raw.toLongOrNull() ?: run {
                        call.respondValidationError(
                            field = "storeId",
                            code = "INVALID_STORE_ID",
                            message = "storeId は数値で指定してください。"
                        )
                        return@post
                    }
                }

            val created = registerShiftUseCase(
                RegisterShiftUseCase.Command(
                    actorId = actorId,
                    targetUserId = targetUserId,
                    requestedStoreId = requestedStoreId,
                    workDate = workDate,
                    startTime = shiftTimes.start,
                    endTime = shiftTimes.end,
                    memo = request.memo,
                    breaks = breakWindows.map {
                        RegisterShiftUseCase.BreakCommand(
                            breakStart = it.first,
                            breakEnd = it.second
                        )
                    },
                    specialHourlyWageId = request.specialHourlyWageId
                ),
                auditContext
            )
            call.respond(HttpStatusCode.Created, ShiftResponse.from(created))
        }

        put("/{shiftId}") {
            call.userIdOrNull() ?: return@put call.respondInvalidUserId()
            val actorId = call.userId()
            val auditContext = call.buildAuditContext(actorId)
            val shiftId = call.parameters["shiftId"]?.toLongOrNull()
                ?: return@put call.respondValidationError("shiftId", "INVALID_SHIFT_ID", "shiftId は数値で指定してください。")
            val request = call.receive<ShiftRequest>()
            val workDate = LocalDate.parse(request.workDate)
            val shiftTimes = call.parseShiftTimes(workDate, request.startTime, request.endTime) ?: return@put
            val breakWindows = call.parseBreakWindows(workDate, request.breaks, shiftTimes) ?: return@put

            val updated = editShiftUseCase(
                EditShiftUseCase.Command(
                    actorId = actorId,
                    shiftId = shiftId,
                    workDate = workDate,
                    startTime = shiftTimes.start,
                    endTime = shiftTimes.end,
                    memo = request.memo,
                    breaks = breakWindows.map {
                        EditShiftUseCase.BreakCommand(
                            breakStart = it.first,
                            breakEnd = it.second
                        )
                    },
                    createdAt = request.createdAt ?: shiftTimes.start,
                    specialHourlyWageId = request.specialHourlyWageId
                ),
                auditContext
            )
            call.respond(ShiftResponse.from(updated))
        }

        patch("/{shiftId}") {
            call.userIdOrNull() ?: return@patch call.respondInvalidUserId()
            val actorId = call.userId()
            val auditContext = call.buildAuditContext(actorId)
            val shiftId = call.parameters["shiftId"]?.toLongOrNull()
                ?: return@patch call.respondValidationError("shiftId", "INVALID_SHIFT_ID", "shiftId は数値で指定してください。")
            val request = call.receive<PatchShiftRequest>()
            val workDate = request.workDate?.let(LocalDate::parse)
            val shiftTimes = if (request.startTime != null && request.endTime != null && workDate != null) {
                call.parseShiftTimes(workDate, request.startTime, request.endTime) ?: return@patch
            } else null
            val breakCommands = request.breaks?.let { breaks ->
                if (breaks.isEmpty()) {
                    emptyList()
                } else {
                    val requiresTiming = breaks.any { !it.delete }
                    val date = if (requiresTiming) {
                        workDate ?: run {
                            call.respondValidationError(
                                field = "workDate",
                                code = "REQUIRED_FOR_BREAKS",
                                message = "休憩を更新する場合は workDate を指定してください。"
                            )
                            return@patch
                        }
                    } else {
                        workDate
                    }
                    val reference = if (requiresTiming) {
                        shiftTimes ?: run {
                            call.respondValidationError(
                                field = "startTime",
                                code = "REQUIRED_FOR_BREAKS",
                                message = "休憩を更新する場合は開始・終了時刻を指定してください。"
                            )
                            return@patch
                        }
                    } else {
                        shiftTimes
                    }
                    call.parseBreakPatchCommands(date, breaks, reference) ?: return@patch
                }
            }

            val updated = patchShiftUseCase(
                PatchShiftUseCase.Command(
                    actorId = actorId,
                    shiftId = shiftId,
                    workDate = workDate,
                    startTime = shiftTimes?.start,
                    endTime = shiftTimes?.end,
                    memo = request.memo,
                    breaks = breakCommands,
                    specialHourlyWageId = request.specialHourlyWageId,
                    clearSpecialHourlyWage = request.clearSpecialHourlyWage == true
                ),
                auditContext
            )
            call.respond(ShiftResponse.from(updated))
        }

        delete("/{shiftId}") {
            call.userIdOrNull() ?: return@delete call.respondInvalidUserId()
            val actorId = call.userId()
            val auditContext = call.buildAuditContext(actorId)
            val shiftId = call.parameters["shiftId"]?.toLongOrNull()
                ?: return@delete call.respondValidationError("shiftId", "INVALID_SHIFT_ID", "shiftId は数値で指定してください。")
            val deleted = deleteShiftUseCase(actorId, shiftId, auditContext)
            if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
        }
    }

    route("/stores/{storeId}/shift-board") {
        get {
            val actorId = call.userId()
            val storeId = call.parameters["storeId"]?.toLongOrNull()
                ?: return@get call.respondValidationError(
                    field = "storeId",
                    code = "INVALID_STORE_ID",
                    message = "storeId は数値で指定してください。"
                )
            val startDate = call.requireLocalDateParam("startDate") ?: return@get
            val endDate = call.requireLocalDateParam("endDate") ?: return@get
            val includeDeleted = call.request.queryParameters["includeDeletedUsers"]?.toBooleanStrictOrNull() ?: false

            val result = try {
                getShiftBoardUseCase(
                    GetShiftBoardUseCase.Command(
                        actorId = actorId,
                        storeId = storeId,
                        startDate = startDate,
                        endDate = endDate,
                        includeDeletedUsers = includeDeleted
                    )
                )
            } catch (ex: DomainValidationException) {
                call.respondValidationErrors(ex.violations, ex.message ?: "入力内容を確認してください。")
                return@get
            }
            call.respond(ShiftBoardResponseDto.from(result))
        }
    }

    route("/stores/{storeId}/shift-requirements") {
        put {
            val actorId = call.userId()
            val actor = getUserUseCase(actorId)
                ?: return@put call.respondForbidden("アカウント情報を取得できません。")
            val storeId = call.parameters["storeId"]?.toLongOrNull()
                ?: return@put call.respondValidationError(
                    field = "storeId",
                    code = "INVALID_STORE_ID",
                    message = "storeId は数値で指定してください。"
                )
            val request = call.receive<ShiftRequirementUpsertRequest>()
            val targetDate = runCatching { LocalDate.parse(request.targetDate) }.getOrElse {
                call.respondValidationError(
                    field = "targetDate",
                    code = "INVALID_DATE",
                    message = "targetDate は YYYY-MM-DD 形式で指定してください。"
                )
                return@put
            }

            val result = try {
                upsertShiftRequirementUseCase(
                    UpsertShiftRequirementUseCase.Command(
                        actor = actor,
                        storeId = storeId,
                        targetDate = targetDate,
                        shiftType = request.shiftType,
                        startRequired = request.startRequired,
                        endRequired = request.endRequired
                    )
                )
            } catch (ex: DomainValidationException) {
                call.respondValidationErrors(ex.violations, ex.message ?: "入力内容を確認してください。")
                return@put
            }
            call.respond(HttpStatusCode.OK, ShiftRequirementResponse.from(result))
        }
    }
}

private suspend fun ApplicationCall.requireLocalDateParam(paramName: String): LocalDate? {
    val raw = request.queryParameters[paramName]
        ?: run {
            respondValidationError(
                field = paramName,
                code = "REQUIRED",
                message = "$paramName は YYYY-MM-DD 形式で入力してください。"
            )
            return null
        }
    val parsed = parseFlexibleLocalDate(raw)
    if (parsed == null) {
        respondValidationError(
            field = paramName,
            code = "INVALID_DATE",
            message = "$paramName は YYYY-MM-DD 形式、または ISO8601 日付文字列で入力してください。"
        )
        return null
    }
    return parsed
}

private suspend fun ApplicationCall.requireLocalDateRange(
    startParam: String,
    endParam: String
): Pair<LocalDate, LocalDate>? {
    val start = requireLocalDateParam(startParam) ?: return null
    val end = requireLocalDateParam(endParam) ?: return null
    if (end < start) {
        respondValidationError(
            field = endParam,
            code = "INVALID_DATE_RANGE",
            message = "$endParam は $startParam 以降の日付を指定してください。"
        )
        return null
    }
    return start to end
}

private data class ParsedShiftTimes(
    val start: Instant,
    val end: Instant,
    val startLocal: LocalTime,
    val endLocal: LocalTime,
    val crossesMidnight: Boolean
)

private suspend fun ApplicationCall.parseShiftTimes(
    workDate: LocalDate,
    start: String,
    end: String
): ParsedShiftTimes? {
    val startLocal = parseLocalTime("startTime", start) ?: return null
    val endLocal = parseLocalTime("endTime", end) ?: return null
    var crossesMidnight = false
    var endDate = workDate
    if (endLocal <= startLocal) {
        crossesMidnight = true
        endDate = workDate.plusDays(1)
    }
    val startInstant = ShiftTimeCodec.toInstant(workDate, startLocal)
    val endInstant = ShiftTimeCodec.toInstant(endDate, endLocal)
    return ParsedShiftTimes(
        start = startInstant,
        end = endInstant,
        startLocal = startLocal,
        endLocal = endLocal,
        crossesMidnight = crossesMidnight
    )
}

private suspend fun ApplicationCall.parseBreakWindows(
    workDate: LocalDate,
    breaks: List<ShiftBreakRequest>,
    shiftTimes: ParsedShiftTimes
): List<Pair<Instant, Instant>>? {
    if (breaks.isEmpty()) return emptyList()
    val result = mutableListOf<Pair<Instant, Instant>>()
    breaks.forEachIndexed { index, br ->
        val startLocal = parseLocalTime("breaks[$index].startTime", br.startTime) ?: return null
        val startDate = resolveBreakDate(workDate, shiftTimes.startLocal, startLocal, shiftTimes.crossesMidnight)
        val endLocal = parseLocalTime("breaks[$index].endTime", br.endTime) ?: return null
        var endDate = resolveBreakDate(workDate, shiftTimes.startLocal, endLocal, shiftTimes.crossesMidnight)
        if (endDate < startDate || (endDate == startDate && endLocal <= startLocal)) {
            endDate = endDate.plusDays(1)
        }
        result += ShiftTimeCodec.toInstant(startDate, startLocal) to ShiftTimeCodec.toInstant(endDate, endLocal)
    }
    return result
}

private fun parseFlexibleLocalDate(raw: String): LocalDate? {
    return runCatching { LocalDate.parse(raw) }.getOrElse {
        runCatching {
            Instant.parse(raw).toLocalDateTime(TimeZone.currentSystemDefault()).date
        }.getOrNull()
    }
}

private suspend fun ApplicationCall.parseBreakPatchCommands(
    workDate: LocalDate?,
    requests: List<PatchShiftBreakRequest>,
    shiftTimes: ParsedShiftTimes?
): List<PatchShiftUseCase.BreakPatchCommand>? {
    val result = mutableListOf<PatchShiftUseCase.BreakPatchCommand>()
    requests.forEachIndexed { index, br ->
            if (br.delete) {
                if (br.id == null) {
                    respondValidationError(
                        field = "breaks[$index].id",
                        code = "ID_REQUIRED_FOR_DELETE",
                        message = "休憩を削除する場合は break id を指定してください。"
                    )
                    return null
                }
            result += PatchShiftUseCase.BreakPatchCommand(
                id = br.id,
                breakStart = null,
                breakEnd = null,
                delete = true
            )
        } else {
            val date = workDate ?: run {
                respondValidationError(
                    field = "workDate",
                    code = "REQUIRED_FOR_BREAKS",
                    message = "休憩を更新する場合は workDate を指定してください。"
                )
                return null
            }
            val reference = shiftTimes ?: run {
                respondValidationError(
                    field = "startTime",
                    code = "REQUIRED_FOR_BREAKS",
                    message = "休憩を更新する場合は開始・終了時刻を指定してください。"
                )
                return null
            }
            val startValue = br.startTime ?: return respondBreakFieldMissing(index, "startTime")
            val endValue = br.endTime ?: return respondBreakFieldMissing(index, "endTime")
            val startLocal = parseLocalTime("breaks[$index].startTime", startValue) ?: return null
            val startDate = resolveBreakDate(date, reference.startLocal, startLocal, reference.crossesMidnight)
            val endLocal = parseLocalTime("breaks[$index].endTime", endValue) ?: return null
            var endDate = resolveBreakDate(date, reference.startLocal, endLocal, reference.crossesMidnight)
            if (endDate < startDate || (endDate == startDate && endLocal <= startLocal)) {
                endDate = endDate.plusDays(1)
            }
            result += PatchShiftUseCase.BreakPatchCommand(
                id = br.id,
                breakStart = ShiftTimeCodec.toInstant(startDate, startLocal),
                breakEnd = ShiftTimeCodec.toInstant(endDate, endLocal),
                delete = false
            )
        }
    }
    return result
}

private fun resolveBreakDate(
    workDate: LocalDate,
    shiftStart: LocalTime,
    target: LocalTime,
    crossesMidnight: Boolean
): LocalDate =
    if (crossesMidnight && target < shiftStart) workDate.plusDays(1) else workDate

private suspend fun ApplicationCall.parseLocalTime(field: String, value: String): LocalTime? =
    runCatching { ShiftTimeCodec.parseLocalTime(value) }.getOrElse {
        respondValidationError(
            field = field,
            code = "INVALID_TIME_FORMAT",
            message = "$field は HH:mm 形式で入力してください。"
        )
        null
    }

private suspend fun ApplicationCall.respondBreakFieldMissing(index: Int, field: String): Nothing? {
    respondValidationError(
        field = "breaks[$index].$field",
        code = "REQUIRED",
        message = "休憩を更新する場合は $field を入力してください。"
    )
    return null
}

private fun LocalDate.plusDays(days: Int): LocalDate =
    this.plus(days, DateTimeUnit.DAY)

private fun Double.roundToTenths(): Double =
    (this * 10).roundToInt() / 10.0
