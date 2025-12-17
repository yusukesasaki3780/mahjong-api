package com.example.infrastructure.db.repository

import com.example.domain.model.User
import com.example.infrastructure.db.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow

internal fun ResultRow.toUserModel(): User =
    User(
        id = this[UsersTable.userId],
        name = this[UsersTable.name],
        nickname = this[UsersTable.nickname],
        storeId = this[UsersTable.storeId],
        storeName = this[UsersTable.storeName],
        prefectureCode = this[UsersTable.prefectureCode].trim(),
        email = this[UsersTable.email],
        zooId = this[UsersTable.zooId],
        isAdmin = this[UsersTable.isAdmin],
        isDeleted = this[UsersTable.isDeleted],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )
