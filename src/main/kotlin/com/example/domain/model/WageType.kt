package com.example.domain.model

import kotlinx.serialization.Serializable

/**
 * 給与体系（時給 or 固定給）を表す列挙。
 */
@Serializable
enum class WageType {
    HOURLY,
    FIXED
}
