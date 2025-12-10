package com.example.domain.repository

import com.example.domain.model.SpecialHourlyWage

interface SpecialHourlyWageRepository {
    suspend fun listByUser(userId: Long): List<SpecialHourlyWage>
    suspend fun findById(id: Long): SpecialHourlyWage?
    suspend fun insert(userId: Long, label: String, hourlyWage: Int): SpecialHourlyWage
    suspend fun delete(userId: Long, id: Long): Boolean
    suspend fun existsLabel(userId: Long, label: String): Boolean
    suspend fun detachFromShifts(specialWageId: Long)
}
