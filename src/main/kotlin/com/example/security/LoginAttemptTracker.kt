package com.example.security

import java.util.concurrent.ConcurrentHashMap
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * ログイン試行回数とロック時間を管理する簡易トラッカー。
 * ユーザーIDとIPの組み合わせごとに失敗回数・ロック状態を保持します。
 */
class LoginAttemptTracker(
    private val firstThreshold: Int = 5,
    private val firstLockDuration: Duration = 5.minutes,
    private val secondThreshold: Int = 10,
    private val secondLockDuration: Duration = 30.minutes,
    private val clock: Clock = Clock.System
) {

    private data class AttemptState(
        var failures: Int = 0,
        var lockedUntil: Instant? = null
    )

    private val attempts = ConcurrentHashMap<String, AttemptState>()

    fun currentLockRemaining(email: String, ip: String): Duration? {
        val state = attempts[key(email, ip)] ?: return null
        val now = clock.now()
        synchronized(state) {
            val lockedUntil = state.lockedUntil ?: return null
            return if (lockedUntil <= now) {
                state.failures = 0
                state.lockedUntil = null
                null
            } else {
                val millis = lockedUntil.toEpochMilliseconds() - now.toEpochMilliseconds()
                millis.coerceAtLeast(0).milliseconds
            }
        }
    }

    fun registerFailure(email: String, ip: String): Duration? {
        val state = attempts.computeIfAbsent(key(email, ip)) { AttemptState() }
        val now = clock.now()
        synchronized(state) {
            if (state.lockedUntil != null && state.lockedUntil!! <= now) {
                state.failures = 0
                state.lockedUntil = null
            }
            state.failures++
            val lockDuration = when {
                state.failures >= secondThreshold -> secondLockDuration
                state.failures >= firstThreshold -> firstLockDuration
                else -> null
            }
            if (lockDuration != null) {
                state.lockedUntil = now.plus(lockDuration.inWholeMilliseconds, DateTimeUnit.MILLISECOND)
                state.failures = 0
            }
            return lockDuration
        }
    }

    fun reset(email: String, ip: String) {
        attempts.remove(key(email, ip))
    }

    private fun key(email: String, ip: String): String =
        "${email.lowercase()}|$ip"
}
