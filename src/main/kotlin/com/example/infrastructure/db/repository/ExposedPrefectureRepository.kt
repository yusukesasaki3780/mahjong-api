package com.example.infrastructure.db.repository

import com.example.domain.model.Prefecture
import com.example.domain.repository.PrefectureRepository
import com.example.infrastructure.db.tables.PrefecturesTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * ### このファイルの役割
 * - 都道府県マスター `prefectures` を取得する Repository 実装です。
 * - 取得結果をドメインモデル `Prefecture` にマッピングして返却します。
 */
class ExposedPrefectureRepository : PrefectureRepository {

    override suspend fun getAll(): List<Prefecture> = dbQuery {
        PrefecturesTable
            .selectAll()
            .orderBy(PrefecturesTable.code to SortOrder.ASC)
            .map(::toPrefecture)
    }

    private fun toPrefecture(row: ResultRow): Prefecture =
        Prefecture(
            code = row[PrefecturesTable.code],
            name = row[PrefecturesTable.name]
        )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
