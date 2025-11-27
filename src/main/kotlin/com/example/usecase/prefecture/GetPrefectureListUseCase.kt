package com.example.usecase.prefecture

import com.example.domain.model.Prefecture
import com.example.domain.repository.PrefectureRepository

/**
 * ### このファイルの役割
 * - 都道府県マスターの一覧を取得するユースケースです。
 * - フロントエンドが 47 件の選択肢を取得したい場面で呼び出されます。
 */
class GetPrefectureListUseCase(
    private val repository: PrefectureRepository
) {
    suspend operator fun invoke(): List<Prefecture> = repository.getAll()
}
