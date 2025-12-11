package com.example.presentation.dto

import com.example.common.serialization.UUIDSerializer
import com.example.domain.model.GameResult
import com.example.domain.model.GameType
import com.example.usecase.game.GetUserStatsUseCase
import com.example.usecase.game.StartSimpleBatchUseCase
import java.util.UUID
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * ���ъ֘A�� DTO�B
 */
@Serializable
data class UpsertGameResultRequest(
    val gameType: GameType,
    val playedAt: LocalDate,
    val place: Int,
    val baseIncome: Long,
    val tipCount: Int,
    val tipIncome: Long,
    val otherIncome: Long = 0,
    val totalIncome: Long,
    val note: String? = null,
    val createdAt: Instant? = null,
    val storeId: Long? = null,
    @Serializable(with = UUIDSerializer::class)
    val simpleBatchId: UUID? = null
)

/**
 * �����X�V���N�G�X�g�i���ׂĔC�Ӎ��ځj�B
 */
@Serializable
data class PatchGameResultRequest(
    val gameType: GameType? = null,
    val playedAt: LocalDate? = null,
    val place: Int? = null,
    val baseIncome: Long? = null,
    val tipCount: Int? = null,
    val tipIncome: Long? = null,
    val otherIncome: Long? = null,
    val totalIncome: Long? = null,
    val note: String? = null,
    val storeId: Long? = null,
    @Serializable(with = UUIDSerializer::class)
    val simpleBatchId: UUID? = null
)

@Serializable
data class GameResultResponse(
    val id: Long,
    val gameType: GameType,
    val playedAt: String?,
    val place: Int,
    val baseIncome: Long,
    val tipCount: Int,
    val tipIncome: Long,
    val otherIncome: Long,
    val totalIncome: Long,
    val note: String?,
    val storeId: Long?,
    val storeName: String?,
    val isFinalIncomeRecord: Boolean,
    val simpleBatchId: String?
) {
    companion object {
        fun from(result: GameResult): GameResultResponse {
            val localDate = result.playedAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
            return GameResultResponse(
                id = result.id!!,
                gameType = result.gameType,
                playedAt = localDate?.toString(),
                place = result.place,
                baseIncome = result.baseIncome,
                tipCount = result.tipCount,
                tipIncome = result.tipIncome,
                otherIncome = result.otherIncome,
                totalIncome = result.totalIncome,
                note = result.note,
                storeId = result.storeId,
                storeName = result.storeName,
                isFinalIncomeRecord = result.isFinalIncomeRecord,
                simpleBatchId = result.simpleBatchId?.toString()
            )
        }
    }
}

@Serializable
data class SimpleBatchStartRequest(
    val storeId: Long,
    val playedAt: LocalDate? = null
)

@Serializable
data class SimpleBatchStartResponse(
    val simpleBatchId: String,
    val storeId: Long,
    val storeName: String,
    val playedAt: String?
) {
    companion object {
        fun from(result: StartSimpleBatchUseCase.Result) = SimpleBatchStartResponse(
            simpleBatchId = result.simpleBatchId.toString(),
            storeId = result.storeId,
            storeName = result.storeName,
            playedAt = result.playedAt.toString()
        )
    }
}

@Serializable
data class SimpleBatchFinishRequest(
    val finalBaseIncome: Long,
    val finalTotalIncome: Long
)

@Serializable
data class SimpleBatchDeleteResponse(
    val deletedCount: Int
)

@Serializable
data class UserStatsResponse(
    val userId: Long,
    val averagePlace: Double?,
    val totalGames: Int,
    val totalIncome: Long,
    val results: List<GameResultResponse>
) {
    companion object {
        fun from(result: GetUserStatsUseCase.Result) = UserStatsResponse(
            userId = result.userId,
            averagePlace = result.averagePlace,
            totalGames = result.totalGames,
            totalIncome = result.totalIncome,
            results = result.results.map(GameResultResponse::from)
        )
    }
}
