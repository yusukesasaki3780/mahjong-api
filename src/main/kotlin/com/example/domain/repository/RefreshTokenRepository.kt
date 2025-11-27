package com.example.domain.repository

import com.example.domain.model.RefreshToken
import kotlinx.datetime.Instant

/**
 * Refresh Token の永続化リポジトリ。
 */
interface RefreshTokenRepository {
    suspend fun create(userId: Long, tokenHash: String, expiresAt: Instant): RefreshToken
    suspend fun findValidTokens(userId: Long, now: Instant): List<RefreshToken>
    suspend fun delete(tokenId: Long)
    suspend fun deleteAllForUser(userId: Long)
    suspend fun deleteExpired(now: Instant)
}
