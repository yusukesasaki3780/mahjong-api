package com.example.usecase.store

import com.example.domain.model.StoreMaster
import com.example.domain.repository.StoreMasterRepository

/**
 * 店舗マスター一覧を取得するだけのシンプルなユースケース。
 */
class GetStoreListUseCase(
    private val repository: StoreMasterRepository
) {
    /**
     * 登録済みの店舗マスターをすべて取得する。
     */
    suspend operator fun invoke(): List<StoreMaster> = repository.getAll()
}
