package com.example.presentation.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime as JavaLocalDateTime

/**
 * シフト/休憩時間（HH:mm 形式）と Instant の相互変換ヘルパー。
 */
object ShiftTimeCodec {
    private val zone: TimeZone = TimeZone.currentSystemDefault()
    private val zoneId: ZoneId = ZoneId.of(zone.id)
    private val isoOffsetFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun toInstant(workDate: LocalDate, hhmm: String): Instant =
        toInstant(workDate, parseLocalTime(hhmm))

    fun toInstant(workDate: LocalDate, localTime: LocalTime): Instant =
        LocalDateTime(workDate, localTime).toInstant(zone)

    fun format(instant: Instant): String {
        val localTime = instant.toLocalDateTime(zone).time
        return "%02d:%02d".format(localTime.hour, localTime.minute)
    }

    fun formatDateTime(instant: Instant): String =
        isoOffsetFormatter.format(instant.toJavaInstant().atZone(zoneId))

    fun dayOffset(start: Instant, end: Instant): Int {
        val startDate = start.toLocalDateTime(zone).date
        val endDate = end.toLocalDateTime(zone).date
        return startDate.daysUntil(endDate)
    }

    fun toLocalDateTime(instant: Instant): LocalDateTime =
        instant.toLocalDateTime(zone)

    fun toLocalTime(instant: Instant): LocalTime =
        instant.toLocalDateTime(zone).time

    fun parseLocalTime(value: String): LocalTime =
        runCatching { LocalTime.parse(value) }
            .getOrElse {
                runCatching { LocalDateTime.parse(value).time }
                    .getOrElse {
                        runCatching { OffsetDateTime.parse(value).toLocalTime().toKotlinLocalTime() }
                            .getOrElse { throw IllegalArgumentException("Invalid time format: $value") }
                    }
            }

    data class AlignedShiftWindow(
        val startInstant: Instant,
        val endInstant: Instant,
        val endDayOffset: Int
    )

    fun alignShiftWindow(workDate: LocalDate, rawStart: Instant, rawEnd: Instant): AlignedShiftWindow {
        val startLocal = toLocalTime(rawStart)
        val endLocal = toLocalTime(rawEnd)
        val crossesMidnight = endLocal <= startLocal
        val endDate = if (crossesMidnight) workDate.plus(DatePeriod(days = 1)) else workDate
        val alignedStart = toInstant(workDate, startLocal)
        val alignedEnd = toInstant(endDate, endLocal)
        val offset = if (crossesMidnight) 1 else 0
        return AlignedShiftWindow(
            startInstant = alignedStart,
            endInstant = alignedEnd,
            endDayOffset = offset
        )
    }
}
