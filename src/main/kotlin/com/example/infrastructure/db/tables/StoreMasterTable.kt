package com.example.infrastructure.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * ### このファイルの役割
 * - 店舗マスターテーブル `store_master` の Exposed 定義です。
 * - 店舗一覧 API やユーザー登録時の参照に利用されるため、ID・名称・作成/更新日時を保持します。
 */
object StoreMasterTable : Table("store_master") {
    val id = long("id").autoIncrement()
    val storeName = varchar("store_name", length = 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
