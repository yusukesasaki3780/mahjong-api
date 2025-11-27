package com.example.domain.repository

import com.example.domain.model.AdvancePayment
import java.time.YearMonth

interface AdvancePaymentRepository {
    suspend fun findByUserIdAndYearMonth(userId: Long, yearMonth: YearMonth): AdvancePayment?
    suspend fun upsert(userId: Long, yearMonth: YearMonth, amount: Double): AdvancePayment
}
