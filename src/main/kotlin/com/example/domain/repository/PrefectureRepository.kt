package com.example.domain.repository

import com.example.domain.model.Prefecture

/**
 * ### このファイルの役割
 * - 都道府県マスターを取得するための Repository インターフェイスです。
 * - データソース（Exposed, キャッシュなど）を隠蔽し、ユースケースからはシンプルに一覧取得できます。
 */
interface PrefectureRepository {
    suspend fun getAll(): List<Prefecture>
}
