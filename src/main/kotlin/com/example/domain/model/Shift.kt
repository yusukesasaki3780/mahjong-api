package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * 勤務シフトと紐づく休憩一覧をまとめたモデル。
 */
@Serializable
data class Shift(
    val id: Long? = null,
    val userId: Long,
    val workDate: LocalDate,
    val startTime: Instant,
    val endTime: Instant,
    val memo: String? = null,
    val specialHourlyWage: SpecialHourlyWage? = null,
    val specialHourlyWageId: Long? = specialHourlyWage?.id,
    val breaks: List<ShiftBreak> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)
