package com.example.presentation.dto

import com.example.serialization.YearMonthSerializer
import com.example.usecase.advance.GetAdvancePaymentUseCase
import com.example.usecase.advance.UpsertAdvancePaymentUseCase
import kotlinx.serialization.Serializable
import java.time.YearMonth

@Serializable
data class AdvancePaymentResponse(
    val userId: Long,
    val yearMonth: String,
    val amount: Double
) {
    companion object {
        fun from(result: GetAdvancePaymentUseCase.Result) = AdvancePaymentResponse(
            userId = result.userId,
            yearMonth = result.yearMonth.toString(),
            amount = result.amount
        )

        fun from(result: UpsertAdvancePaymentUseCase.Result) = AdvancePaymentResponse(
            userId = result.userId,
            yearMonth = result.yearMonth.toString(),
            amount = result.amount
        )
    }
}

@Serializable
data class UpsertAdvancePaymentRequest(
    val amount: Double
)
