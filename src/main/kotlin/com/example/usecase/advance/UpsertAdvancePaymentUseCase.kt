package com.example.usecase.advance

import com.example.domain.model.AuditContext
import com.example.domain.repository.AdvancePaymentRepository
import com.example.infrastructure.logging.AuditLogger
import java.time.YearMonth
import kotlin.math.abs

class UpsertAdvancePaymentUseCase(
    private val repository: AdvancePaymentRepository,
    private val auditLogger: AuditLogger
) {

    data class Command(
        val userId: Long,
        val yearMonth: YearMonth,
        val amount: Double,
        val auditContext: AuditContext
    )

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

    data class Result(
        val userId: Long,
        val yearMonth: YearMonth,
        val amount: Double
    )
}
