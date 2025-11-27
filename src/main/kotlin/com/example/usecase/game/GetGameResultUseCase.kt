package com.example.usecase.game

import com.example.domain.model.GameResult
import com.example.domain.repository.GameResultRepository

/**
 * 単一のゲーム結果を取得するユースケース。
 * ユーザー本人のレコードのみ返し、それ以外は null を返す。
 */
class GetGameResultUseCase(
    private val repository: GameResultRepository
) {
    suspend operator fun invoke(userId: Long, resultId: Long): GameResult? {
        val result = repository.findById(resultId) ?: return null
        return if (result.userId == userId) result else null
    }
}
