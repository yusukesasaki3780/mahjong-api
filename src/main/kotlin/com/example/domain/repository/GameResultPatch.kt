package com.example.domain.repository

import com.example.domain.model.GameType
import kotlinx.datetime.Instant

/**
 * ゲーム結果の部分更新に使う値オブジェクト。
 */
data class GameResultPatch(
    val gameType: GameType? = null,
    val playedAt: Instant? = null,
    val place: Int? = null,
    val baseIncome: Long? = null,
    val tipCount: Int? = null,
    val tipIncome: Long? = null,
    val totalIncome: Long? = null,
    val note: String? = null,
    val updatedAt: Instant? = null
)
