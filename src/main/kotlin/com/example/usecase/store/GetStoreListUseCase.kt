package com.example.usecase.store

import com.example.domain.model.StoreMaster
import com.example.domain.repository.StoreMasterRepository

/**
 * ### このファイルの役割
 * - 店舗マスター一覧を取得するだけのシンプルなユースケースです。
 * - 画面側からは Repository の存在を意識せず、`invoke()` で店舗リストを得られるようにします。
 */
class GetStoreListUseCase(
    private val repository: StoreMasterRepository
) {
    suspend operator fun invoke(): List<StoreMaster> = repository.getAll()
}
