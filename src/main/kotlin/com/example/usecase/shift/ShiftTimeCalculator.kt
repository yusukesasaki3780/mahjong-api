package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - シフトから日中・深夜ごとの勤務分数を計算するための共通ヘルパーです。
 * - 給与計算やダッシュボード集計から使えるように、休憩の控除や時刻の分割処理を提供します。
 */

import com.example.domain.model.Shift
import com.example.presentation.util.ShiftTimeCodec
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * シフト勤務時間（昼夜）を算出するヘルパー。
 */
object ShiftTimeCalculator {

    data class Minutes(
        val dayMinutes: Long,
        val nightMinutes: Long
    ) {
        val totalMinutes: Long get() = dayMinutes + nightMinutes
    }

    fun calculateMinutes(shifts: List<Shift>, timeZone: TimeZone): Minutes {
        var daySeconds = 0L
        var nightSeconds = 0L

        shifts.forEach { shift ->
            val normalized = normalizeShift(shift, timeZone)
            subtractBreaks(normalized).forEach { (start, end) ->
                val (dayPart, nightPart) = splitByTimeOfDay(start, end, timeZone)
                daySeconds += dayPart
                nightSeconds += nightPart
            }
        }

        return Minutes(
            dayMinutes = daySeconds / 60,
            nightMinutes = nightSeconds / 60
        )
    }

    private fun subtractBreaks(shift: NormalizedShift): List<Pair<Instant, Instant>> {
        var workingSegments = mutableListOf(shift.start to shift.end)
        val breaks = shift.breaks.sortedBy { it.first }

        breaks.forEach { br ->
            val (breakStart, breakEnd) = br
            val next = mutableListOf<Pair<Instant, Instant>>()
            workingSegments.forEach { (segStart, segEnd) ->
                if (breakEnd <= segStart || breakStart >= segEnd) {
                    next += segStart to segEnd
                } else {
                    if (breakStart > segStart) next += segStart to minOf(segEnd, breakStart)
                    if (breakEnd < segEnd) next += maxOf(breakEnd, segStart) to segEnd
                }
            }
            workingSegments = next
        }

        return workingSegments.filter { it.first < it.second }
    }

    private fun splitByTimeOfDay(
        start: Instant,
        end: Instant,
        timeZone: TimeZone
    ): Pair<Long, Long> {
        var cursor = start
        var daySeconds = 0L
        var nightSeconds = 0L

        while (cursor < end) {
            val isDay = cursor.isDay(timeZone)
            val boundary = nextBoundaryInstant(cursor, timeZone, isDay)
            val sliceEnd = minOf(boundary, end)
            val seconds = (sliceEnd - cursor).inWholeSeconds
            if (seconds > 0) {
                if (isDay) daySeconds += seconds else nightSeconds += seconds
            }
            cursor = sliceEnd
        }

        return daySeconds to nightSeconds
    }

    private fun Instant.isDay(timeZone: TimeZone): Boolean {
        val localTime = this.toLocalDateTime(timeZone).time
        val dayStart = LocalTime(5, 0)
        val dayEnd = LocalTime(22, 0)
        return localTime >= dayStart && localTime < dayEnd
    }

    private fun nextBoundaryInstant(cursor: Instant, timeZone: TimeZone, currentlyDay: Boolean): Instant {
        val local = cursor.toLocalDateTime(timeZone)
        val date = local.date
        val dayStart = LocalTime(5, 0)
        val dayEnd = LocalTime(22, 0)

        val boundaryLocal: LocalDateTime = if (currentlyDay) {
            LocalDateTime(date, dayEnd)
        } else {
            if (local.time >= dayEnd) {
                LocalDateTime(date.plusDays(1), dayStart)
            } else {
                LocalDateTime(date, dayStart)
            }
        }

        return boundaryLocal.toInstant(timeZone)
    }

    private data class NormalizedShift(
        val start: Instant,
        val end: Instant,
        val breaks: List<Pair<Instant, Instant>>
    )

    private fun normalizeShift(shift: Shift, timeZone: TimeZone): NormalizedShift {
        val aligned = ShiftTimeCodec.alignShiftWindow(shift.workDate, shift.startTime, shift.endTime)
        val startLocal = ShiftTimeCodec.toLocalTime(aligned.startInstant)
        val crossesMidnight = aligned.endDayOffset > 0

        val normalizedBreaks = shift.breaks.map { br ->
            val breakStartLocal = ShiftTimeCodec.toLocalTime(br.breakStart)
            var breakStartDate = shift.workDate
            if (crossesMidnight && breakStartLocal < startLocal) {
                breakStartDate = breakStartDate.plusDays(1)
            }
            val breakEndLocal = ShiftTimeCodec.toLocalTime(br.breakEnd)
            var breakEndDate = breakStartDate
            if (breakEndLocal < breakStartLocal) {
                breakEndDate = breakEndDate.plusDays(1)
            }
            val breakStartInstant = ShiftTimeCodec.toInstant(breakStartDate, breakStartLocal)
            var breakEndInstant = ShiftTimeCodec.toInstant(breakEndDate, breakEndLocal)
            if (breakEndInstant <= breakStartInstant) {
                breakEndInstant = ShiftTimeCodec.toInstant(breakEndDate.plusDays(1), breakEndLocal)
            }
            breakStartInstant to breakEndInstant
        }

        return NormalizedShift(
            start = aligned.startInstant,
            end = aligned.endInstant,
            breaks = normalizedBreaks
        )
    }

    private fun LocalDate.plusDays(days: Int): LocalDate =
        this.plus(DatePeriod(days = days))
}
