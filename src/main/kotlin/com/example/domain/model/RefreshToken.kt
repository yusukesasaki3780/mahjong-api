package com.example.domain.model

import kotlinx.datetime.Instant

/**
 * Refresh Token の永続化モデル。
 */
data class RefreshToken(
    val id: Long?,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val createdAt: Instant
)
