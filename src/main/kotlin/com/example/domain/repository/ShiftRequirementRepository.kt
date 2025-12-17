package com.example.domain.repository

import com.example.domain.model.ShiftRequirement
import com.example.domain.model.ShiftSlotType
import kotlinx.datetime.LocalDate

interface ShiftRequirementRepository {

    suspend fun findByStoreAndDateRange(
        storeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ShiftRequirement>

    suspend fun upsert(
        storeId: Long,
        targetDate: LocalDate,
        shiftType: ShiftSlotType,
        startRequired: Int,
        endRequired: Int
    ): ShiftRequirement

    suspend fun existsBefore(storeId: Long, referenceDate: LocalDate): Boolean
}
