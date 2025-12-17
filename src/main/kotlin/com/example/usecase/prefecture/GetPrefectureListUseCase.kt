package com.example.usecase.prefecture

import com.example.domain.model.Prefecture
import com.example.domain.repository.PrefectureRepository

/**
 * 都道府県マスター一覧を返すユースケース。
 */
class GetPrefectureListUseCase(
    private val repository: PrefectureRepository
) {
    /**
     * 都道府県マスターデータをすべて取得する。
     */
    suspend operator fun invoke(): List<Prefecture> = repository.getAll()
}
