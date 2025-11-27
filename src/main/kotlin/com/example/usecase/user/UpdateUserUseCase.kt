package com.example.usecase.user

/**
 * ### このファイルの役割
 * - ユーザー情報を全項目上書きするユースケースで、PUT リクエストから呼ばれます。
 * - 入力値の検証と監査ログをセットで処理し、Repository への update 呼び出しをカプセル化します。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.datetime.Clock

/**
 * ユーザプロフィール更新ユースケース。
 */
class UpdateUserUseCase(
    private val userRepository: UserRepository,
    private val auditLogger: AuditLogger
) {

    data class Command(
        val userId: Long,
        val name: String,
        val nickname: String,
        val storeName: String,
        val prefectureCode: String,
        val email: String
    )

    suspend operator fun invoke(command: Command, auditContext: AuditContext): User {
        command.validateFields()

        val current = userRepository.findById(command.userId)
            ?: throw IllegalArgumentException("User not found: ${command.userId}")

        if (command.email != current.email) {
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

        val updated = current.copy(
            name = command.name,
            nickname = command.nickname,
            storeName = command.storeName,
            prefectureCode = command.prefectureCode,
            email = command.email,
            updatedAt = Clock.System.now()
        )
        val result = userRepository.updateUser(updated)

        auditLogger.log(
            entityType = "USER",
            entityId = result.id ?: command.userId,
            action = "PUT",
            context = auditContext,
            before = current,
            after = result
        )
        return result
    }
}

