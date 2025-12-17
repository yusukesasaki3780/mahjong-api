package com.example.usecase.user

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.repository.UserCredentialRepository
import com.example.domain.repository.UserRepository

/**
 * 管理者が一般ユーザーのパスワードを再発行するユースケース。
 */
class AdminResetUserPasswordUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository
) {

    suspend operator fun invoke(
        adminStoreId: Long,
        targetUserId: Long,
        newPassword: String
    ): Boolean {
        val target = userRepository.findById(targetUserId) ?: return false
        if (target.isAdmin) {
            throw validationError("userId", "ADMIN_PASSWORD_RESET_FORBIDDEN", "管理者アカウントのパスワードは再発行できません。")
        }
        if (target.storeId != adminStoreId) {
            throw validationError("userId", "DIFFERENT_STORE", "他店舗のメンバーは再発行対象外です。")
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
