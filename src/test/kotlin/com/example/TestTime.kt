package com.example

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

object TestTime {
    private val zone = TimeZone.currentSystemDefault()

    fun instant(date: LocalDate, time: String): Instant =
        LocalDateTime(date, LocalTime.parse(time)).toInstant(zone)

    fun instant(dateIso: String, time: String): Instant =
        instant(LocalDate.parse(dateIso), time)
}

fun LocalDate.at(time: String): Instant = TestTime.instant(this, time)
