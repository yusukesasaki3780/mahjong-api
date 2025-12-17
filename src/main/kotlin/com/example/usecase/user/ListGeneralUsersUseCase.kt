package com.example.usecase.user

/**
 * ### このファイルの役割
 * - 管理者向けに一般ユーザー（非管理者ユーザー）一覧を取得するユースケースです。
 */

import com.example.domain.model.User
import com.example.domain.repository.UserRepository

/**
 * 店舗に紐づくユーザー一覧を取得するユースケース。
 */
class ListGeneralUsersUseCase(
    private val userRepository: UserRepository
) {
    /**
     * 店舗 ID をもとに、削除済み・管理者含むかの条件を指定してユーザーを取得する。
     */
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
