package com.example.domain.model

/**
 * ### このファイルの役割
 * - 都道府県マスターの 1 件分を表すシンプルなドメインモデルです。
 * - code（都道府県コード）と name（名称）をセットで保持し、画面からの選択肢として利用します。
 */
data class Prefecture(
    val code: String,
    val name: String
)
