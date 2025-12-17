package com.example.usecase.user

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.User
import com.example.domain.repository.StoreMasterRepository
import com.example.domain.repository.UserCredentialRepository
import com.example.domain.repository.UserRepository
import com.example.usecase.settings.CreateDefaultGameSettingsUseCase
import kotlinx.datetime.Clock

/**
 * ユーザーと認証情報をまとめて登録するユースケース。
 */
class CreateUserUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val storeMasterRepository: StoreMasterRepository,
    private val createDefaultGameSettingsUseCase: CreateDefaultGameSettingsUseCase
) {

    data class Command(
        val name: String,
        val nickname: String,
        val storeId: Long,
        val prefectureCode: String,
        val email: String,
        val zooId: Int,
        val password: String,
        val passwordConfirm: String
    )

    suspend operator fun invoke(command: Command): User {
        if (command.password != command.passwordConfirm) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "passwordConfirm",
                        code = "PASSWORD_NOT_MATCH",
                        message = "確認用パスワードが一致しません。"
                    )
                ),
                message = "確認用パスワードが一致しません。"
            )
        }
        command.validateFields()
        PasswordPolicy.validate(command.password)

        val existing = userRepository.findByEmail(command.email)
        if (existing != null) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "email",
                        code = "EMAIL_ALREADY_EXISTS",
                        message = "このメールアドレスは既に使用されています。"
                    )
                ),
                message = "このメールアドレスは既に使用されています。"
            )
        }
        val zooDuplicated = userRepository.findByZooId(command.zooId)
        if (zooDuplicated != null) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "zooId",
                        code = "ZOO_ID_ALREADY_EXISTS",
                        message = "このZooIDは既に使用されています。"
                    )
                ),
                message = "このZooIDは既に使用されています。"
            )
        }

        val store = storeMasterRepository.findById(command.storeId) ?: throw DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "storeId",
                    code = "STORE_NOT_FOUND",
                    message = "選択した店舗が存在しません"
                )
            ),
            message = "Store not found"
        )

        val now = Clock.System.now()
        val created = userRepository.createUser(
            User(
                id = null,
                name = command.name,
                nickname = command.nickname,
                storeId = store.id,
                storeName = store.storeName,
                prefectureCode = command.prefectureCode,
                email = command.email,
                zooId = command.zooId,
                isAdmin = false,
                isDeleted = false,
                createdAt = now,
                updatedAt = now
            )
        )

        val hash = BCrypt.withDefaults().hashToString(12, command.password.toCharArray())
        credentialRepository.createCredentials(created.id!!, command.email, hash)
        createDefaultGameSettingsUseCase(created.id)
        return created
    }
}
