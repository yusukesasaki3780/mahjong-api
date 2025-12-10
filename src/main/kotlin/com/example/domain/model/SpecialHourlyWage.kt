package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 特別時給プリセットを表すドメインモデル。
 */
@Serializable
data class SpecialHourlyWage(
    val id: Long,
    val userId: Long,
    val label: String,
    val hourlyWage: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
