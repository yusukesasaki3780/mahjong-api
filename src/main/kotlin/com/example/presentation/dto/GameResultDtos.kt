package com.example.presentation.dto

import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.usecase.game.GetUserStatsUseCase
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * 成績関連の DTO。
 */
@Serializable
data class UpsertGameResultRequest(
    val gameType: GameType,
    val playedAt: LocalDate,
    val place: Int,
    val baseIncome: Long,
    val tipCount: Int,
    val tipIncome: Long,
    val totalIncome: Long,
    val note: String? = null,
    val createdAt: Instant? = null
)

/**
 * 部分更新リクエスト（すべて任意項目）。
 */
@Serializable
data class PatchGameResultRequest(
    val gameType: GameType? = null,
    val playedAt: LocalDate? = null,
    val place: Int? = null,
    val baseIncome: Long? = null,
    val tipCount: Int? = null,
    val tipIncome: Long? = null,
    val totalIncome: Long? = null,
    val note: String? = null
)

@Serializable
data class GameResultResponse(
    val id: Long,
    val gameType: GameType,
    val playedAt: String,
    val place: Int,
    val baseIncome: Long,
    val tipCount: Int,
    val tipIncome: Long,
    val totalIncome: Long,
    val note: String?
) {
    companion object {
        fun from(result: GameResult): GameResultResponse {
            val localDate = result.playedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            return GameResultResponse(
                id = result.id!!,
                gameType = result.gameType,
                playedAt = localDate.toString(),
                place = result.place,
                baseIncome = result.baseIncome,
                tipCount = result.tipCount,
                tipIncome = result.tipIncome,
            totalIncome = result.totalIncome,
            note = result.note
        )
    }
}
}

@Serializable
data class UserStatsResponse(
    val userId: Long,
    val averagePlace: Double?,
    val totalGames: Int,
    val totalIncome: Long,
    val results: List<GameResultResponse>
) {
    companion object {
        fun from(result: GetUserStatsUseCase.Result) = UserStatsResponse(
            userId = result.userId,
            averagePlace = result.averagePlace,
            totalGames = result.totalGames,
            totalIncome = result.totalIncome,
            results = result.results.map(GameResultResponse::from)
        )
    }
}
