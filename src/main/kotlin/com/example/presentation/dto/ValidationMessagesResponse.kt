package com.example.presentation.dto

import kotlinx.serialization.Serializable

/**
 * バリデーションエラーをユーザー向けに伝えるレスポンス。
 */
@Serializable
data class ValidationMessagesResponse(
    val message: String,
    val errors: List<ValidationMessageItem>
)

@Serializable
data class ValidationMessageItem(
    val message: String,
    val field: String? = null
)

