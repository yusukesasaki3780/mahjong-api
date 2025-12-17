package com.example.usecase.game

/**
 * ### このファイルの役割
 * - 「まとめて簡単入力モード」を開始する際に必要な検証とレスポンス生成を担当します。
 * - 店舗 ID の存在チェックや日付のデフォルト補完を行い、front に返すための UUID を発行します。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.repository.StoreMasterRepository
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 簡易入力バッチ開始時の前提チェックと初期レスポンス生成を行うユースケース。
 */
class StartSimpleBatchUseCase(
    private val storeMasterRepository: StoreMasterRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    /**
     * 簡単入力を開始する際に指定する店舗や日付などの情報。
     */
    data class Command(
        val userId: Long,
        val storeId: Long,
        val playedAt: LocalDate?
    )

    /**
     * バッチ開始時にフロントへ返す UUID や店舗情報をまとめた結果。
     */
    data class Result(
        val simpleBatchId: UUID,
        val storeId: Long,
        val storeName: String,
        val playedAt: LocalDate
    )

    /**
     * 店舗存在を確認し、日付を補完して新しいバッチ ID を発行する。
     */
    suspend operator fun invoke(command: Command): Result {
        val store = storeMasterRepository.findById(command.storeId)
            ?: throw DomainValidationException(
                listOf(
                    FieldError(
                        field = "storeId",
                        code = "STORE_NOT_FOUND",
                        message = "指定した店舗が存在しません。"
                    )
                )
            )
        val playedAt = command.playedAt ?: today()
        if (playedAt < MIN_PLAYED_DATE) {
            throw DomainValidationException(
                listOf(
                    FieldError(
                        field = "playedAt",
                        code = "INVALID_PLAYED_AT",
                        message = "playedAt は 1970-01-01 以降の日付を指定してください。"
                    )
                )
            )
        }

        return Result(
            simpleBatchId = UUID.randomUUID(),
            storeId = store.id,
            storeName = store.storeName,
            playedAt = playedAt
        )
    }

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(timeZone).date
}
