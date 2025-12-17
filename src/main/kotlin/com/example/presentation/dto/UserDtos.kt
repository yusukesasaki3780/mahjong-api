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
    val email: String,
    val currentPassword: String? = null,
    val newPassword: String? = null
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
    val email: String? = null,
    val currentPassword: String? = null,
    val newPassword: String? = null
)

@Serializable
data class UserResponse(
    val id: Long,
    val name: String,
    val nickname: String,
    val storeId: Long,
    val storeName: String,
    val prefectureCode: String,
    val email: String,
    val zooId: Int,
    val isAdmin: Boolean,
    val isDeleted: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id!!,
            name = user.name,
            nickname = user.nickname,
            storeId = user.storeId,
            storeName = user.storeName,
            prefectureCode = user.prefectureCode,
            email = user.email,
            zooId = user.zooId,
            isAdmin = user.isAdmin,
            isDeleted = user.isDeleted,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString()
        )
    }
}

@Serializable
data class AdminUserSummaryResponse(
    val id: Long,
    val name: String,
    val nickname: String,
    val email: String,
    val storeId: Long,
    val storeName: String,
    val isDeleted: Boolean
) {
    companion object {
        fun from(user: User) = AdminUserSummaryResponse(
            id = user.id!!,
            name = user.name,
            nickname = user.nickname,
            email = user.email,
            storeId = user.storeId,
            storeName = user.storeName,
            isDeleted = user.isDeleted
        )
    }
}

@Serializable
data class AdminPasswordResetRequest(
    val newPassword: String
)

@Serializable
data class AdminPasswordResetResponse(
    val status: String = "ok"
)
