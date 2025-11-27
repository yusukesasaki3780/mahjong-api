package com.example.presentation.dto

import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import kotlinx.serialization.Serializable

/**
 * ゲーム設定 API で使用する DTO。
 */
@Serializable
data class GameSettingsResponse(
    val userId: Long,
    val yonmaGameFee: Int,
    val sanmaGameFee: Int,
    val sanmaGameFeeBack: Int,
    val yonmaTipUnit: Int,
    val sanmaTipUnit: Int,
    val wageType: WageType,
    val hourlyWage: Int,
    val fixedSalary: Int?,
    val nightRateMultiplier: Double,
    val baseMinWage: Int,
    val incomeTaxRate: Double?,
    val transportPerShift: Int?
) {
    companion object {
        fun from(settings: GameSettings) = GameSettingsResponse(
            userId = settings.userId,
            yonmaGameFee = settings.yonmaGameFee,
            sanmaGameFee = settings.sanmaGameFee,
            sanmaGameFeeBack = settings.sanmaGameFeeBack,
            yonmaTipUnit = settings.yonmaTipUnit,
            sanmaTipUnit = settings.sanmaTipUnit,
            wageType = settings.wageType,
            hourlyWage = settings.hourlyWage,
            fixedSalary = settings.fixedSalary.takeIf { settings.wageType == WageType.FIXED },
            nightRateMultiplier = settings.nightRateMultiplier,
            baseMinWage = settings.baseMinWage,
            incomeTaxRate = settings.incomeTaxRate,
            transportPerShift = settings.transportPerShift
        )
    }
}

@Serializable
data class UpdateGameSettingsRequest(
    val yonmaGameFee: Int,
    val sanmaGameFee: Int,
    val sanmaGameFeeBack: Int,
    val yonmaTipUnit: Int,
    val sanmaTipUnit: Int,
    val wageType: WageType,
    val hourlyWage: Int,
    val fixedSalary: Int? = null,
    val nightRateMultiplier: Double,
    val baseMinWage: Int,
    val incomeTaxRate: Double?,
    val transportPerShift: Int?
)

/**
 * 部分更新用 DTO。指定された項目のみ更新する。
 */
@Serializable
data class PatchGameSettingsRequest(
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
    val transportPerShift: Int? = null
)
