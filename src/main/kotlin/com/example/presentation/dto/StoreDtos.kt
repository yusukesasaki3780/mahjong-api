package com.example.presentation.dto

import com.example.domain.model.StoreMaster
import kotlinx.serialization.Serializable

/**
 * ### このファイルの役割
 * - 店舗マスターを API レスポンスとして返すための DTO を定義します。
 * - ドメインモデルから必要な最小限の項目だけを抽出してフロントエンドへ渡します。
 */
@Serializable
data class StoreResponse(
    val id: Long,
    val storeName: String
) {
    companion object {
        fun from(model: StoreMaster) = StoreResponse(
            id = model.id,
            storeName = model.storeName
        )
    }
}
