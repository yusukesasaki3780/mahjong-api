package com.example.usecase.settings

import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import com.example.domain.repository.GameSettingsRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

/**
 * ユーザー作成直後にデフォルトのゲーム設定を自動作成するユースケース。
 */
class CreateDefaultGameSettingsUseCase(
    private val repository: GameSettingsRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    suspend operator fun invoke(userId: Long) {
        val existing = repository.getSettings(userId)
        if (existing != null) return

        val now = Clock.System.now()
        val defaultSettings = GameSettings(
            userId = userId,
            yonmaGameFee = 400,
            sanmaGameFee = 300,
            sanmaGameFeeBack = 0,
            yonmaTipUnit = 100,
            sanmaTipUnit = 100,
            wageType = WageType.HOURLY,
            hourlyWage = 1200,
            fixedSalary = 0,
            nightRateMultiplier = 1.25,
            baseMinWage = 1200,
            incomeTaxRate = 0.1021,
            transportPerShift = 500,
            createdAt = now,
            updatedAt = now
        )

        repository.updateSettings(userId, defaultSettings)
    }
}
