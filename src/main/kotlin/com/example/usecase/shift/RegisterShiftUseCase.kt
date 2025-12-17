package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - 新規シフト登録を行うユースケースで、休憩と勤務時間の整合性チェックを実装しています。
 * - Command モデルからドメインモデルを組み立て、Repository への保存までを担当します。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.SpecialHourlyWageRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.valiktor.functions.isLessThan
import org.valiktor.validate

/**
 * シフト登録ユースケース。
 */
class RegisterShiftUseCase(
    private val repository: ShiftRepository,
    private val specialHourlyWageRepository: SpecialHourlyWageRepository,
    private val contextProvider: ShiftContextProvider,
    private val shiftNotificationService: ShiftNotificationService,
    private val permissionService: ShiftPermissionService
) {

    /**
     * シフト登録時の入力値を保持するコマンド。
     */
    data class Command(
        val actorId: Long,
        val targetUserId: Long,
        val requestedStoreId: Long?,
        val workDate: LocalDate,
        val startTime: Instant,
        val endTime: Instant,
        val memo: String?,
        val breaks: List<BreakCommand>,
        val specialHourlyWageId: Long? = null
    )

    /**
     * 1 回分の休憩時間を表すコマンド。
     */
    data class BreakCommand(
        val breakStart: Instant,
        val breakEnd: Instant
    )

    /**
     * 権限チェックとバリデーションを通過したシフトを登録し、監査情報付きで結果を返す。
     */
    suspend operator fun invoke(command: Command, auditContext: AuditContext): Shift {
        val context = contextProvider.forCreate(
            actorId = command.actorId,
            targetUserId = command.targetUserId,
            requestedStoreId = command.requestedStoreId
        )
        permissionService.ensureCanCreate(context)
        val ownerId = context.targetUser.id ?: error("Target user missing id.")
        val storeId = context.targetStore.id
        command.validate(ownerId)
        val now = Clock.System.now()
        val specialWage = command.specialHourlyWageId?.let { requireSpecialWage(ownerId, it) }
        val shift = Shift(
            id = null,
            userId = ownerId,
            storeId = storeId,
            workDate = command.workDate,
            startTime = command.startTime,
            endTime = command.endTime,
            memo = command.memo,
            specialHourlyWage = specialWage,
            breaks = command.breaks.map {
                ShiftBreak(
                    id = null,
                    shiftId = null,
                    breakStart = it.breakStart,
                    breakEnd = it.breakEnd
                )
            },
            createdAt = now,
            updatedAt = now
        )
        val created = repository.insertShift(shift)
        shiftNotificationService.notifyCreated(
            actorId = auditContext.performedBy,
            targetUserId = ownerId,
            shift = created
        )
        return created
    }

    private suspend fun Command.validate(ownerId: Long) {
        validate(this) {
            validate(Command::startTime).isLessThan(endTime)
        }

        val violations = mutableListOf<FieldError>()
        validateBreaks(
            breaks = breaks.mapIndexed { idx, br -> Triple(idx, br.breakStart, br.breakEnd) },
            shiftStart = startTime,
            shiftEnd = endTime,
            violations = violations
        )

        if (hasShiftOverlap(ownerId, workDate, startTime, endTime, null)) {
            violations += overlapError()
        }

        if (violations.isNotEmpty()) {
            throw DomainValidationException(violations)
        }
    }

    private fun validateBreaks(
        breaks: List<Triple<Int, Instant, Instant>>,
        shiftStart: Instant,
        shiftEnd: Instant,
        violations: MutableList<FieldError>
    ) {
        breaks.forEach { (index, start, end) ->
            if (start >= end) {
                violations += FieldError(
                    field = "breaks[$index]",
                    code = "INVALID_BREAK_RANGE",
                    message = "休憩終了時刻は開始時刻より後に設定してください。"
                )
            }
            if (start < shiftStart) {
                violations += FieldError(
                    field = "breaks[$index].breakStart",
                    code = "BREAK_BEFORE_SHIFT",
                    message = "休憩開始時刻は勤務開始時刻以降に設定してください。"
                )
            }
            if (end > shiftEnd) {
                violations += FieldError(
                    field = "breaks[$index].breakEnd",
                    code = "BREAK_AFTER_SHIFT",
                    message = "休憩終了時刻が勤務終了時刻を超えています。"
                )
            }
        }

        val sorted = breaks.sortedBy { it.second }
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val current = sorted[i]
            if (current.second < prev.third) {
                violations += FieldError(
                    field = "breaks[${current.first}]",
                    code = "BREAK_OVERLAP",
                    message = "休憩時間が互いに重複しています。"
                )
                break
            }
        }
    }

    private suspend fun hasShiftOverlap(
        userId: Long,
        workDate: LocalDate,
        start: Instant,
        end: Instant,
        excludeShiftId: Long?
    ): Boolean {
        return repository.getShiftsOnDate(userId, workDate)
            .filter { it.id != excludeShiftId }
            .any { ShiftOverlapChecker.overlaps(it.startTime, it.endTime, start, end) }
    }

    private fun overlapError() = FieldError(
        field = "timeRange",
        code = "OVERLAP",
        message = "同一日に時間が重複するシフトは登録できません。"
    )

    private suspend fun requireSpecialWage(userId: Long, specialWageId: Long) =
        specialHourlyWageRepository.findById(specialWageId)?.takeIf { it.userId == userId }
            ?: throw DomainValidationException(
                listOf(
                    FieldError(
                        field = "specialHourlyWageId",
                        code = "INVALID_SPECIAL_WAGE",
                        message = "特別時給が見つからないか、他のユーザーのものです。"
                    )
                )
            )
}
