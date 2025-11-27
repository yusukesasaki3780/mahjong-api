package com.example.presentation.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShiftTimeCodecTest {

    @Test
    fun `parseLocalTime accepts HHmm`() {
        val time = ShiftTimeCodec.parseLocalTime("09:30")
        assertEquals(LocalTime(hour = 9, minute = 30), time)
    }

    @Test
    fun `parseLocalTime accepts ISO local datetime`() {
        val time = ShiftTimeCodec.parseLocalTime("2025-11-26T21:15:00")
        assertEquals(LocalTime(hour = 21, minute = 15), time)
    }

    @Test
    fun `parseLocalTime accepts ISO instant with offset`() {
        val time = ShiftTimeCodec.parseLocalTime("2025-11-26T13:00:00Z")
        assertEquals(LocalTime(hour = 13, minute = 0), time)
    }

    @Test
    fun `toInstant preserves work date`() {
        val workDate = LocalDate.parse("2025-11-25")
        val instant = ShiftTimeCodec.toInstant(workDate, "22:00")
        val zoned = ShiftTimeCodec.formatDateTime(instant)
        assertTrue(zoned.startsWith("2025-11-25"))
    }

    @Test
    fun `alignShiftWindow rebase times onto work date`() {
        val workDate = LocalDate(2025, 11, 26)
        val incorrectStart = ShiftTimeCodec.toInstant(LocalDate(1970, 1, 1), "23:00")
        val incorrectEnd = ShiftTimeCodec.toInstant(LocalDate(1970, 1, 2), "10:00")

        val aligned = ShiftTimeCodec.alignShiftWindow(workDate, incorrectStart, incorrectEnd)

        assertTrue(ShiftTimeCodec.formatDateTime(aligned.startInstant).startsWith("2025-11-26"))
        assertTrue(ShiftTimeCodec.formatDateTime(aligned.endInstant).startsWith("2025-11-27"))
        assertEquals(1, aligned.endDayOffset)
    }

    @Test
    fun `formatDateTime returns ISO offset`() {
        val workDate = LocalDate.parse("2025-11-25")
        val instant = ShiftTimeCodec.toInstant(workDate, "22:00")
        val formatted = ShiftTimeCodec.formatDateTime(instant)
        assertTrue(formatted.endsWith("+09:00") || formatted.endsWith("Z"))
    }
}
