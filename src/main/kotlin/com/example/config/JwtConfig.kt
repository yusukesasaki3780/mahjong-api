package com.example.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import java.util.Date
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaInstant

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiresInSec: Long
) {
    companion object {
        fun from(appConfig: ApplicationConfig): JwtConfig {
            val config = appConfig.config("ktor.jwt")
            return JwtConfig(
                secret = config.property("secret").getString(),
                issuer = config.property("issuer").getString(),
                audience = config.property("audience").getString(),
                realm = config.property("realm").getString(),
                expiresInSec = config.property("expiresInSec").getString().toLong()
            )
        }
    }
}

data class JwtTokenResult(val token: String, val issuedAt: Instant)

class JwtProvider(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun generateToken(userId: Long): JwtTokenResult {
        val issuedAt = Clock.System.now()
        val expiresAt = issuedAt.plus(config.expiresInSec, DateTimeUnit.SECOND)
        val token = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim("userId", userId)
            .withIssuedAt(Date.from(issuedAt.toJavaInstant()))
            .withExpiresAt(Date.from(expiresAt.toJavaInstant()))
            .sign(algorithm)
        return JwtTokenResult(token = token, issuedAt = issuedAt)
    }
}
