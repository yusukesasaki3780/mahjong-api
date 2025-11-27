package com.example.usecase

import com.example.domain.model.AuditContext
import com.example.domain.model.AuditEntry
import com.example.domain.repository.AuditRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.serialization.json.Json

object TestAuditSupport {
    val auditLogger = AuditLogger(object : AuditRepository {
        override suspend fun record(entry: AuditEntry) {
            // no-op for tests
        }
    }, Json { encodeDefaults = true })

    val auditContext = AuditContext(
        performedBy = 1L,
        path = "/test",
        ipAddress = "127.0.0.1"
    )
}
