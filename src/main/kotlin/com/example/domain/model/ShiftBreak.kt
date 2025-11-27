package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * シフト中の休憩時間帯を表すモデル。
 */
@Serializable
data class ShiftBreak(
    val id: Long? = null,
    val shiftId: Long? = null,
    val breakStart: Instant,
    val breakEnd: Instant
)
