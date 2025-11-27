package com.example.domain.model

import com.example.serialization.YearMonthSerializer
import kotlinx.datetime.Instant
import java.time.YearMonth
import kotlinx.serialization.Serializable

@Serializable
data class AdvancePayment(
    val userId: Long,
    @Serializable(with = YearMonthSerializer::class)
    val yearMonth: YearMonth,
    val amount: Double,
    val createdAt: Instant,
    val updatedAt: Instant
)
