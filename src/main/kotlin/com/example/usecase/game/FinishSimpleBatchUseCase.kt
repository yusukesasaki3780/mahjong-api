package com.example.usecase.game

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.GameResult
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.GameSettingsRepository
import java.util.UUID
import kotlinx.datetime.Clock

/**
 * 簡易入力バッチの終了時に最終収支レコードを確定させるユースケース。
 */
class FinishSimpleBatchUseCase(
    private val repository: GameResultRepository,
    private val settingsRepository: GameSettingsRepository
) {

    /**
     * バッチ終了時に必要なユーザー・バッチ ID と最終金額をまとめるコマンド。
     */
    data class Command(
        val userId: Long,
        val simpleBatchId: UUID,
        val finalBaseIncome: Long,
        val finalTotalIncome: Long
    )

    /**
     * バッチの最新行を取得して最終収支フラグと金額を確定させる。
     */
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
