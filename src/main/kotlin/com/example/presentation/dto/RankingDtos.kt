package com.example.presentation.dto

import com.example.domain.model.RankingEntry
import kotlinx.serialization.Serializable

@Serializable
data class RankingEntryResponse(
    val userId: Long,
    val name: String,
    val totalIncome: Long,
    val gameCount: Int,
    val averagePlace: Double?
) {
    companion object {
        fun from(entry: RankingEntry) = RankingEntryResponse(
            userId = entry.userId,
            name = entry.name,
            totalIncome = entry.totalIncome,
            gameCount = entry.gameCount,
            averagePlace = entry.averagePlace
        )
    }
}
