package com.example.infrastructure.db.repository

import com.example.domain.model.StoreMaster
import com.example.domain.repository.StoreMasterRepository
import com.example.infrastructure.db.tables.StoreMasterTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * ### このファイルの役割
 * - 店舗マスターを Exposed 経由で取得する Repository 実装です。
 * - DB アクセスの詳細を隠蔽し、ドメインモデル `StoreMaster` へ変換して返却します。
 */
class ExposedStoreMasterRepository : StoreMasterRepository {

    override suspend fun getAll(): List<StoreMaster> = dbQuery {
        StoreMasterTable
            .selectAll()
            .orderBy(StoreMasterTable.storeName to SortOrder.ASC)
            .map(::toStoreMaster)
    }

    override suspend fun findById(id: Long): StoreMaster? = dbQuery {
        StoreMasterTable
            .select { StoreMasterTable.id eq id }
            .singleOrNull()
            ?.let(::toStoreMaster)
    }

    private fun toStoreMaster(row: ResultRow): StoreMaster =
        StoreMaster(
            id = row[StoreMasterTable.id],
            storeName = row[StoreMasterTable.storeName],
            createdAt = row[StoreMasterTable.createdAt],
            updatedAt = row[StoreMasterTable.updatedAt]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
