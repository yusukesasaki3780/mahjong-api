package com.example.usecase.shift

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * シフトの時間帯が重なっているかを日付またぎも考慮して判定するユーティリティ。
 * ローカル時刻を日内ナノ秒に正規化し、日付を跨ぐ場合は 24 時間分を加算して比較する。
 */
object ShiftOverlapChecker {

    private val timeZone = TimeZone.currentSystemDefault()
    private const val NANOS_PER_SECOND = 1_000_000_000L
    private const val SECONDS_PER_DAY = 24 * 60 * 60
    private const val NANOS_PER_DAY = SECONDS_PER_DAY * NANOS_PER_SECOND

    /**
     * 2 つのシフト時間が交差しているかを判定する。
     */
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
