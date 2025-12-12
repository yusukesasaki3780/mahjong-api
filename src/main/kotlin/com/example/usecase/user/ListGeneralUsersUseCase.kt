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
    suspend operator fun invoke(): List<User> = userRepository.listNonAdminUsers()
}

