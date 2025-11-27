package com.example.domain.repository

import com.example.domain.model.AuditEntry

interface AuditRepository {
    suspend fun record(entry: AuditEntry)
}
