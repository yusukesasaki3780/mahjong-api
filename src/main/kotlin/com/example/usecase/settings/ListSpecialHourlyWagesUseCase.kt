package com.example.usecase.settings

import com.example.domain.repository.SpecialHourlyWageRepository

class ListSpecialHourlyWagesUseCase(
    private val repository: SpecialHourlyWageRepository
) {
    suspend operator fun invoke(userId: Long) = repository.listByUser(userId)
}
