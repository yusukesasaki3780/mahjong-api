package com.example.usecase.settings

/**
 * ### このファイルの役割
 * - ゲーム設定の入力値を Valiktor で検証するための共通関数群です。
 * - UseCase 側から呼び出して、単価や賃金種別などのドメインルールを一箇所で管理します。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.WageType
import org.valiktor.Constraint
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isPositiveOrZero
import org.valiktor.validate

private const val MIN_DOUBLE_ZERO = 0.0
private const val MAX_TAX_RATE = 1.0

internal fun UpdateGameSettingsUseCase.Command.validateFields() {
    validate(this) {
        validate(UpdateGameSettingsUseCase.Command::yonmaGameFee).isPositiveOrZero()
        validate(UpdateGameSettingsUseCase.Command::sanmaGameFee).isPositiveOrZero()
        validate(UpdateGameSettingsUseCase.Command::sanmaGameFeeBack).isPositiveOrZero()
        validate(UpdateGameSettingsUseCase.Command::yonmaTipUnit).isPositiveOrZero()
        validate(UpdateGameSettingsUseCase.Command::sanmaTipUnit).isPositiveOrZero()
        validate(UpdateGameSettingsUseCase.Command::hourlyWage).isPositiveOrZero()
        validate(UpdateGameSettingsUseCase.Command::fixedSalary).optionalNonNegative()
        validate(UpdateGameSettingsUseCase.Command::nightRateMultiplier).isGreaterThanOrEqualTo(1.0)
        validate(UpdateGameSettingsUseCase.Command::baseMinWage).isPositiveOrZero()
        validate(UpdateGameSettingsUseCase.Command::incomeTaxRate).optionalTaxRate()
        validate(UpdateGameSettingsUseCase.Command::transportPerShift).optionalNonNegative()
    }
}

internal fun PatchGameSettingsUseCase.Command.validateFields() {
    validate(this) {
        validate(PatchGameSettingsUseCase.Command::yonmaGameFee).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::sanmaGameFee).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::sanmaGameFeeBack).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::yonmaTipUnit).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::sanmaTipUnit).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::hourlyWage).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::fixedSalary).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::nightRateMultiplier).optionalNightRate()
        validate(PatchGameSettingsUseCase.Command::baseMinWage).optionalNonNegative()
        validate(PatchGameSettingsUseCase.Command::incomeTaxRate).optionalTaxRate()
        validate(PatchGameSettingsUseCase.Command::transportPerShift).optionalNonNegative()
    }
}

private object NonNegativeConstraint : Constraint {
    override val name: String = "NonNegative"
}

private object NightRateConstraint : Constraint {
    override val name: String = "NightRate"
}

private object TaxRateConstraint : Constraint {
    override val name: String = "TaxRate"
}

private fun <T> org.valiktor.Validator<T>.Property<Int?>.optionalNonNegative() =
    validate(NonNegativeConstraint) { value -> value == null || value >= 0 }

private fun org.valiktor.Validator<PatchGameSettingsUseCase.Command>.Property<Double?>.optionalNightRate() =
    validate(NightRateConstraint) { value -> value == null || value >= 1.0 }

private fun <T> org.valiktor.Validator<T>.Property<Double?>.optionalTaxRate() =
    validate(TaxRateConstraint) { value -> value == null || (value >= MIN_DOUBLE_ZERO && value <= MAX_TAX_RATE) }

internal fun requireFixedSalaryWhenNeeded(wageType: WageType, fixedSalary: Int?) {
    if (wageType == WageType.FIXED && (fixedSalary == null || fixedSalary <= 0)) {
        throw DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "fixedSalary",
                    code = "FIXED_WAGE_REQUIRED",
                    message = "fixedWage is required when wageType is FIXED"
                )
            ),
            message = "fixedWage is required when wageType is FIXED"
        )
    }
}

internal fun UpdateGameSettingsUseCase.Command.ensureFixedSalaryRequirement() =
    requireFixedSalaryWhenNeeded(wageType, fixedSalary)

