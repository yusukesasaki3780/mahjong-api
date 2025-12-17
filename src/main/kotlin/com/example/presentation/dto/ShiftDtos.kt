@file:OptIn(ExperimentalSerializationApi::class)

package com.example.presentation.dto

import com.example.domain.model.Shift
import com.example.domain.model.ShiftSlotType
import com.example.domain.model.ShiftRequirement
import com.example.domain.model.SpecialHourlyWage
import com.example.presentation.util.ShiftTimeCodec
import com.example.usecase.shift.GetShiftBoardUseCase
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ShiftBreakRequest(
    @JsonNames("startTime", "breakStart")
    val startTime: String,
    @JsonNames("endTime", "breakEnd")
    val endTime: String
)

@Serializable
data class ShiftRequest(
    @JsonNames("workDate", "date")
    val workDate: String,
    @JsonNames("startTime")
    val startTime: String,
    @JsonNames("endTime")
    val endTime: String,
    @JsonNames("memo", "notes")
    val memo: String? = null,
    val breaks: List<ShiftBreakRequest> = emptyList(),
    val createdAt: Instant? = null,
    val breakMinutes: Int? = null,
    val specialHourlyWageId: Long? = null
)

@Serializable
data class PatchShiftBreakRequest(
    val id: Long? = null,
    @JsonNames("startTime", "breakStart")
    val startTime: String? = null,
    @JsonNames("endTime", "breakEnd")
    val endTime: String? = null,
    val delete: Boolean = false
)

@Serializable
data class PatchShiftRequest(
    @JsonNames("workDate", "date")
    val workDate: String? = null,
    @JsonNames("startTime")
    val startTime: String? = null,
    @JsonNames("endTime")
    val endTime: String? = null,
    val breakMinutes: Int? = null,
    @JsonNames("memo", "notes")
    val memo: String? = null,
    val breaks: List<PatchShiftBreakRequest>? = null,
    val specialHourlyWageId: Long? = null,
    val clearSpecialHourlyWage: Boolean? = null
)

@Serializable
data class ShiftBreakResponse(
    val startTime: String,
    val endTime: String
)

@Serializable
data class ShiftResponse(
    val id: Long,
    val date: String,
    val workDate: String,
    val startTime: String,
    val endTime: String,
    val startDateTime: String,
    val endDateTime: String,
    val endDayOffset: Int,
    val memo: String?,
    @SerialName("specialWageId")
    val specialHourlyWageId: Long?,
    val specialHourlyWage: SpecialHourlyWageResponse?,
    val breaks: List<ShiftBreakResponse>
) {
    companion object {
        fun from(shift: Shift): ShiftResponse {
            val aligned = ShiftTimeCodec.alignShiftWindow(shift.workDate, shift.startTime, shift.endTime)
            return ShiftResponse(
                id = shift.id!!,
                date = shift.workDate.toString(),
                workDate = shift.workDate.toString(),
                startTime = ShiftTimeCodec.format(aligned.startInstant),
                endTime = ShiftTimeCodec.format(aligned.endInstant),
                startDateTime = ShiftTimeCodec.formatDateTime(aligned.startInstant),
                endDateTime = ShiftTimeCodec.formatDateTime(aligned.endInstant),
                endDayOffset = aligned.endDayOffset,
                memo = shift.memo,
                specialHourlyWageId = shift.specialHourlyWageId ?: shift.specialHourlyWage?.id,
                specialHourlyWage = shift.specialHourlyWage?.let { SpecialHourlyWageResponse.from(it) },
                breaks = shift.breaks.map {
                    ShiftBreakResponse(
                        startTime = ShiftTimeCodec.format(it.breakStart),
                        endTime = ShiftTimeCodec.format(it.breakEnd)
                    )
                }
            )
        }
    }
}

@Serializable
data class SpecialHourlyWageResponse(
    val id: Long,
    val label: String,
    val hourlyWage: Int
) {
    companion object {
        fun from(model: SpecialHourlyWage) = SpecialHourlyWageResponse(
            id = model.id,
            label = model.label,
            hourlyWage = model.hourlyWage
        )
    }
}

@Serializable
data class ShiftStatsResponse(
    val totalHours: Double,
    val nightHours: Double,
    val avgHours: Double,
    val count: Int
)

@Serializable
data class ShiftRequirementUpsertRequest(
    val targetDate: String,
    val shiftType: ShiftSlotType,
    val startRequired: Int,
    val endRequired: Int
)

@Serializable
data class ShiftRequirementResponse(
    val id: Long,
    val storeId: Long,
    val targetDate: String,
    val shiftType: ShiftSlotType,
    val startRequired: Int,
    val endRequired: Int
) {
    companion object {
        fun from(model: ShiftRequirement) = ShiftRequirementResponse(
            id = model.id ?: error("Requirement ID is missing."),
            storeId = model.storeId,
            targetDate = model.targetDate.toString(),
            shiftType = model.shiftType,
            startRequired = model.startRequired,
            endRequired = model.endRequired
        )
    }
}

@Serializable
data class ShiftBoardUserDto(
    val id: Long,
    val name: String,
    val nickname: String,
    val zooId: Int,
    val isDeleted: Boolean
)

@Serializable
data class ShiftBoardShiftDto(
    val id: Long,
    val userId: Long,
    val workDate: String,
    val shiftType: ShiftSlotType,
    val startTime: String,
    val endTime: String,
    val memo: String?
)

@Serializable
data class ShiftBoardRequirementDto(
    val id: Long?,
    val targetDate: String,
    val shiftType: ShiftSlotType,
    val startRequired: Int,
    val endRequired: Int,
    val startActual: Int,
    val endActual: Int,
    val editable: Boolean
)

@Serializable
data class ShiftBoardResponseDto(
    val storeId: Long,
    val startDate: String,
    val endDate: String,
    val users: List<ShiftBoardUserDto>,
    val shifts: List<ShiftBoardShiftDto>,
    val requirements: List<ShiftBoardRequirementDto>,
    val editable: Boolean
) {
    companion object {
        fun from(result: GetShiftBoardUseCase.Result): ShiftBoardResponseDto =
            ShiftBoardResponseDto(
                storeId = result.storeId,
                startDate = result.startDate.toString(),
                endDate = result.endDate.toString(),
                users = result.users.map { user ->
                    ShiftBoardUserDto(
                        id = user.id,
                        name = user.name,
                        nickname = user.nickname,
                        zooId = user.zooId,
                        isDeleted = user.isDeleted
                    )
                },
                shifts = result.shifts.map { shift ->
                    ShiftBoardShiftDto(
                        id = shift.id,
                        userId = shift.userId,
                        workDate = shift.workDate.toString(),
                        shiftType = shift.shiftType,
                        startTime = ShiftTimeCodec.format(shift.startTime),
                        endTime = ShiftTimeCodec.format(shift.endTime),
                        memo = shift.memo
                    )
                },
                requirements = result.requirements.map { req ->
                    ShiftBoardRequirementDto(
                        id = req.id,
                        targetDate = req.targetDate.toString(),
                        shiftType = req.shiftType,
                        startRequired = req.startRequired,
                        endRequired = req.endRequired,
                        startActual = req.startActual,
                        endActual = req.endActual,
                        editable = req.editable
                    )
                },
                editable = result.editable
            )
    }
}
