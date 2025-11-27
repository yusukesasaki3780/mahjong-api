package com.example.presentation.dto

import com.example.domain.model.Prefecture
import kotlinx.serialization.Serializable

/**
 * ### このファイルの役割
 * - 都道府県マスターを JSON に変換するための DTO です。
 * - code（都道府県コード）と name（表示名）だけを切り出して返します。
 */
@Serializable
data class PrefectureResponse(
    val code: String,
    val name: String
) {
    companion object {
        fun from(model: Prefecture) = PrefectureResponse(
            code = model.code,
            name = model.name
        )
    }
}
