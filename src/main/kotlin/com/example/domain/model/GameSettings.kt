package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * ゲーム料金や給与系のユーザ設定を表す。
 */
@Serializable
data class GameSettings(
    val userId: Long,
    val yonmaGameFee: Int = 400,
    val sanmaGameFee: Int = 250,
    val sanmaGameFeeBack: Int = 0,
    val yonmaTipUnit: Int = 100,
    val sanmaTipUnit: Int = 50,
    val wageType: WageType = WageType.HOURLY,
    val hourlyWage: Int = 1226,
    val fixedSalary: Int = 350_000,
    val nightRateMultiplier: Double = 1.25,
    val baseMinWage: Int = 1226,
    val incomeTaxRate: Double? = null,
    val transportPerShift: Int? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
