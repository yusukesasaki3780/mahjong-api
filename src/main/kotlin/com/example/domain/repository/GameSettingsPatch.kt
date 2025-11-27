package com.example.domain.repository

import com.example.domain.model.WageType
import kotlinx.datetime.Instant

/**
 * ゲーム設定の部分更新値。
 */
data class GameSettingsPatch(
    val yonmaGameFee: Int? = null,
    val sanmaGameFee: Int? = null,
    val sanmaGameFeeBack: Int? = null,
    val yonmaTipUnit: Int? = null,
    val sanmaTipUnit: Int? = null,
    val wageType: WageType? = null,
    val hourlyWage: Int? = null,
    val fixedSalary: Int? = null,
    val nightRateMultiplier: Double? = null,
    val baseMinWage: Int? = null,
    val incomeTaxRate: Double? = null,
    val transportPerShift: Int? = null,
    val updatedAt: Instant? = null
)
