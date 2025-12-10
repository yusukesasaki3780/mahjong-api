package com.example.usecase.game

/**
 * ### このファイルの役割
 * - ゲーム結果の一部項目だけを更新する PATCH 用ユースケースです。
 * - 差分適用のためのパッチモデルを組み立て、監査ログも部分更新に対応できるようにしています。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.repository.GameResultPatch
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.slf4j.LoggerFactory

class PatchGameResultUseCase(
    private val repository: GameResultRepository,
    private val settingsRepository: GameSettingsRepository,
    private val auditLogger: AuditLogger,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    data class Command(
        val userId: Long,
        val resultId: Long,
        val gameType: GameType? = null,
        val playedAt: LocalDate? = null,
        val place: Int? = null,
        val baseIncome: Long? = null,
        val tipCount: Int? = null,
        val tipIncome: Long? = null,
        val otherIncome: Long? = null,
        val totalIncome: Long? = null,
        val note: String? = null
    )

    suspend operator fun invoke(command: Command, auditContext: AuditContext): GameResult {
        val before = repository.findById(command.resultId)
            ?: throw IllegalArgumentException("Game result not found: ${command.resultId}")

        validate(command, before)

        val patch = GameResultPatch(
            gameType = command.gameType ?: before.gameType,
            playedAt = command.playedAt?.atStartOfDayIn(timeZone),
            place = command.place,
            baseIncome = command.baseIncome,
            tipCount = command.tipCount,
            tipIncome = command.tipIncome,
            otherIncome = command.otherIncome,
            totalIncome = command.totalIncome,
            note = command.note,
            updatedAt = Clock.System.now()
        )
        val result = repository.patchGameResult(command.userId, command.resultId, patch)

        auditLogger.log(
            entityType = "GAME_RESULT",
            entityId = command.resultId,
            action = "PATCH",
            context = auditContext,
            before = before,
            after = result
        )
        return result
    }

    private suspend fun validate(command: Command, existing: GameResult) {
        command.playedAt?.let {
            if (it < MIN_PLAYED_DATE) {
                throw DomainValidationException(
                    listOf(
                        FieldError(
                            field = "playedAt",
                            code = "INVALID_PLAYED_AT",
                            message = "playedAt must be on or after 1970-01-01"
                        )
                    )
                )
            }
        }

        command.place?.let { place ->
            val type = command.gameType ?: existing.gameType
            val valid = when (type) {
                GameType.YONMA -> place in 1..4
                GameType.SANMA -> place in 1..3
            }
            if (!valid) {
                throw DomainValidationException(
                    listOf(
                        FieldError(
                            field = "place",
                            code = "PLACE_OUT_OF_RANGE",
                            message = "place must match the range allowed by $type"
                        )
                    )
                )
            }
        }

        val incomeFields = listOf(
            command.baseIncome,
            command.tipIncome,
            command.tipCount,
            command.otherIncome,
            command.totalIncome
        )
        val hasIncomeUpdates = incomeFields.any { it != null }
        if (hasIncomeUpdates && incomeFields.any { it == null }) {
            throw DomainValidationException(
                listOf(
                    FieldError(
                        field = "baseIncome/tipIncome/tipCount/otherIncome/totalIncome",
                        code = "INCOMPLETE_INCOME_SET",
                        message = "Updating income requires all related fields together"
                    )
                )
            )
        }

        if (hasIncomeUpdates) {
            val type = command.gameType ?: existing.gameType
            val settings = settingsRepository.resolveSettings(command.userId)
            val tipUnit = settings.tipUnit(type)
            validateIncomeSet(
                userId = command.userId,
                resultId = command.resultId,
                tipCount = command.tipCount!!,
                tipIncome = command.tipIncome!!,
                baseIncome = command.baseIncome!!,
                otherIncome = command.otherIncome!!,
                totalIncome = command.totalIncome!!,
                tipUnit = tipUnit,
                gameType = type,
                place = command.place ?: existing.place,
                settings = settings
            )
        }
    }

    private fun validateIncomeSet(
        userId: Long,
        resultId: Long,
        tipCount: Int,
        tipIncome: Long,
        baseIncome: Long,
        otherIncome: Long,
        totalIncome: Long,
        tipUnit: Int,
        gameType: GameType,
        place: Int,
        settings: ResolvedGameSettings
    ) {
        val expectedTip = tipCount.toLong() * tipUnit
        if (tipIncome != expectedTip) {
            logger.warn(
                "tipIncome mismatch on patch user={} result={} expected={} actual={}",
                userId,
                resultId,
                expectedTip,
                tipIncome
            )
        }
        ensureTotalIncomeMatches(
            totalIncome = totalIncome,
            gameType = gameType,
            baseIncome = baseIncome,
            tipIncome = tipIncome,
            otherIncome = otherIncome,
            place = place,
            settings = settings
        )
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(PatchGameResultUseCase::class.java)
    }
}

