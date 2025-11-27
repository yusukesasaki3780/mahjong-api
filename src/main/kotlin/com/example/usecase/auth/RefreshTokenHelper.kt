package com.example.usecase.auth

/**
 * ### このファイルの役割
 * - リフレッシュトークンの生成・ハッシュ化・比較を担うユーティリティです。
 * - セキュアな乱数の取得や Bcrypt ハッシュの作成を共通化し、ユースケース側の複雑さを減らしています。
 */

import at.favre.lib.crypto.bcrypt.BCrypt
import java.security.SecureRandom
import java.util.Base64
import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

internal object RefreshTokenHelper {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private const val TOKEN_BYTE_SIZE = 32
    private val bcrypt = BCrypt.withDefaults()
    private val verifier = BCrypt.verifyer()

    fun generateToken(userId: Long): String {
        val randomPart = generateRandomPart()
        return "$userId.$randomPart"
    }

    private fun generateRandomPart(): String {
        val bytes = ByteArray(TOKEN_BYTE_SIZE)
        secureRandom.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun hash(token: String): String = bcrypt.hashToString(12, token.toCharArray())

    fun verify(raw: String, hashed: String): Boolean =
        verifier.verify(raw.toCharArray(), hashed).verified

    fun extractUserId(token: String): Long? {
        val separatorIndex = token.indexOf('.')
        if (separatorIndex <= 0) return null
        return token.substring(0, separatorIndex).toLongOrNull()
    }
}

internal fun Instant.plusDays(days: Int): Instant =
    this.plus(days.toLong() * 24 * 60 * 60, DateTimeUnit.SECOND)

