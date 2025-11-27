package com.example.infrastructure.db.repository

import com.example.domain.model.AuditEntry
import com.example.domain.repository.AuditRepository
import com.example.infrastructure.db.tables.AuditLogsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ExposedAuditRepository : AuditRepository {

    override suspend fun record(entry: AuditEntry) {
        dbQuery {
            AuditLogsTable.insert { row ->
                row[entityType] = entry.entityType
                row[entityId] = entry.entityId
                row[action] = entry.action
                row[performedBy] = entry.performedBy
                row[performedAt] = entry.performedAt
                row[before] = entry.beforeJson
                row[after] = entry.afterJson
                row[path] = entry.path
                row[ipAddress] = entry.ipAddress
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
