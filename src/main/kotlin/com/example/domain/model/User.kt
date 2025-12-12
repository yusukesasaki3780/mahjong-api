package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 従業員アカウントを表すドメインモデル。
 */
@Serializable
data class User(
    val id: Long? = null,
    val name: String,
    val nickname: String,
    val storeName: String,
    val prefectureCode: String,
    val email: String,
    val zooId: Int,
    val isAdmin: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
