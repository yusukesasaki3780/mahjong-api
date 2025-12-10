package com.example.usecase.game

/**
 * ### このファイルの役割
 * - ゲーム結果を丸ごと更新するユースケースで、検証と監査ログを一括で扱います。
 * - 全項目を上書きする PUT 操作の実態をここに閉じ込めています。
 */

import com.example.domain.model.AuditContext
import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import com.example.infrastructure.logging.AuditLogger
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
 * ゲーム結果の修正を行うユースケース。
 */
class EditGameResultUseCase(
    private val repository: GameResultRepository,
    private val settingsRepository: GameSettingsRepository,
    private val auditLogger: AuditLogger,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    data class Command(
        val id: Long,
        val userId: Long,
        val gameType: GameType,
        val playedAt: LocalDate,
        val place: Int,
        val baseIncome: Long,
        val tipCount: Int,
        val tipIncome: Long,
        val otherIncome: Long,
        val totalIncome: Long,
        val note: String? = null,
        val createdAt: Instant
    )

    suspend operator fun invoke(command: Command, auditContext: AuditContext): GameResult {
        val settings = settingsRepository.resolveSettings(command.userId)
        val tipUnit = settings.tipUnit(command.gameType)
        command.validate(tipUnit)

        val before = repository.findById(command.id)
            ?: throw IllegalArgumentException("Game result not found: ${command.id}")

        val playedAtInstant = command.playedAt.atStartOfDayIn(timeZone)
        if (command.tipIncome != command.tipCount.toLong() * tipUnit) {
            logger.warn(
                "tipIncome mismatch on edit user={} result={} expected={} actual={}",
                command.userId,
                command.id,
                command.tipCount.toLong() * tipUnit,
                command.tipIncome
            )
        }
        ensureTotalIncomeMatches(
            totalIncome = command.totalIncome,
            gameType = command.gameType,
            baseIncome = command.baseIncome,
            tipIncome = command.tipIncome,
            otherIncome = command.otherIncome,
            place = command.place,
            settings = settings
        )

        val updated = GameResult(
            id = command.id,
            userId = command.userId,
            gameType = command.gameType,
            playedAt = playedAtInstant,
            place = command.place,
            baseIncome = command.baseIncome,
            tipCount = command.tipCount,
            tipIncome = command.tipIncome,
            otherIncome = command.otherIncome,
            totalIncome = command.totalIncome,
            note = command.note,
            createdAt = command.createdAt,
            updatedAt = Clock.System.now()
        )
        val result = repository.updateGameResult(updated)

        auditLogger.log(
            entityType = "GAME_RESULT",
            entityId = command.id,
            action = "PUT",
            context = auditContext,
            before = before,
            after = result
        )
        return result
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
        private val logger = LoggerFactory.getLogger(EditGameResultUseCase::class.java)
    }
}

