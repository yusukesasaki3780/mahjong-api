package com.example.usecase.advance

import com.example.domain.repository.AdvancePaymentRepository
import java.time.YearMonth

/**
 * 指定ユーザーと年月の立替金額を取得するユースケース。
 */
class GetAdvancePaymentUseCase(
    private val repository: AdvancePaymentRepository
) {

    /**
     * ユーザー ID・年月・立替金額を束ねたレスポンス DTO。
     */
    data class Result(
        val userId: Long,
        val yearMonth: YearMonth,
        val amount: Double
    )

    /**
     * 対象ユーザーと年月の立替金レコードを検索し、存在しなければ金額 0 として返す。
     */
    suspend operator fun invoke(userId: Long, yearMonth: YearMonth): Result {
        val payment = repository.findByUserIdAndYearMonth(userId, yearMonth)
        return Result(
            userId = userId,
            yearMonth = yearMonth,
            amount = payment?.amount ?: 0.0
        )
    }
}
