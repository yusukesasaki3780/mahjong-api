package com.example.usecase.game

/**
 * ### このファイルの役割
 * - ゲーム種別ごとの着順ルールや収支計算ルールといったドメインバリデーションを集約したヘルパーです。
 * - ユースケースから呼び出すことで同じロジックを重複させずに済むようにしています。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.GameType
import com.example.domain.repository.GameSettingsRepository
import kotlinx.datetime.LocalDate

internal const val DEFAULT_YONMA_TIP_UNIT = 100
internal const val DEFAULT_SANMA_TIP_UNIT = 50
internal const val DEFAULT_YONMA_GAME_FEE = 400
internal const val DEFAULT_SANMA_GAME_FEE = 250
internal const val DEFAULT_SANMA_GAME_FEE_BACK = 0
internal val MIN_PLAYED_DATE: LocalDate = LocalDate(1970, 1, 1)

internal data class ResolvedGameSettings(
    val yonmaGameFee: Int,
    val sanmaGameFee: Int,
    val sanmaGameFeeBack: Int,
    val yonmaTipUnit: Int,
    val sanmaTipUnit: Int
) {
    fun tipUnit(gameType: GameType): Int = when (gameType) {
        GameType.YONMA -> yonmaTipUnit
        GameType.SANMA -> sanmaTipUnit
    }
}

internal suspend fun GameSettingsRepository.resolveSettings(userId: Long): ResolvedGameSettings {
    val settings = getSettings(userId)
    return ResolvedGameSettings(
        yonmaGameFee = settings?.yonmaGameFee ?: DEFAULT_YONMA_GAME_FEE,
        sanmaGameFee = settings?.sanmaGameFee ?: DEFAULT_SANMA_GAME_FEE,
        sanmaGameFeeBack = settings?.sanmaGameFeeBack ?: DEFAULT_SANMA_GAME_FEE_BACK,
        yonmaTipUnit = settings?.yonmaTipUnit ?: DEFAULT_YONMA_TIP_UNIT,
        sanmaTipUnit = settings?.sanmaTipUnit ?: DEFAULT_SANMA_TIP_UNIT
    )
}

internal fun calculateExpectedTotalIncome(
    gameType: GameType,
    baseIncome: Long,
    tipIncome: Long,
    otherIncome: Long,
    place: Int,
    settings: ResolvedGameSettings
): Long {
    val basePlusTip = baseIncome + tipIncome + otherIncome
    return when (gameType) {
        GameType.YONMA -> {
            var expected = basePlusTip
            if (place == 1) {
                expected -= settings.yonmaGameFee
            }
            expected
        }
        GameType.SANMA -> {
            var expected = basePlusTip
            if (place == 1) {
                expected -= settings.sanmaGameFee
            }
            expected + settings.sanmaGameFeeBack
        }
    }
}

internal fun ensureTotalIncomeMatches(
    totalIncome: Long,
    gameType: GameType,
    baseIncome: Long,
    tipIncome: Long,
    otherIncome: Long,
    place: Int,
    settings: ResolvedGameSettings
) {
    val expected = calculateExpectedTotalIncome(gameType, baseIncome, tipIncome, otherIncome, place, settings)
    if (totalIncome != expected) {
        throw DomainValidationException(
            listOf(
                FieldError(
                    field = "totalIncome",
                    code = "TOTAL_INCOME_MISMATCH",
                    message = "totalIncome must equal $expected for ${gameType.name} games"
                )
            )
        )
    }
}
