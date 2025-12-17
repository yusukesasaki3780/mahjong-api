package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class ShiftSlotType {
    EARLY,
    LATE
}

@Serializable
data class ShiftRequirement(
    val id: Long? = null,
    val storeId: Long,
    val targetDate: LocalDate,
    val shiftType: ShiftSlotType,
    val startRequired: Int,
    val endRequired: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
