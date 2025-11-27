package com.example.usecase.shift

import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ShiftTimeCalculatorTest {

    private val zone = TimeZone.currentSystemDefault()

    @Test
    fun `calculates day and night minutes for overnight shift`() {
        val shift = shift(
            start = instant("2025-11-26T13:00:00Z"), // 22:00+09
            end = instant("2025-11-27T01:00:00Z")    // 10:00+09
        )

        val minutes = ShiftTimeCalculator.calculateMinutes(listOf(shift), zone)

        assertEquals(300, minutes.dayMinutes)   // 5 hours
        assertEquals(420, minutes.nightMinutes) // 7 hours
    }

    @Test
    fun `respects breaks when subtracting`() {
        val shift = shift(
            start = instant("2025-11-26T04:00:00Z"), // 13:00+09
            end = instant("2025-11-26T08:00:00Z"),   // 17:00+09
            breaks = listOf(
                breakRange(
                    start = instant("2025-11-26T06:00:00Z"), // 15:00+09
                    end = instant("2025-11-26T06:30:00Z")    // 15:30+09
                )
            )
        )

        val minutes = ShiftTimeCalculator.calculateMinutes(listOf(shift), zone)

        assertEquals(210, minutes.dayMinutes) // 3.5 hours
        assertEquals(0, minutes.nightMinutes)
    }

    @Test
    fun `captures early morning night minutes`() {
        val shift = shift(
            start = instant("2025-11-25T18:00:00Z"), // 03:00+09
            end = instant("2025-11-25T21:00:00Z")    // 06:00+09
        )

        val minutes = ShiftTimeCalculator.calculateMinutes(listOf(shift), zone)

        assertEquals(60, minutes.dayMinutes)   // 05:00-06:00
        assertEquals(120, minutes.nightMinutes) // 03:00-05:00
    }

    private fun shift(
        start: Instant,
        end: Instant,
        breaks: List<ShiftBreak> = emptyList()
    ): Shift =
        Shift(
            id = 1,
            userId = 1,
            workDate = start.toLocalDateTime(zone).date,
            startTime = start,
            endTime = end,
            memo = null,
            breaks = breaks,
            createdAt = start,
            updatedAt = start
        )

    private fun breakRange(start: Instant, end: Instant) =
        ShiftBreak(
            id = null,
            shiftId = 1,
            breakStart = start,
            breakEnd = end
        )

    private fun instant(iso: String): Instant = Instant.parse(iso)
}
