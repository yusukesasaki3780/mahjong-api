package com.example.usecase.user

/**
 * ### このファイルの役割
 * - ユーザー情報を ID から取得するだけの薄いユースケースです。
 * - 他レイヤーから Repository への依存を遮断する目的で用意しています。
 */

import com.example.domain.model.User
import com.example.domain.repository.UserRepository

/**
 * 単一ユーザ取得ユースケース。
 */
class GetUserUseCase(
    private val userRepository: UserRepository
) {

    /**
     * 指定 ID のユーザーを取得し、削除済みなら null を返す。
     */
    suspend operator fun invoke(userId: Long): User? =
        userRepository.findById(userId)?.takeIf { !it.isDeleted }
}
