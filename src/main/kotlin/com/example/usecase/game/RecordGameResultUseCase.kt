package com.example.usecase.game

/**
 * ### このファイルの役割
 * - ゲーム結果を新規登録するときの入力検証とオブジェクト生成をまとめたユースケースです。
 * - バリデーションユーティリティを使ってドメインルールを守りつつ、Repository への保存を一元化します。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.slf4j.LoggerFactory
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isIn
import org.valiktor.validate

/**
 * 新しいゲーム結果を保存するユースケース。
 */
class RecordGameResultUseCase(
    private val repository: GameResultRepository,
    private val settingsRepository: GameSettingsRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    data class Command(
        val userId: Long,
        val gameType: GameType,
        val playedAt: LocalDate,
        val place: Int,
        val baseIncome: Long,
        val tipCount: Int,
        val tipIncome: Long,
        val otherIncome: Long = 0,
        val totalIncome: Long,
        val note: String? = null,
        val storeId: Long? = null,
        val simpleBatchId: UUID? = null
    )

    suspend operator fun invoke(command: Command): GameResult {
        val settings = settingsRepository.resolveSettings(command.userId)
        val tipUnit = settings.tipUnit(command.gameType)
        command.validate(tipUnit)

        val isSimpleMode = command.simpleBatchId != null
        if (isSimpleMode && command.storeId == null) {
            throw DomainValidationException(
                listOf(
                    FieldError(
                        field = "storeId",
                        code = "REQUIRED",
                        message = "storeId is required when simple batch mode is enabled."
                    )
                )
            )
        }

        val now = Clock.System.now()
        val playedAtInstant = command.playedAt.atStartOfDayIn(timeZone)
        if (!isSimpleMode && command.tipIncome != command.tipCount.toLong() * tipUnit) {
            logger.warn(
                "tipIncome mismatch for user={} expected={} actual={}",
                command.userId,
                command.tipCount.toLong() * tipUnit,
                command.tipIncome
            )
        }
        if (!isSimpleMode) {
            ensureTotalIncomeMatches(
                totalIncome = command.totalIncome,
                gameType = command.gameType,
                baseIncome = command.baseIncome,
                tipIncome = command.tipIncome,
                otherIncome = command.otherIncome,
                place = command.place,
                settings = settings
            )
        }

        val sanitizedBaseIncome = if (isSimpleMode) 0L else command.baseIncome
        val sanitizedTipCount = if (isSimpleMode) 0 else command.tipCount
        val sanitizedTipIncome = if (isSimpleMode) 0L else command.tipIncome
        val sanitizedOtherIncome = if (isSimpleMode) 0L else command.otherIncome
        val sanitizedTotalIncome = if (isSimpleMode) 0L else command.totalIncome

        val result = GameResult(
            id = null,
            userId = command.userId,
            gameType = command.gameType,
            playedAt = playedAtInstant,
            place = command.place,
            baseIncome = sanitizedBaseIncome,
            tipCount = sanitizedTipCount,
            tipIncome = sanitizedTipIncome,
            otherIncome = sanitizedOtherIncome,
            totalIncome = sanitizedTotalIncome,
            note = command.note,
            storeId = command.storeId,
            simpleBatchId = command.simpleBatchId,
            isFinalIncomeRecord = false,
            createdAt = now,
            updatedAt = now
        )
        return repository.insertGameResult(result)
    }

    private fun Command.validate(tipUnit: Int) {
        validate(this) {
            validate(Command::playedAt).isGreaterThanOrEqualTo(MIN_PLAYED_DATE)
            when (gameType) {
                GameType.YONMA -> validate(Command::place).isIn(1..4)
                GameType.SANMA -> validate(Command::place).isIn(1..3)
            }
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RecordGameResultUseCase::class.java)
    }
}
