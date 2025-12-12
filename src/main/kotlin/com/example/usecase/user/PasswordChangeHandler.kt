package com.example.usecase.user

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.repository.UserCredentialRepository

private const val CURRENT_PASSWORD_REQUIRED_MESSAGE = "パスワードを変更するには現在のパスワードが必要です。"
private const val NEW_PASSWORD_REQUIRED_MESSAGE = "新しいパスワードを入力してください。"
private const val CURRENT_PASSWORD_INVALID_MESSAGE = "現在のパスワードが正しくありません。"

private val passwordHasher = BCrypt.withDefaults()

internal suspend fun changePasswordIfRequested(
    userId: Long,
    credentialRepository: UserCredentialRepository,
    currentPassword: String?,
    newPassword: String?
) {
    if (currentPassword.isNullOrBlank() && newPassword.isNullOrBlank()) {
        return
    }

    if (currentPassword.isNullOrBlank()) {
        throw DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "currentPassword",
                    code = "CURRENT_PASSWORD_REQUIRED",
                    message = CURRENT_PASSWORD_REQUIRED_MESSAGE
                )
            ),
            message = CURRENT_PASSWORD_REQUIRED_MESSAGE
        )
    }

    if (newPassword.isNullOrBlank()) {
        throw DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "newPassword",
                    code = "NEW_PASSWORD_REQUIRED",
                    message = NEW_PASSWORD_REQUIRED_MESSAGE
                )
            ),
            message = NEW_PASSWORD_REQUIRED_MESSAGE
        )
    }

    val verified = credentialRepository.verifyPassword(userId, currentPassword)
    if (!verified) {
        throw DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "currentPassword",
                    code = "CURRENT_PASSWORD_INVALID",
                    message = CURRENT_PASSWORD_INVALID_MESSAGE
                )
            ),
            message = CURRENT_PASSWORD_INVALID_MESSAGE
        )
    }

    PasswordPolicy.validate(newPassword)
    val hashed = passwordHasher.hashToString(12, newPassword.toCharArray())
    credentialRepository.updatePassword(userId, hashed)
}

