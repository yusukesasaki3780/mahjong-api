package com.example.domain.model

/**
 * Aggregated ranking metrics for a user within a specified period.
 */
data class RankingEntry(
    val userId: Long,
    val name: String,
    val totalIncome: Long,
    val gameCount: Int,
    val averagePlace: Double?
)
