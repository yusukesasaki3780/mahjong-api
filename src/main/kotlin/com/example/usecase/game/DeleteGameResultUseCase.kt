package com.example.usecase.game

/**
 * ### このファイルの役割
 * - ゲーム結果を削除し、必要な監査ログを残す処理です。
 * - 削除対象を事前に取得して監査ログへ before 情報を渡す点がポイントです。
 */

import com.example.domain.model.AuditContext
import com.example.domain.repository.GameResultRepository
import com.example.infrastructure.logging.AuditLogger

/**
 * ゲーム結果の削除と監査ログ出力を担うユースケース。
 */
class DeleteGameResultUseCase(
    private val repository: GameResultRepository,
    private val auditLogger: AuditLogger
) {

    /**
     * 削除対象を取得してから削除処理を行い、成功時のみ監査ログへ記録する。
     */
    suspend operator fun invoke(resultId: Long, auditContext: AuditContext): Boolean {
        val before = repository.findById(resultId) ?: return false
        val deleted = repository.deleteGameResult(resultId)
        if (deleted) {
            auditLogger.log(
                entityType = "GAME_RESULT",
                entityId = resultId,
                action = "DELETE",
                context = auditContext,
                before = before,
                after = null
            )
        }
        return deleted
    }
}

