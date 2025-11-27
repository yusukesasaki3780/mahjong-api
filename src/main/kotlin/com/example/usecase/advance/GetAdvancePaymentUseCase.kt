package com.example.usecase.advance

import com.example.domain.model.AdvancePayment
import com.example.domain.repository.AdvancePaymentRepository
import java.time.YearMonth

class GetAdvancePaymentUseCase(
    private val repository: AdvancePaymentRepository
) {

    data class Result(
        val userId: Long,
        val yearMonth: YearMonth,
        val amount: Double
    )

    suspend operator fun invoke(userId: Long, yearMonth: YearMonth): Result {
        val payment = repository.findByUserIdAndYearMonth(userId, yearMonth)
        return Result(
            userId = userId,
            yearMonth = yearMonth,
            amount = payment?.amount ?: 0.0
        )
    }
}
