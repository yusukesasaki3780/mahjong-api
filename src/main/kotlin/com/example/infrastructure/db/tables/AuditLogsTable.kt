package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AuditLogsTable : Table("audit_logs") {
    val id = long("id").autoIncrement()
    val entityType = varchar("entity_type", length = 64)
    val entityId = long("entity_id").nullable()
    val action = varchar("action", length = 64)
    val performedBy = long("performed_by")
    val performedAt = timestamp("performed_at")
    val before = text("before").nullable()
    val after = text("after").nullable()
    val path = varchar("path", length = 255)
    val ipAddress = varchar("ip_address", length = 64)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
