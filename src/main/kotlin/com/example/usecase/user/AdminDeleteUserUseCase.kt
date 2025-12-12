package com.example.usecase.user

/**
 * ### このファイルの役割
 * - 管理者が一般ユーザーアカウントを削除するドメインルールをまとめたユースケースです。
 * - 管理者自身や他の管理者を削除できないようにバリデーションを行います。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.repository.UserRepository

class AdminDeleteUserUseCase(
    private val userRepository: UserRepository,
    private val deleteUserUseCase: DeleteUserUseCase
) {

    suspend operator fun invoke(
        adminId: Long,
        targetUserId: Long,
        auditContext: AuditContext
    ): Boolean {
        if (adminId == targetUserId) {
            throw validationError("userId", "SELF_DELETE_FORBIDDEN", "自分自身を削除することはできません。")
        }

        val target = userRepository.findById(targetUserId) ?: return false
        if (target.isAdmin) {
            throw validationError("userId", "ADMIN_DELETE_FORBIDDEN", "管理者アカウントは削除できません。")
        }

        return deleteUserUseCase(targetUserId, auditContext)
    }

    private fun validationError(field: String, code: String, message: String): DomainValidationException =
        DomainValidationException(
            violations = listOf(FieldError(field = field, code = code, message = message)),
            message = message
        )
}

