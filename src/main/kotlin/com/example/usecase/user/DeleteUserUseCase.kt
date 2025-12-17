package com.example.usecase.user

/**
 * ### このファイルの役割
 * - ユーザー削除と監査ログ記録を担当するユースケースです。
 * - 先に対象を取得して存在確認し、削除成功時のみ監査ログを出力します。
 */

import com.example.domain.model.AuditContext
import com.example.domain.repository.UserRepository
import com.example.infrastructure.logging.AuditLogger

/**
 * ユーザ削除ユースケース。
 */
class DeleteUserUseCase(
    private val userRepository: UserRepository,
    private val auditLogger: AuditLogger
) {

    suspend operator fun invoke(userId: Long, auditContext: AuditContext): Boolean {
        val before = userRepository.findById(userId) ?: return false
        if (before.isDeleted) {
            return false
        }
        val deleted = userRepository.deleteUser(userId)
        if (deleted) {
            auditLogger.log(
                entityType = "USER",
                entityId = userId,
                action = "DELETE",
                context = auditContext,
                before = before,
                after = before.copy(isDeleted = true)
            )
        }
        return deleted
    }
}
