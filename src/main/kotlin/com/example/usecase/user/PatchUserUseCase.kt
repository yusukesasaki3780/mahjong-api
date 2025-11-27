package com.example.usecase.user

/**
 * ### このファイルの役割
 * - ユーザー情報の部分更新（PATCH）を扱い、nullable プロパティで差分更新を実現します。
 * - 監査ログ用に更新前後の値を保持し、必要最小限の書き換えを行います。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.User
import com.example.domain.repository.UserPatch
import com.example.domain.repository.UserRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.datetime.Clock

class PatchUserUseCase(
    private val userRepository: UserRepository,
    private val auditLogger: AuditLogger
) {

    data class Command(
        val userId: Long,
        val name: String? = null,
        val nickname: String? = null,
        val storeName: String? = null,
        val prefectureCode: String? = null,
        val email: String? = null
    )

    suspend operator fun invoke(command: Command, auditContext: AuditContext): User {
        command.validateFields()

        val existing = userRepository.findById(command.userId)
            ?: throw IllegalArgumentException("User not found: ${command.userId}")

        if (command.email != null && command.email != existing.email) {
            val duplicated = userRepository.findByEmail(command.email)
            if (duplicated != null && duplicated.id != command.userId) {
                throw DomainValidationException(
                    violations = listOf(
                        FieldError(
                            field = "email",
                            code = "EMAIL_ALREADY_EXISTS",
                            message = "同じメールアドレスが既に登録されています"
                        )
                    ),
                    message = "Email already registered"
                )
            }
        }

        val merged = existing.copy(
            name = command.name ?: existing.name,
            nickname = command.nickname ?: existing.nickname,
            storeName = command.storeName ?: existing.storeName,
            prefectureCode = command.prefectureCode ?: existing.prefectureCode,
            email = command.email ?: existing.email,
            updatedAt = Clock.System.now()
        )

        val patch = UserPatch(
            name = command.name,
            nickname = command.nickname,
            storeName = command.storeName,
            prefectureCode = command.prefectureCode,
            email = command.email,
            updatedAt = merged.updatedAt
        )

        val result = userRepository.patchUser(command.userId, patch)
        auditLogger.log(
            entityType = "USER",
            entityId = result.id ?: command.userId,
            action = "PATCH",
            context = auditContext,
            before = existing,
            after = result
        )
        return result
    }
}

