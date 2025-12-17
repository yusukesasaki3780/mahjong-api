package com.example.usecase.user

/**
 * ### このファイルの役割
 * - 管理者向けに一般ユーザー（非管理者ユーザー）一覧を取得するユースケースです。
 */

import com.example.domain.model.User
import com.example.domain.repository.UserRepository

class ListGeneralUsersUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        storeId: Long,
        includeDeleted: Boolean = false,
        includeAdmins: Boolean = false
    ): List<User> =
        userRepository.listUsers(
            storeId = storeId,
            includeDeleted = includeDeleted,
            includeAdmins = includeAdmins
        )
}
