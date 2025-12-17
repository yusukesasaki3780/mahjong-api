package com.example.usecase.game

import com.example.domain.repository.GameResultRepository
import java.util.UUID

/**
 * 簡易入力バッチで作成したゲーム結果を丸ごと削除するユースケース。
 */
class DeleteSimpleBatchResultsUseCase(
    private val repository: GameResultRepository
) {

    /**
     * 一括削除対象となるユーザーと simpleBatchId を受け取るコマンド。
     */
    data class Command(val userId: Long, val simpleBatchId: UUID)

    /**
     * 削除件数のみを返す結果 DTO。
     */
    data class Result(val deletedCount: Int)

    /**
     * ユーザーとバッチ ID を指定して一括削除し、その件数を返す。
     */
    suspend operator fun invoke(command: Command): Result {
        val deleted = repository.deleteBySimpleBatch(command.userId, command.simpleBatchId)
        return Result(deletedCount = deleted)
    }
}
