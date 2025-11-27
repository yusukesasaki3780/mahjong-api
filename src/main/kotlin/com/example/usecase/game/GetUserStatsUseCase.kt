package com.example.usecase.game

/**
 * ### このファイルの役割
 * - ユーザー単位でゲーム数・平均着順・収支を取りまとめ、詳細結果と合わせて返すユースケースです。
 * - 期間情報（StatsRange）を受け取って Repository を呼び出すだけの薄い層に保っています。
 */

import com.example.domain.model.GameResult
import com.example.domain.model.StatsRange
import com.example.domain.repository.GameResultRepository

/**
 * ユーザの成績統計値を返すユースケース。
 */
class GetUserStatsUseCase(
    private val repository: GameResultRepository
) {

    data class Command(
        val userId: Long,
        val range: StatsRange
    )

    data class Result(
        val userId: Long,
        val range: StatsRange,
        val averagePlace: Double?,
        val totalGames: Int,
        val totalIncome: Long,
        val results: List<GameResult>
    )

    suspend operator fun invoke(command: Command): Result {
        val results = repository.getUserResults(command.userId, command.range)
        val average = repository.getAveragePlace(command.userId, command.range)
        val income = repository.getTotalIncome(command.userId, command.range)

        return Result(
            userId = command.userId,
            range = command.range,
            averagePlace = average,
            totalGames = results.size,
            totalIncome = income,
            results = results
        )
    }
}

