package com.example.usecase.shift

import com.example.domain.model.Shift
import com.example.domain.model.StoreMaster
import com.example.domain.model.User

/**
 * シフト作成時に必要なアクター／対象ユーザー／店舗をまとめたコンテキスト。
 */
data class ShiftCreateContext(
    val actor: User,
    val targetUser: User,
    val targetStore: StoreMaster
)

/**
 * シフト更新時の権限判定や監査に使う情報セット。
 */
data class ShiftUpdateContext(
    val actor: User,
    val targetUser: User,
    val targetStore: StoreMaster,
    val shift: Shift
)

/**
 * シフト削除時の権限判定や監査に使う情報セット。
 */
data class ShiftDeleteContext(
    val actor: User,
    val targetUser: User,
    val targetStore: StoreMaster,
    val shift: Shift
)

/**
 * シフトの閲覧処理に必要なユーザー／店舗や編集可否を保持するコンテキスト。
 */
data class ShiftViewContext(
    val actor: User,
    val targetUsers: List<User>,
    val targetStores: List<StoreMaster>,
    val requestedUser: User?,
    val requestedStore: StoreMaster?,
    val scope: Scope,
    val editable: Boolean,
    val canIncludeDeletedUsers: Boolean,
    val viewableStoreIds: Set<Long>,
    val viewableUserIds: Set<Long>
) {
    val primaryUser: User? get() = requestedUser ?: targetUsers.firstOrNull()
    val primaryStore: StoreMaster? get() = requestedStore ?: targetStores.firstOrNull()

    enum class Scope {
        USER,
        STORE
    }
}
