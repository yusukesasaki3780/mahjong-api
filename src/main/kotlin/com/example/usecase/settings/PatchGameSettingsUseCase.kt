package com.example.usecase.settings

/**
 * ### このファイルの役割
 * - ゲーム設定の一部項目だけを更新するユースケースで、差分更新や監査ログ記録を担います。
 * - Command の nullable プロパティを利用して、指定があった項目だけを Repository に渡します。
 */

import com.example.domain.model.AuditContext
import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import com.example.domain.repository.GameSettingsPatch
import com.example.domain.repository.GameSettingsRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.datetime.Clock

class PatchGameSettingsUseCase(
    private val repository: GameSettingsRepository,
    private val auditLogger: AuditLogger
) {

    data class Command(
        val userId: Long,
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

    suspend operator fun invoke(command: Command, auditContext: AuditContext): GameSettings {
        command.validateFields()

        val before = repository.getSettings(command.userId)
            ?: throw IllegalArgumentException("Game settings not found for user ${command.userId}")

        val targetWageType = command.wageType ?: before.wageType
        val resolvedFixedSalary = command.fixedSalary ?: before.fixedSalary
        requireFixedSalaryWhenNeeded(targetWageType, resolvedFixedSalary)

        val patch = GameSettingsPatch(
            yonmaGameFee = command.yonmaGameFee,
            sanmaGameFee = command.sanmaGameFee,
            sanmaGameFeeBack = command.sanmaGameFeeBack,
            yonmaTipUnit = command.yonmaTipUnit,
            sanmaTipUnit = command.sanmaTipUnit,
            wageType = command.wageType,
            hourlyWage = command.hourlyWage,
            fixedSalary = command.fixedSalary,
            nightRateMultiplier = command.nightRateMultiplier,
            baseMinWage = command.baseMinWage,
            incomeTaxRate = command.incomeTaxRate,
            transportPerShift = command.transportPerShift,
            updatedAt = Clock.System.now()
        )
        val result = repository.patchSettings(command.userId, patch)

        auditLogger.log(
            entityType = "GAME_SETTINGS",
            entityId = command.userId,
            action = "PATCH",
            context = auditContext,
            before = before,
            after = result
        )
        return result
    }
}

