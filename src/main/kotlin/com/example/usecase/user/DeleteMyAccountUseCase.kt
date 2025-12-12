package com.example.usecase.user

/**
 * ### このファイルの役割
 * - ログイン中ユーザー自身のアカウントを削除するためのユースケースです。
 * - 関連する履歴や精算情報を可能な限り削除したうえでユーザー本体を削除します。
 */

import com.example.domain.model.AuditContext
import com.example.domain.repository.AdvancePaymentRepository
import com.example.domain.repository.GameResultRepository
import com.example.domain.repository.RefreshTokenRepository
import com.example.domain.repository.ShiftRepository

class DeleteMyAccountUseCase(
    private val gameResultRepository: GameResultRepository,
    private val shiftRepository: ShiftRepository,
    private val advancePaymentRepository: AdvancePaymentRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val deleteUserUseCase: DeleteUserUseCase
) {

    suspend operator fun invoke(userId: Long, auditContext: AuditContext): Boolean {
        refreshTokenRepository.deleteAllForUser(userId)
        advancePaymentRepository.deleteAllForUser(userId)
        gameResultRepository.deleteAllForUser(userId)
        shiftRepository.deleteAllForUser(userId)
        return deleteUserUseCase(userId, auditContext)
    }
}

