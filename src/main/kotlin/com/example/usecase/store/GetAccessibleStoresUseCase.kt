package com.example.usecase.store

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.StoreMaster
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.StoreMasterRepository
import com.example.domain.repository.UserRepository

/**
 * ユーザーがアクセスできる店舗一覧を取得するユースケース。
 */
class GetAccessibleStoresUseCase(
    private val userRepository: UserRepository,
    private val storeRepository: StoreMasterRepository,
    private val shiftRepository: ShiftRepository
) {

    /**
     * 管理者なら全店舗、一般ユーザーなら所属店舗＋勤務実績のある店舗だけを返す。
     */
    suspend operator fun invoke(actorId: Long): List<StoreMaster> {
        val actor = userRepository.findById(actorId) ?: throw DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "userId",
                    code = "NOT_FOUND",
                    message = "ユーザー情報を取得できません。"
                )
            )
        )

        return if (actor.isAdmin) {
            storeRepository.getAll()
        } else {
            val storeIds = mutableSetOf(actor.storeId)
            storeIds += shiftRepository.getStoreIdsForUser(actorId)
            if (storeIds.isEmpty()) {
                emptyList()
            } else {
                storeRepository.findByIds(storeIds)
            }
        }
    }
}
