package com.example.usecase.shift

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.ShiftRequirement
import com.example.domain.model.ShiftSlotType
import com.example.domain.model.User
import com.example.domain.repository.ShiftRequirementRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class UpsertShiftRequirementUseCase(
    private val shiftRequirementRepository: ShiftRequirementRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    data class Command(
        val actor: User,
        val storeId: Long,
        val targetDate: LocalDate,
        val shiftType: ShiftSlotType,
        val startRequired: Int,
        val endRequired: Int
    )

    suspend operator fun invoke(command: Command): ShiftRequirement {
        ensureAdmin(command.actor)
        ensureStore(command.actor, command.storeId)
        ensureFutureOrToday(command.targetDate)
        ensureCounts(command.startRequired, command.endRequired)

        return shiftRequirementRepository.upsert(
            storeId = command.storeId,
            targetDate = command.targetDate,
            shiftType = command.shiftType,
            startRequired = command.startRequired,
            endRequired = command.endRequired
        )
    }

    private fun ensureAdmin(actor: User) {
        if (!actor.isAdmin) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "actor",
                        code = "FORBIDDEN",
                        message = "管理者のみが必要人数を編集できます。"
                    )
                ),
                message = "管理者権限が必要です。"
            )
        }
    }

    private fun ensureStore(actor: User, storeId: Long) {
        if (actor.storeId != storeId) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "storeId",
                        code = "STORE_MISMATCH",
                        message = "他店舗の必要人数は編集できません。"
                    )
                )
            )
        }
    }

    private fun ensureFutureOrToday(targetDate: LocalDate) {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        if (targetDate < today) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "targetDate",
                        code = "PAST_DATE",
                        message = "過去日の必要人数は編集できません。"
                    )
                )
            )
        }
    }

    private fun ensureCounts(startRequired: Int, endRequired: Int) {
        if (startRequired < 0 || endRequired < 0) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "startRequired/endRequired",
                        code = "NEGATIVE_VALUE",
                        message = "必要人数は 0 以上を指定してください。"
                    )
                )
            )
        }
        if (startRequired > MAX_REQUIRED || endRequired > MAX_REQUIRED) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "startRequired/endRequired",
                        code = "REQUIRED_TOO_LARGE",
                        message = "必要人数は最大 $MAX_REQUIRED 名まで設定できます。"
                    )
                )
            )
        }
    }

    companion object {
        private const val MAX_REQUIRED = 20
    }
}
