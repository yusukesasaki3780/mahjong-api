package com.example.usecase.settings

import com.example.domain.repository.SpecialHourlyWageRepository

/**
 * 特別時給設定の一覧を取得するユースケース。
 */
class ListSpecialHourlyWagesUseCase(
    private val repository: SpecialHourlyWageRepository
) {
    /**
     * 指定ユーザーに紐づく特別時給設定をすべて取得する。
     */
    suspend operator fun invoke(userId: Long) = repository.listByUser(userId)
}
