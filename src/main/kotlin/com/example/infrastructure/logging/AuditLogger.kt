package com.example.infrastructure.logging

import com.example.domain.model.AuditContext
import com.example.domain.model.AuditEntry
import com.example.domain.repository.AuditRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuditLogger(
    private val repository: AuditRepository,
    private val json: Json
) {

    @PublishedApi
    internal val auditRepository: AuditRepository = repository

    @PublishedApi
    internal val auditJson: Json = json

    suspend inline fun <reified T> log(
        entityType: String,
        entityId: Long?,
        action: String,
        context: AuditContext,
        before: T?,
        after: T?
    ) {
        val entry = AuditEntry(
            entityType = entityType,
            entityId = entityId,
            action = action,
            performedBy = context.performedBy,
            performedAt = Clock.System.now(),
            beforeJson = before?.let { auditJson.encodeToString(it) },
            afterJson = after?.let { auditJson.encodeToString(it) },
            path = context.path,
            ipAddress = context.ipAddress
        )
        auditRepository.record(entry)
    }
}
