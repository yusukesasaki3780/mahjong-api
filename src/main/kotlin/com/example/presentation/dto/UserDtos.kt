package com.example.presentation.dto

import com.example.domain.model.User
import kotlinx.serialization.Serializable

/**
 * ユーザ関連のリクエスト/レスポンス DTO。
 */
@Serializable
data class UpdateUserRequest(
    val name: String,
    val nickname: String,
    val storeName: String,
    val prefectureCode: String,
    val email: String
)

/**
 * 部分更新で使用する nullable DTO。
 */
@Serializable
data class PatchUserRequest(
    val name: String? = null,
    val nickname: String? = null,
    val storeName: String? = null,
    val prefectureCode: String? = null,
    val email: String? = null
)

@Serializable
data class UserResponse(
    val id: Long,
    val name: String,
    val nickname: String,
    val storeName: String,
    val prefectureCode: String,
    val email: String,
    val zooId: Int,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id!!,
            name = user.name,
            nickname = user.nickname,
            storeName = user.storeName,
            prefectureCode = user.prefectureCode,
            email = user.email,
            zooId = user.zooId,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString()
        )
    }
}
