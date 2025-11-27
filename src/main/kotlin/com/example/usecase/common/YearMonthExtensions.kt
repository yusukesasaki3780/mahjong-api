package com.example.usecase.common

/**
 * ### このファイルの役割
 * - YearMonth からドメインで使う StatsRange へ変換する拡張関数をまとめています。
 * - 給与計算やダッシュボード統計など、対象月の期間計算が必要な箇所で再利用します。
 */

import com.example.domain.model.StatsRange
import java.time.YearMonth
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

/**
 * YearMonth を StatsRange に変換する拡張関数。
 */
fun YearMonth.toStatsRange(timeZone: TimeZone): StatsRange {
    val startDate = LocalDate(year, monthValue, 1)
    val endDate = LocalDate(year, monthValue, lengthOfMonth())
    val start = startDate.atStartOfDayIn(timeZone)
    val endExclusive = endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone)
    return StatsRange(start, endExclusive)
}
