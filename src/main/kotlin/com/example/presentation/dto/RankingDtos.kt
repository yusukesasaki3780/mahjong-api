package com.example.presentation.dto

import com.example.domain.model.RankingEntry
import com.example.usecase.game.GetMyRankingUseCase
import kotlinx.serialization.Serializable

@Serializable
data class RankingEntryResponse(
    val userId: Long,
    val zooId: Int,
    val name: String,
    val gameCount: Int,
    val averagePlace: Double?
) {
    companion object {
        fun from(entry: RankingEntry) = RankingEntryResponse(
            userId = entry.userId,
            zooId = entry.zooId,
            name = entry.name,
            gameCount = entry.gameCount,
            averagePlace = entry.averagePlace
        )
    }
}

@Serializable
data class MyRankingResponse(
    val rank: Int?,
    val totalPlayers: Int,
    val averageRank: Double?,
    val gameCount: Int,
    val user: MyRankingUserResponse
) {
    companion object {
        fun from(result: GetMyRankingUseCase.Result) = MyRankingResponse(
            rank = result.rank,
            totalPlayers = result.totalPlayers,
            averageRank = result.averageRank,
            gameCount = result.gameCount,
            user = MyRankingUserResponse(
                id = result.user.id,
                nickname = result.user.nickname
            )
        )
    }
}

@Serializable
data class MyRankingUserResponse(
    val id: Long,
    val nickname: String
)

@Serializable
data class MyRankingStatsResponse(
    val games: Int,
    val averageRank: Double?
)

@Serializable
data class RankingListResponse(
    val myRank: Int?,
    val totalPlayers: Int,
    val myStats: MyRankingStatsResponse?,
    val ranking: List<RankingEntryResponse>
)
