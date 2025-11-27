package com.example.usecase.game

/**
 * ### このファイルの役割
 * - ゲーム結果を削除し、必要な監査ログを残す処理です。
 * - 削除対象を事前に取得して監査ログへ before 情報を渡す点がポイントです。
 */

import com.example.domain.model.AuditContext
import com.example.domain.repository.GameResultRepository
import com.example.infrastructure.logging.AuditLogger

class DeleteGameResultUseCase(
    private val repository: GameResultRepository,
    private val auditLogger: AuditLogger
) {

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

