package com.example.usecase.shift

import com.example.common.error.AccessDeniedException

/**
 * シフト機能に関する権限判定をまとめるサービス。
 * すべてのメソッドは事前に構築済みのコンテキストのみを受け取り、
 * ルートやユースケースが生のリクエスト値に依存しないようにする。
 */
class ShiftPermissionService {

    fun ensureCanCreate(context: ShiftCreateContext) {
        val actorId = context.actor.id ?: throw AccessDeniedException("アカウント情報を取得できません。")
        val targetId = context.targetUser.id ?: throw AccessDeniedException("対象ユーザー情報を取得できません。")
        if (!context.actor.isAdmin && actorId != targetId) {
            throw AccessDeniedException("他ユーザーのシフトを登録する権限がありません。")
        }
    }

    fun ensureCanUpdate(context: ShiftUpdateContext) {
        val actorId = context.actor.id ?: throw AccessDeniedException("アカウント情報を取得できません。")
        if (!context.actor.isAdmin && actorId != context.shift.userId) {
            throw AccessDeniedException("このシフトを更新する権限がありません。")
        }
    }

    fun ensureCanDelete(context: ShiftDeleteContext) {
        val actorId = context.actor.id ?: throw AccessDeniedException("アカウント情報を取得できません。")
        if (!context.actor.isAdmin && actorId != context.shift.userId) {
            throw AccessDeniedException("このシフトを削除する権限がありません。")
        }
    }

    fun ensureCanView(context: ShiftViewContext) {
        when (context.scope) {
            ShiftViewContext.Scope.USER -> ensureUserView(context)
            ShiftViewContext.Scope.STORE -> ensureStoreView(context)
        }
    }

    private fun ensureUserView(context: ShiftViewContext) {
        val targetId = context.primaryUser?.id ?: throw AccessDeniedException("対象ユーザー情報を取得できません。")
        if (!context.actor.isAdmin && targetId !in context.viewableUserIds) {
            throw AccessDeniedException("他ユーザーのシフトにはアクセスできません。")
        }
    }

    private fun ensureStoreView(context: ShiftViewContext) {
        val storeId = context.primaryStore?.id ?: throw AccessDeniedException("店舗情報を取得できません。")
        if (context.actor.isAdmin) return
        if (storeId !in context.viewableStoreIds) {
            throw AccessDeniedException("この店舗のシフトを閲覧する権限がありません。")
        }
    }
}
