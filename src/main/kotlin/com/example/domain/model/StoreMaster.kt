package com.example.domain.model

import kotlinx.datetime.Instant

/**
 * ### このファイルの役割
 * - 店舗マスターの 1 行を表すドメインモデルです。
 * - storeName の表記ゆれを防ぐためにマスタ化し、ユーザー登録時などで参照できるようにします。
 * - createdAt / updatedAt を保持し、将来的なメンテナンスや並び替えにも対応しやすくしています。
 */
data class StoreMaster(
    val id: Long,
    val storeName: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
