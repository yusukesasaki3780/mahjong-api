package com.example.usecase.user

/**
 * ### このファイルの役割
 * - ログイン中ユーザー自身のアカウントを削除するためのユースケースです。
 * - 関連する履歴や精算情報を可能な限り削除したうえでユーザー本体を削除します。
 */

import com.example.domain.model.AuditContext
import com.example.domain.repository.RefreshTokenRepository

/**
 * ログイン中のユーザー自身がアカウント削除するユースケース。
 */
class DeleteMyAccountUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val deleteUserUseCase: DeleteUserUseCase
) {

    /**
     * ユーザーのリフレッシュトークンをすべて削除した上で、ユーザー削除ユースケースを呼び出す。
     */
    suspend operator fun invoke(userId: Long, auditContext: AuditContext): Boolean {
        refreshTokenRepository.deleteAllForUser(userId)
        return deleteUserUseCase(userId, auditContext)
    }
}
