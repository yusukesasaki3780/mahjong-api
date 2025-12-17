package com.example.usecase.shift

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utility to evaluate shift time overlaps in a day-aware manner.
 * It normalizes each shift's local start/end times into a monotonic timeline
 * (adding 24 hours to the end when crossing midnight) and checks for intersections.
 */
object ShiftOverlapChecker {

    private val timeZone = TimeZone.currentSystemDefault()
    private const val NANOS_PER_SECOND = 1_000_000_000L
    private const val SECONDS_PER_DAY = 24 * 60 * 60
    private const val NANOS_PER_DAY = SECONDS_PER_DAY * NANOS_PER_SECOND

    fun overlaps(
        aStart: Instant,
        aEnd: Instant,
        bStart: Instant,
        bEnd: Instant
    ): Boolean {
        val first = normalize(aStart, aEnd)
        val second = normalize(bStart, bEnd)
        return first.first < second.second && second.first < first.second
    }

    private fun normalize(start: Instant, end: Instant): Pair<Long, Long> {
        val startNanos = secondOfDay(start)
        val endNanos = secondOfDay(end)
        return if (endNanos > startNanos) {
            startNanos to endNanos
        } else {
            startNanos to (endNanos + NANOS_PER_DAY)
        }
    }

    private fun secondOfDay(instant: Instant): Long {
        val time = instant.toLocalDateTime(timeZone).time
        val seconds = (time.hour * 60L + time.minute) * 60L + time.second
        return seconds * NANOS_PER_SECOND + time.nanosecond
    }
}
