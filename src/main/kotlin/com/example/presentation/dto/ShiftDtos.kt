@file:OptIn(ExperimentalSerializationApi::class)

package com.example.presentation.dto

import com.example.domain.model.Shift
import com.example.presentation.util.ShiftTimeCodec
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlinx.serialization.ExperimentalSerializationApi
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
    val breakMinutes: Int? = null
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
    val breaks: List<PatchShiftBreakRequest>? = null
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
    val breaks: List<ShiftBreakResponse>
) {
    companion object {
        fun from(shift: Shift): ShiftResponse {
            val aligned = ShiftTimeCodec.alignShiftWindow(shift.workDate, shift.startTime, shift.endTime)
            val startLocal = LocalDateTime(shift.workDate, ShiftTimeCodec.toLocalTime(aligned.startInstant))
            val endWorkDate = shift.workDate.plus(aligned.endDayOffset, DateTimeUnit.DAY)
            val endLocal = LocalDateTime(endWorkDate, ShiftTimeCodec.toLocalTime(aligned.endInstant))
            println("startLocal=$startLocal")
            println("endLocal=$endLocal")
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
data class ShiftStatsResponse(
    val totalHours: Double,
    val nightHours: Double,
    val avgHours: Double,
    val count: Int
)
