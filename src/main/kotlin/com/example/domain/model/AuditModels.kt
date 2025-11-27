package com.example.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AuditContext(
    val performedBy: Long,
    val path: String,
    val ipAddress: String
)

data class AuditEntry(
    val entityType: String,
    val entityId: Long?,
    val action: String,
    val performedBy: Long,
    val performedAt: Instant,
    val beforeJson: String?,
    val afterJson: String?,
    val path: String,
    val ipAddress: String
)
