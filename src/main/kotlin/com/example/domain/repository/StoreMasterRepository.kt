package com.example.domain.repository

import com.example.domain.model.StoreMaster

/**
 * ### このファイルの役割
 * - 店舗マスターへのアクセスを抽象化する Repository インターフェイスです。
 * - 取得系の操作のみ提供し、UI 層からは永続化の詳細を意識せずに利用できます。
 */
interface StoreMasterRepository {
    suspend fun getAll(): List<StoreMaster>
}
