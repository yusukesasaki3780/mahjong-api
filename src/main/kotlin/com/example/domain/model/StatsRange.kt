package com.example.domain.model

import kotlinx.datetime.Instant

/**
 * 任意の統計集計期間を表す時間範囲。
 *
 * - [start]: 範囲の開始（含む）
 * - [end]: 範囲の終了（含まない）で、データ取得時は `value < end` として扱う。
 */
data class StatsRange(
    val start: Instant,
    val end: Instant
)
