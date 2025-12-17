package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - シフトを全更新するときのユースケースで、休憩や勤務時間の複雑な検証を担当します。
 * - 更新後のデータを作り直して Repository に渡し、その内容を監査ログにも残します。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.SpecialHourlyWageRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.valiktor.functions.isLessThan
import org.valiktor.validate

/**
 * 既存シフトを全更新する際のバリデーションと監査記録を担うユースケース。
 */
class EditShiftUseCase(
    private val repository: ShiftRepository,
    private val specialHourlyWageRepository: SpecialHourlyWageRepository,
    private val auditLogger: AuditLogger,
    private val shiftNotificationService: ShiftNotificationService,
    private val contextProvider: ShiftContextProvider,
    private val permissionService: ShiftPermissionService
) {

    /**
     * シフト更新で受け取る全フィールドをまとめたコマンド。
     */
    data class Command(
        val actorId: Long,
        val shiftId: Long,
        val workDate: LocalDate,
        val startTime: Instant,
        val endTime: Instant,
        val memo: String?,
        val breaks: List<BreakCommand>,
        val createdAt: Instant,
        val specialHourlyWageId: Long? = null
    )

    /**
     * 編集時に送られる休憩時間帯を示すコマンド。
     */
    data class BreakCommand(
        val breakStart: Instant,
        val breakEnd: Instant
    )

    /**
     * 指定シフトの内容を整合性チェックしたうえで更新し、監査ログと通知を発行する。
     */
    suspend operator fun invoke(command: Command, auditContext: AuditContext): Shift {
        val context = contextProvider.forUpdate(command.actorId, command.shiftId)
        permissionService.ensureCanUpdate(context)
        val targetUserId = context.targetUser.id ?: error("Target user missing id.")
        command.validate(targetUserId)

        val before = context.shift
        val specialWage = command.specialHourlyWageId?.let { requireSpecialWage(targetUserId, it) }

        val shift = Shift(
            id = command.shiftId,
            userId = targetUserId,
            storeId = context.targetStore.id,
            workDate = command.workDate,
            startTime = command.startTime,
            endTime = command.endTime,
            memo = command.memo,
            specialHourlyWage = specialWage,
            breaks = command.breaks.map {
                ShiftBreak(
                    id = null,
                    shiftId = command.shiftId,
                    breakStart = it.breakStart,
                    breakEnd = it.breakEnd
                )
            },
            createdAt = command.createdAt,
            updatedAt = Clock.System.now()
        )
        val result = repository.updateShift(shift)

        shiftNotificationService.notifyUpdated(
            actorId = auditContext.performedBy,
            targetUserId = targetUserId,
            shift = result
        )

        auditLogger.log(
            entityType = "SHIFT",
            entityId = command.shiftId,
            action = "PUT",
            context = auditContext,
            before = before,
            after = result
        )
        return result
    }

    private suspend fun Command.validate(targetUserId: Long) {
        validate(this) {
            validate(Command::startTime).isLessThan(endTime)
        }

        val violations = mutableListOf<FieldError>()
        breaks.forEachIndexed { index, br ->
            if (br.breakStart >= br.breakEnd) {
                violations += FieldError(
                    field = "breaks[$index]",
                    code = "INVALID_BREAK_RANGE",
                    message = "breakEnd must be after breakStart"
                )
            }
            if (br.breakStart < startTime) {
                violations += FieldError(
                    field = "breaks[$index].breakStart",
                    code = "BREAK_BEFORE_SHIFT",
                    message = "breakStart must not be before shift start"
                )
            }
            if (br.breakEnd > endTime) {
                violations += FieldError(
                    field = "breaks[$index].breakEnd",
                    code = "BREAK_AFTER_SHIFT",
                    message = "breakEnd must not exceed shift end"
                )
            }
        }

        val sorted = breaks.sortedBy { it.breakStart }
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val current = sorted[i]
            if (current.breakStart < prev.breakEnd) {
                violations += FieldError(
                    field = "breaks[$i]",
                    code = "BREAK_OVERLAP",
                    message = "休憩時間が互いに重複しています"
                )
                break
            }
        }

        val overlap = repository.getShiftsOnDate(targetUserId, workDate)
            .filter { it.id != shiftId }
            .any { shift -> ShiftOverlapChecker.overlaps(shift.startTime, shift.endTime, startTime, endTime) }
        if (overlap) {
            violations += overlapError()
        }

        if (violations.isNotEmpty()) {
            throw DomainValidationException(violations)
        }
    }

    private suspend fun requireSpecialWage(userId: Long, specialWageId: Long) =
        specialHourlyWageRepository.findById(specialWageId)?.takeIf { it.userId == userId }
            ?: throw DomainValidationException(
                listOf(
                    FieldError(
                        field = "specialHourlyWageId",
                        code = "INVALID_SPECIAL_WAGE",
                        message = "特別時給が見つからないか、他のユーザーのものです"
                    )
                )
            )

    private fun overlapError() = FieldError(
        field = "timeRange",
        code = "OVERLAP",
        message = "同一日に時間が重複するシフトは登録できません。"
    )
}
