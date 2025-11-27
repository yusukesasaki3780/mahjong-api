package com.example.usecase.settings

/**
 * ### このファイルの役割
 * - ゲーム設定を全項目上書きするユースケースです。
 * - PUT 操作の入り口として入力検証と監査ログを一括で処理します。
 */

import com.example.domain.model.AuditContext
import com.example.domain.model.GameSettings
import com.example.domain.model.WageType
import com.example.domain.repository.GameSettingsRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.datetime.Clock

class UpdateGameSettingsUseCase(
    private val repository: GameSettingsRepository,
    private val auditLogger: AuditLogger
) {

    data class Command(
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
    )

    suspend operator fun invoke(command: Command, auditContext: AuditContext): GameSettings {
        command.validateFields()
        command.ensureFixedSalaryRequirement()
        val resolvedFixedSalary = command.fixedSalary ?: 0

        val now = Clock.System.now()
        val current = repository.getSettings(command.userId)
        val settings = GameSettings(
            userId = command.userId,
            yonmaGameFee = command.yonmaGameFee,
            sanmaGameFee = command.sanmaGameFee,
            sanmaGameFeeBack = command.sanmaGameFeeBack,
            yonmaTipUnit = command.yonmaTipUnit,
            sanmaTipUnit = command.sanmaTipUnit,
            wageType = command.wageType,
            hourlyWage = command.hourlyWage,
            fixedSalary = resolvedFixedSalary,
            nightRateMultiplier = command.nightRateMultiplier,
            baseMinWage = command.baseMinWage,
            incomeTaxRate = command.incomeTaxRate,
            transportPerShift = command.transportPerShift,
            createdAt = current?.createdAt ?: now,
            updatedAt = now
        )
        val result = repository.updateSettings(command.userId, settings)
        auditLogger.log(
            entityType = "GAME_SETTINGS",
            entityId = command.userId,
            action = "PUT",
            context = auditContext,
            before = current,
            after = result
        )
        return result
    }
}

