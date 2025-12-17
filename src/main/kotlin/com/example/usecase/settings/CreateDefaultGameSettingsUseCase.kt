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

    /**
     * まだ設定が存在しない場合にデフォルト値でゲーム設定を保存する。
     */
    suspend operator fun invoke(userId: Long) {
        val existing = repository.getSettings(userId)
        if (existing != null) return

        val now = Clock.System.now()
        val defaultSettings = GameSettings(
            userId = userId,
            yonmaGameFee = 2000,
            sanmaGameFee = 850,
            sanmaGameFeeBack = 50,
            yonmaTipUnit = 100,
            sanmaTipUnit = 50,
            wageType = WageType.HOURLY,
            hourlyWage = 1226,
            fixedSalary = 0,
            nightRateMultiplier = 1.25,
            baseMinWage = 1226,
            incomeTaxRate = 0.0321,
            transportPerShift = 0,
            createdAt = now,
            updatedAt = now
        )

        repository.updateSettings(userId, defaultSettings)
    }
}
