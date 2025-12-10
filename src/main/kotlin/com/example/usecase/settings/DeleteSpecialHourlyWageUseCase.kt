package com.example.usecase.settings

import com.example.domain.repository.SpecialHourlyWageRepository

class DeleteSpecialHourlyWageUseCase(
    private val repository: SpecialHourlyWageRepository
) {
    suspend operator fun invoke(userId: Long, id: Long): Boolean {
        val target = repository.findById(id) ?: return false
        if (target.userId != userId) return false
        repository.detachFromShifts(id)
        return repository.delete(userId, id)
    }
}
