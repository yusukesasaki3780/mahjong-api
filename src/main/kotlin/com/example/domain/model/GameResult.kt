package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 1 半荘分の成績情報を保持する。
 */
@Serializable
data class GameResult(
    val id: Long? = null,
    val userId: Long,
    val gameType: GameType,
    val playedAt: Instant,
    val place: Int,
    val baseIncome: Long,
    val tipCount: Int,
    val tipIncome: Long,
    val otherIncome: Long,
    val totalIncome: Long,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
