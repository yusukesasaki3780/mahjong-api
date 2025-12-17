package com.example.usecase.settings

import com.example.domain.repository.SpecialHourlyWageRepository

/**
 * 特別時給設定を削除するユースケース。
 */
class DeleteSpecialHourlyWageUseCase(
    private val repository: SpecialHourlyWageRepository
) {
    /**
     * 所有者チェックを行い、該当時給をシフトから切り離して削除する。
     */
    suspend operator fun invoke(userId: Long, id: Long): Boolean {
        val target = repository.findById(id) ?: return false
        if (target.userId != userId) return false
        repository.detachFromShifts(id)
        return repository.delete(userId, id)
    }
}
