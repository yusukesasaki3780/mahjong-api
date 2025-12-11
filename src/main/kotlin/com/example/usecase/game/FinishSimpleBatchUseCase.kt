package com.example.usecase.game

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.GameResult
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import java.util.UUID
import kotlinx.datetime.Clock

/**
 * ### このファイルの役割
 * - まとめて簡単入力モードの終了時に、最終収支レコードを確定させます。
 * - simpleBatchId に紐づく最後のレコードを取り出し、最終収支専用フラグを付けた上で値を書き換えます。
 */
class FinishSimpleBatchUseCase(
    private val repository: GameResultRepository,
    private val settingsRepository: GameSettingsRepository
) {

    data class Command(
        val userId: Long,
        val simpleBatchId: UUID,
        val finalBaseIncome: Long,
        val finalTotalIncome: Long
    )

    suspend operator fun invoke(command: Command): GameResult {
        val latest = repository.findLatestBySimpleBatch(command.userId, command.simpleBatchId)
            ?: throw DomainValidationException(
                listOf(
                    FieldError(
                        field = "simpleBatchId",
                        code = "BATCH_NOT_FOUND",
                        message = "指定した simpleBatchId のレコードが見つかりません。"
                    )
                )
            )

        val settings = settingsRepository.resolveSettings(command.userId)
        ensureTotalIncomeMatches(
            totalIncome = command.finalTotalIncome,
            gameType = latest.gameType,
            baseIncome = command.finalBaseIncome,
            tipIncome = latest.tipIncome,
            otherIncome = latest.otherIncome,
            place = latest.place,
            settings = settings
        )

        val updated = latest.copy(
            playedAt = null,
            baseIncome = command.finalBaseIncome,
            totalIncome = command.finalTotalIncome,
            isFinalIncomeRecord = true,
            simpleBatchId = latest.simpleBatchId,
            updatedAt = Clock.System.now()
        )
        return repository.updateGameResult(updated)
    }
}
