package com.example.domain.model

import kotlinx.datetime.Instant

/**
 * ランキングなどで利用する命名付き期間。
 */
data class StatsPeriod(
    val name: String,
    val start: Instant,
    val end: Instant
)
