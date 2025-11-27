package com.example.domain.model

import kotlinx.serialization.Serializable

/**
 * ゲーム種別（四麻 or 三麻）を示す列挙。
 */
@Serializable
enum class GameType {
    YONMA,
    SANMA
}
