package com.example.usecase.shift

import com.example.common.error.AccessDeniedException
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.Shift
import com.example.domain.model.StoreMaster
import com.example.domain.model.User
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.StoreMasterRepository
import com.example.domain.repository.UserRepository

/**
 * シフトの閲覧・作成・更新に必要なユーザー／店舗情報と権限チェックを集約するプロバイダー。
 */
class ShiftContextProvider(
    private val userRepository: UserRepository,
    private val shiftRepository: ShiftRepository,
    private val storeMasterRepository: StoreMasterRepository
) {

    /**
     * シフト新規作成時に必要な俯瞰情報を解決し、指定店舗の作成権限を検証する。
     */
    suspend fun forCreate(
        actorId: Long,
        targetUserId: Long,
        requestedStoreId: Long?
    ): ShiftCreateContext {
        val actor = requireUser(actorId, "actorId")
        val targetUser = requireUser(targetUserId, "userId")
        val viewableStores = resolveViewableStoreData(actor)
        val storeId = when {
            requestedStoreId != null -> {
                if (!actor.isAdmin) {
                    throw AccessDeniedException("storeId を指定する権限がありません。")
                }
                requestedStoreId
            }

            else -> {
                val actorId = actor.id ?: throw AccessDeniedException("アカウント情報を取得できません。")
                val targetId = targetUser.id ?: throw AccessDeniedException("対象ユーザー情報を取得できません。")
                if (actorId != targetId) {
                    throw DomainValidationException(
                        violations = listOf(
                            FieldError(
                                field = "storeId",
                                code = "REQUIRED",
                                message = "storeId を指定してください。"
                            )
                        )
                    )
                }
                actor.storeId ?: throw DomainValidationException(
                    violations = listOf(
                        FieldError(
                            field = "storeId",
                            code = "NOT_IDENTIFIED",
                            message = "店舗を特定できません。所属店舗を設定してください。"
                        )
                    )
                )
            }
        }
        if (!actor.isAdmin && viewableStores.ids.isNotEmpty() && storeId !in viewableStores.ids) {
            throw AccessDeniedException("この店舗のシフトを登録する権限がありません。")
        }
        val store = requireStore(storeId)
        return ShiftCreateContext(actor, targetUser, store)
    }

    /**
     * シフト更新時に対象データと権限を解決する。
     */
    suspend fun forUpdate(actorId: Long, shiftId: Long): ShiftUpdateContext {
        val actor = requireUser(actorId, "actorId")
        val shift = requireShift(shiftId)
        ensureStoreEditable(actor, shift.storeId)
        val targetUser = requireUser(shift.userId, "userId")
        val store = requireStore(shift.storeId)
        return ShiftUpdateContext(actor, targetUser, store, shift)
    }

    /**
     * シフト削除時に対象データと権限を解決する。
     */
    suspend fun forDelete(actorId: Long, shiftId: Long): ShiftDeleteContext {
        val actor = requireUser(actorId, "actorId")
        val shift = requireShift(shiftId)
        ensureStoreEditable(actor, shift.storeId)
        val targetUser = requireUser(shift.userId, "userId")
        val store = requireStore(shift.storeId)
        return ShiftDeleteContext(actor, targetUser, store, shift)
    }

    /**
     * 個別ユーザーのシフト閲覧に必要な情報と権限を構築する。
     */
    suspend fun forUserView(actorId: Long, targetUserId: Long): ShiftViewContext {
        val actor = requireUser(actorId, "actorId")
        val targetUser = requireUser(targetUserId, "userId")
        val actorViewableStores = resolveViewableStoreData(actor)
        val viewableUserIds = resolveViewableUserIds(actor)
        val targetId = targetUser.id ?: throw AccessDeniedException("対象ユーザー情報を取得できません。")
        if (!actor.isAdmin && targetId !in viewableUserIds) {
            throw AccessDeniedException("他ユーザーのシフトにはアクセスできません。")
        }
        val stores = resolveStoresForUser(targetUser)
        return ShiftViewContext(
            actor = actor,
            targetUsers = listOf(targetUser),
            targetStores = stores,
            requestedUser = targetUser,
            requestedStore = null,
            scope = ShiftViewContext.Scope.USER,
            editable = actor.isAdmin || actor.id == targetUser.id,
            canIncludeDeletedUsers = false,
            viewableStoreIds = actorViewableStores.ids,
            viewableUserIds = viewableUserIds
        )
    }

    /**
     * 店舗単位のシフト閲覧に必要な情報と権限を構築する。
     */
    suspend fun forStoreView(actorId: Long, storeId: Long): ShiftViewContext {
        val actor = requireUser(actorId, "actorId")
        val actorViewableStores = resolveViewableStoreData(actor)
        if (!actor.isAdmin && storeId !in actorViewableStores.ids) {
            throw AccessDeniedException("この店舗のシフトを閲覧する権限がありません。")
        }
        val store = requireStore(storeId)
        val sameStore = actor.storeId == storeId
        return ShiftViewContext(
            actor = actor,
            targetUsers = emptyList(),
            targetStores = actorViewableStores.stores,
            requestedUser = null,
            requestedStore = store,
            scope = ShiftViewContext.Scope.STORE,
            editable = actor.isAdmin && sameStore,
            canIncludeDeletedUsers = actor.isAdmin,
            viewableStoreIds = actorViewableStores.ids,
            viewableUserIds = resolveViewableUserIds(actor)
        )
    }

    private suspend fun requireUser(id: Long, field: String): User =
        userRepository.findById(id) ?: throw DomainValidationException(
            violations = listOf(
                FieldError(
                    field = field,
                    code = "NOT_FOUND",
                    message = "ユーザーが存在しません。"
                )
            )
        )

    private suspend fun requireStore(id: Long): StoreMaster =
        storeMasterRepository.findById(id)
            ?: throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "storeId",
                        code = "NOT_FOUND",
                        message = "店舗情報が見つかりません。"
                    )
                )
            )

    private suspend fun requireShift(id: Long): Shift =
        shiftRepository.findById(id)
            ?: throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "shiftId",
                        code = "NOT_FOUND",
                        message = "シフトが存在しません。"
                    )
                )
            )

    private suspend fun resolveStoresForUser(user: User): List<StoreMaster> {
        val ids = resolveMemberStoreIds(user)
        if (ids.isEmpty()) return emptyList()
        return storeMasterRepository.findByIds(ids)
    }

    private suspend fun resolveMemberStoreIds(user: User): LinkedHashSet<Long> {
        val result = linkedSetOf<Long>()
        user.storeId?.let(result::add)
        val userId = user.id
        if (userId != null) {
            result += shiftRepository.getStoreIdsForUser(userId)
        }
        return result
    }

    private suspend fun resolveViewableStoreData(actor: User): ViewableStoreData {
        return if (actor.isAdmin) {
            val all = storeMasterRepository.getAll()
            ViewableStoreData(all.map { it.id }.toSet(), all)
        } else {
            val ids = resolveMemberStoreIds(actor)
            val stores = if (ids.isEmpty()) emptyList() else storeMasterRepository.findByIds(ids)
            ViewableStoreData(ids, stores)
        }
    }

    private fun resolveViewableUserIds(actor: User): Set<Long> =
        if (actor.isAdmin) emptySet() else setOfNotNull(actor.id)

    private data class ViewableStoreData(
        val ids: Set<Long>,
        val stores: List<StoreMaster>
    )

    private fun ensureSameUser(actor: User, target: User) {
        val actorId = actor.id ?: throw AccessDeniedException("アカウント情報を取得できません。")
        val targetId = target.id ?: throw AccessDeniedException("対象ユーザー情報を取得できません。")
        if (actorId != targetId) {
            throw AccessDeniedException("他ユーザーのシフトにはアクセスできません。")
        }
    }

    private suspend fun ensureStoreEditable(actor: User, storeId: Long) {
        if (actor.isAdmin) return
        val viewableStores = resolveViewableStoreData(actor)
        if (storeId !in viewableStores.ids) {
            throw AccessDeniedException("この店舗のシフトを編集する権限がありません。")
        }
    }
}
