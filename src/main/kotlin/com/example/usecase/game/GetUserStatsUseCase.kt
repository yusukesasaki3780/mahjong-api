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

    /**
     * 成績集計の対象ユーザーと期間を指定するコマンド。
     */
    data class Command(
        val userId: Long,
        val range: StatsRange
    )

    /**
     * 指定期間の統計値と詳細結果を含むレスポンス。
     */
    data class Result(
        val userId: Long,
        val range: StatsRange,
        val averagePlace: Double?,
        val totalGames: Int,
        val totalIncome: Long,
        val results: List<GameResult>
    )

    /**
     * 成績レンジを指定して統計値と明細を同時に取得する。
     */
    suspend operator fun invoke(command: Command): Result {
        val results = repository.getUserResults(command.userId, command.range)
        val average = repository.getAveragePlace(command.userId, command.range)
        val income = repository.getTotalIncome(command.userId, command.range)
        val playableCount = results.count { !it.isFinalIncomeRecord }

        return Result(
            userId = command.userId,
            range = command.range,
            averagePlace = average,
            totalGames = playableCount,
            totalIncome = income,
            results = results
        )
    }
}

