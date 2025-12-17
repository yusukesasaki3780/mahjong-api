package com.example.usecase.user

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.repository.UserRepository

/**
 * 管理者によるユーザー削除ユースケース。
 * - 自身や他店舗のメンバーは削除できない。
 * - 実際の削除処理と監査記録は DeleteUserUseCase に委譲する。
 */
/**
 * 管理者が一般ユーザーを削除するユースケース。
 */
class AdminDeleteUserUseCase(
    private val userRepository: UserRepository,
    private val deleteUserUseCase: DeleteUserUseCase
) {

    /**
     * 自分自身や異なる店舗・管理者を対象外とし、条件を満たすユーザーを削除する。
     */
    suspend operator fun invoke(
        adminId: Long,
        adminStoreId: Long,
        targetUserId: Long,
        auditContext: AuditContext
    ): Boolean {
        if (adminId == targetUserId) {
            throw validationError("userId", "SELF_DELETE_FORBIDDEN", "自分自身を削除することはできません。")
        }

        val target = userRepository.findById(targetUserId) ?: return false
        if (target.isAdmin) {
            throw validationError("userId", "ADMIN_DELETE_FORBIDDEN", "管理者アカウントは削除できません。")
        }
        if (target.storeId != adminStoreId) {
            throw validationError("userId", "DIFFERENT_STORE", "他店舗のメンバーは削除できません。")
        }
        if (target.isDeleted) {
            return false
        }

        return deleteUserUseCase(targetUserId, auditContext)
    }

    private fun validationError(field: String, code: String, message: String): DomainValidationException =
        DomainValidationException(
            violations = listOf(FieldError(field = field, code = code, message = message)),
            message = message
        )
}
