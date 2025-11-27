package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.Table

/**
 * ### このファイルの役割
 * - 都道府県マスター `prefectures` を表す Exposed テーブル定義です。
 * - code（都道府県コード）を主キーに、名称だけを保持するシンプルな構造にしています。
 */
object PrefecturesTable : Table("prefectures") {
    val code = varchar("code", length = 4)
    val name = varchar("name", length = 50)

    override val primaryKey: PrimaryKey = PrimaryKey(code)
}
