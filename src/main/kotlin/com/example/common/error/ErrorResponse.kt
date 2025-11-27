package com.example.common.error

import kotlinx.serialization.Serializable

/**
 * API 共通のエラーレスポンスを表す DTO。
 */
@Serializable
data class ErrorResponse(
    val errorCode: String,
    val message: String,
    val errors: List<FieldError> = emptyList()
)

/**
 * 単一フィールドのエラー詳細。
 */
@Serializable
data class FieldError(
    val field: String?,
    val code: String,
    val message: String
)

/**
 * ドメインルール違反時に投げる例外。
 */
class DomainValidationException(
    val violations: List<FieldError>,
    override val message: String = "Domain validation failed"
) : RuntimeException(message)
