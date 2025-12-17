package com.example.usecase.game

/**
 * ### このファイルの役割
 * - 指定されたゲームタイプと期間に応じてランキング情報を取得するユースケースです。
 * - 集計処理自体は Repository 側へ委譲し、ここでは入力の正規化と結果の整形を担当します。
 */

import com.example.domain.model.GameType
import com.example.domain.model.RankingEntry
import com.example.domain.model.StatsPeriod
import com.example.domain.repository.UserRepository

/**
 * ランキング一覧取得ユースケース。
 */
class GetRankingUseCase(
    private val userRepository: UserRepository
) {

    /**
     * ランキング取得条件（ゲーム種別・期間）を表すコマンド。
     */
    data class Command(
        val gameType: GameType,
        val period: StatsPeriod
    )

    /**
     * 指定ゲーム種別と期間でランキングを検索する。
     */
    suspend operator fun invoke(command: Command): List<RankingEntry> =
        userRepository.findRanking(command.gameType, command.period)
}
