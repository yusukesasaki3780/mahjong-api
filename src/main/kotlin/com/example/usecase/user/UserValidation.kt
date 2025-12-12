package com.example.usecase.user

/**
 * ### このファイルの役割
 * - ユーザー関連の Valiktor 検証を集約したファイルです。
 * - 名前や都道府県コードといった共通ルールをここで定義し、複数ユースケースから再利用します。
 */

import org.valiktor.Constraint
import org.valiktor.functions.hasSize
import org.valiktor.functions.isBetween
import org.valiktor.functions.matches
import org.valiktor.validate

private val PREFECTURE_CODE_REGEX = Regex("^\\d{2}\$")
private val EMAIL_REGEX =
    Regex("^(?=.{1,255}\$)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}\$", RegexOption.IGNORE_CASE)
private const val NAME_MIN = 1
private const val NAME_MAX = 100
private const val PASSWORD_MIN = 8
private const val PASSWORD_MAX = 100

internal fun CreateUserUseCase.Command.validateFields() {
    validate(this) {
        validate(CreateUserUseCase.Command::name).hasSize(NAME_MIN, NAME_MAX)
        validate(CreateUserUseCase.Command::nickname).hasSize(NAME_MIN, NAME_MAX)
        validate(CreateUserUseCase.Command::storeId).isBetween(1L, Long.MAX_VALUE)
        validate(CreateUserUseCase.Command::prefectureCode).matches(PREFECTURE_CODE_REGEX)
        validate(CreateUserUseCase.Command::email).matches(EMAIL_REGEX)
        validate(CreateUserUseCase.Command::zooId).isBetween(1, 999_999)
        validate(CreateUserUseCase.Command::password).hasSize(PASSWORD_MIN, PASSWORD_MAX)
        validate(CreateUserUseCase.Command::passwordConfirm).hasSize(PASSWORD_MIN, PASSWORD_MAX)
    }
}

internal fun UpdateUserUseCase.Command.validateFields() {
    validate(this) {
        validate(UpdateUserUseCase.Command::name).hasSize(NAME_MIN, NAME_MAX)
        validate(UpdateUserUseCase.Command::nickname).hasSize(NAME_MIN, NAME_MAX)
        validate(UpdateUserUseCase.Command::storeName).hasSize(NAME_MIN, NAME_MAX)
        validate(UpdateUserUseCase.Command::prefectureCode).matches(PREFECTURE_CODE_REGEX)
        validate(UpdateUserUseCase.Command::email).matches(EMAIL_REGEX)
    }
}

internal fun PatchUserUseCase.Command.validateFields() {
    validate(this) {
        validate(PatchUserUseCase.Command::name).optionalSize()
        validate(PatchUserUseCase.Command::nickname).optionalSize()
        validate(PatchUserUseCase.Command::storeName).optionalSize()
        validate(PatchUserUseCase.Command::prefectureCode).optionalMatches()
        validate(PatchUserUseCase.Command::email).optionalEmail()
    }
}

private fun org.valiktor.Validator<PatchUserUseCase.Command>.Property<String?>.optionalSize() =
    validate(SizeConstraint) { value -> value == null || value.length in NAME_MIN..NAME_MAX }

private fun org.valiktor.Validator<PatchUserUseCase.Command>.Property<String?>.optionalMatches() =
    validate(MatchesConstraint) { value -> value == null || PREFECTURE_CODE_REGEX.matches(value) }

private fun org.valiktor.Validator<PatchUserUseCase.Command>.Property<String?>.optionalEmail() =
    validate(MatchesConstraint) { value -> value == null || EMAIL_REGEX.matches(value) }

private object SizeConstraint : Constraint {
    override val name: String = "Size"
}

private object MatchesConstraint : Constraint {
    override val name: String = "Matches"
}

