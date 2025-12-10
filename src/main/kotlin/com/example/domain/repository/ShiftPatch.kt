package com.example.domain.repository

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * シフト本体の部分更新。
 */
data class ShiftPatch(
    val workDate: LocalDate? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val memo: String? = null,
    val specialHourlyWageId: Long? = null,
    val specialHourlyWageIdSet: Boolean = false,
    val updatedAt: Instant? = null,
    val breakPatches: List<ShiftBreakPatch>? = null
)

/**
 * 休憩単位の部分更新。
 */
data class ShiftBreakPatch(
    val id: Long?,
    val breakStart: Instant? = null,
    val breakEnd: Instant? = null,
    val delete: Boolean = false
)
