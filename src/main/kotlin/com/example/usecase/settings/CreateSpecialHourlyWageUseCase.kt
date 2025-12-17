package com.example.usecase.settings

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.repository.SpecialHourlyWageRepository

/**
 * 特別時給を新規登録するユースケース。
 */
class CreateSpecialHourlyWageUseCase(
    private val repository: SpecialHourlyWageRepository
) {
    /**
     * 特別時給作成に必要なユーザー ID・ラベル・時給を保持するコマンド。
     */
    data class Command(
        val userId: Long,
        val label: String,
        val hourlyWage: Int
    )

    /**
     * ラベルと単価を検証したうえで特別時給を作成する。
     */
    suspend operator fun invoke(command: Command) = run {
        val normalizedLabel = command.label.trim()
        validate(command.userId, normalizedLabel, command.hourlyWage)
        repository.insert(command.userId, normalizedLabel, command.hourlyWage)
    }

    private suspend fun validate(userId: Long, label: String, hourlyWage: Int) {
        val errors = mutableListOf<FieldError>()

        if (label.isEmpty() || label.length !in 1..50) {
            errors += FieldError(
                field = "label",
                code = "INVALID_LABEL",
                message = "特別手当名が無効です。"
            )
        } else if (repository.existsLabel(userId, label)) {
            errors += FieldError(
                field = "label",
                code = "INVALID_LABEL",
                message = "特別手当名が無効です。"
            )
        }

        if (hourlyWage < 0 || hourlyWage > 100_000) {
            errors += FieldError(
                field = "hourlyWage",
                code = "INVALID_HOURLY_WAGE",
                message = "特別手当時給が不正です。"
            )
        }

        if (errors.isNotEmpty()) {
            throw DomainValidationException(errors)
        }
    }
}
