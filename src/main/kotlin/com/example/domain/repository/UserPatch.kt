package com.example.domain.repository

import kotlinx.datetime.Instant

/**
 * ユーザ部分更新で指定されたフィールド群。
 */
data class UserPatch(
    val name: String? = null,
    val nickname: String? = null,
    val storeName: String? = null,
    val prefectureCode: String? = null,
    val email: String? = null,
    val updatedAt: Instant? = null
)
