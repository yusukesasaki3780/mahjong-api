package com.example.domain.repository

import com.example.domain.model.GameResult
import com.example.domain.model.StatsRange

/**
 * ゲーム結果の CRUD と統計情報取得を担当するリポジトリインターフェース。
 */
interface GameResultRepository {

    /**
     * ゲーム結果を新規登録し、登録済みエンティティを返す。
     */
    suspend fun insertGameResult(result: GameResult): GameResult

    /**
     * 既存のゲーム結果を更新し、更新後のエンティティを返す。
     */
    suspend fun updateGameResult(result: GameResult): GameResult

    suspend fun patchGameResult(userId: Long, resultId: Long, patch: GameResultPatch): GameResult

    /**
     * 結果 ID を指定して削除し、削除件数が 1 以上なら true を返す。
     */
    suspend fun deleteGameResult(resultId: Long): Boolean
    suspend fun findById(resultId: Long): GameResult?

    /**
     * ユーザと区間条件でゲーム結果一覧を取得する。
     */
    suspend fun getUserResults(userId: Long, range: StatsRange): List<GameResult>

    /**
     * 指定範囲内の平均着順を算出して返す。
     */
    suspend fun getAveragePlace(userId: Long, range: StatsRange): Double?

    /**
     * 指定範囲内で得た総収入を返す。
     */
    suspend fun getTotalIncome(userId: Long, range: StatsRange): Long
}
