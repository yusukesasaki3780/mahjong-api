package com.example.usecase.game

/**
 * ### このファイルの役割
 * - 指定ゲーム種別と期間におけるランキングの中から、ログインユーザー自身の順位情報だけを取り出します。
 * - ユーザー情報とランキング情報を組み合わせ、フロントが必要とするサマリーを返します。
 */

import com.example.domain.model.GameType
import com.example.domain.model.StatsPeriod
import com.example.domain.repository.UserRepository

class GetMyRankingUseCase(
    private val userRepository: UserRepository
) {

    data class Command(
        val userId: Long,
        val gameType: GameType,
        val period: StatsPeriod
    )

    data class Result(
        val rank: Int?,
        val totalPlayers: Int,
        val averageRank: Double?,
        val totalProfit: Long,
        val gameCount: Int,
        val user: UserSummary
    ) {
        data class UserSummary(
            val id: Long,
            val nickname: String
        )
    }

    suspend operator fun invoke(command: Command): Result {
        val user = userRepository.findById(command.userId)
            ?: throw IllegalArgumentException("User ${command.userId} not found.")

        val entries = userRepository.findRanking(command.gameType, command.period)
        val totalPlayers = entries.size
        val index = entries.indexOfFirst { it.userId == command.userId }
        val entry = if (index >= 0) entries[index] else null

        return Result(
            rank = if (index >= 0) index + 1 else null,
            totalPlayers = totalPlayers,
            averageRank = entry?.averagePlace,
            totalProfit = entry?.totalIncome ?: 0,
            gameCount = entry?.gameCount ?: 0,
            user = Result.UserSummary(
                id = user.id ?: command.userId,
                nickname = user.nickname
            )
        )
    }
}
