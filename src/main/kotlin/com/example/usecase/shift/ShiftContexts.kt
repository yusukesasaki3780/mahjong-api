package com.example.usecase.shift

import com.example.domain.model.Shift
import com.example.domain.model.StoreMaster
import com.example.domain.model.User

data class ShiftCreateContext(
    val actor: User,
    val targetUser: User,
    val targetStore: StoreMaster
)

data class ShiftUpdateContext(
    val actor: User,
    val targetUser: User,
    val targetStore: StoreMaster,
    val shift: Shift
)

data class ShiftDeleteContext(
    val actor: User,
    val targetUser: User,
    val targetStore: StoreMaster,
    val shift: Shift
)

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
