package com.example.usecase.advance

import com.example.domain.model.AuditContext
import com.example.domain.repository.AdvancePaymentRepository
import com.example.infrastructure.logging.AuditLogger
import java.time.YearMonth
import kotlin.math.abs

/**
 * 利用者の立替金を新規作成または更新するユースケース。
 */
class UpsertAdvancePaymentUseCase(
    private val repository: AdvancePaymentRepository,
    private val auditLogger: AuditLogger
) {

    /**
     * 立替金の対象ユーザー・年月・金額・監査情報をまとめた入力 DTO。
     */
    data class Command(
        val userId: Long,
        val yearMonth: YearMonth,
        val amount: Double,
        val auditContext: AuditContext
    )

    /**
     * 受け取った金額を絶対値に正規化し、立替金を upsert して監査ログを記録する。
     */
    suspend operator fun invoke(command: Command): Result {
        val normalizedAmount = abs(command.amount)
        val before = repository.findByUserIdAndYearMonth(command.userId, command.yearMonth)
        val updated = repository.upsert(command.userId, command.yearMonth, normalizedAmount)
        auditLogger.log(
            entityType = "ADVANCE_PAYMENT",
            entityId = command.userId,
            action = "UPSERT",
            context = command.auditContext,
            before = before,
            after = updated
        )
        return Result(
            userId = updated.userId,
            yearMonth = updated.yearMonth,
            amount = updated.amount
        )
    }

    /**
     * upsert 後の立替金レコードをシンプルに返すための DTO。
     */
    data class Result(
        val userId: Long,
        val yearMonth: YearMonth,
        val amount: Double
    )
}
