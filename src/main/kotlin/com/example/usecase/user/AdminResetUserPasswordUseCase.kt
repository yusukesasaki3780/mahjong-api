package com.example.usecase.user

/**
 * ### このファイルの役割
 * - 管理者が一般ユーザーのパスワードを再発行するユースケースです。
 * - 管理者アカウントに対してはパスワード変更を許可しません。
 */

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.repository.UserCredentialRepository
import com.example.domain.repository.UserRepository

class AdminResetUserPasswordUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository
) {

    suspend operator fun invoke(targetUserId: Long, newPassword: String): Boolean {
        val target = userRepository.findById(targetUserId) ?: return false
        if (target.isAdmin) {
            throw validationError("userId", "ADMIN_PASSWORD_RESET_FORBIDDEN", "管理者アカウントのパスワードは再発行できません。")
        }

        PasswordPolicy.validate(newPassword)
        val hash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
        credentialRepository.updatePassword(targetUserId, hash)
        return true
    }

    private fun validationError(field: String, code: String, message: String): DomainValidationException =
        DomainValidationException(
            violations = listOf(FieldError(field = field, code = code, message = message)),
            message = message
        )
}

