package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - シフトの部分更新、特に休憩の追加・削除など細かな修正に対応するユースケースです。
 * - 既存データとパッチ情報を突き合わせて最終状態を導き出し、監査も同時に取ります。
 */

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.AuditContext
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.repository.ShiftBreakPatch
import com.example.domain.repository.ShiftPatch
import com.example.domain.repository.ShiftRepository
import com.example.infrastructure.logging.AuditLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class PatchShiftUseCase(
    private val repository: ShiftRepository,
    private val auditLogger: AuditLogger
) {

    data class Command(
        val userId: Long,
        val shiftId: Long,
        val workDate: LocalDate? = null,
        val startTime: Instant? = null,
        val endTime: Instant? = null,
        val memo: String? = null,
        val breaks: List<BreakPatchCommand>? = null
    )

    data class BreakPatchCommand(
        val id: Long?,
        val breakStart: Instant? = null,
        val breakEnd: Instant? = null,
        val delete: Boolean = false
    )

    suspend operator fun invoke(command: Command, auditContext: AuditContext): Shift {
        val before = repository.findById(command.shiftId)
            ?: throw IllegalArgumentException("Shift not found: ${command.shiftId}")
        val normalizedBreaks = normalizeBreakCommands(command.breaks, before.breaks)
        val normalizedCommand = command.copy(breaks = normalizedBreaks)
        validate(normalizedCommand, before)

        val patch = ShiftPatch(
            workDate = normalizedCommand.workDate,
            startTime = normalizedCommand.startTime,
            endTime = normalizedCommand.endTime,
            memo = normalizedCommand.memo,
            updatedAt = Clock.System.now(),
            breakPatches = normalizedCommand.breaks?.map {
                ShiftBreakPatch(
                    id = it.id,
                    breakStart = it.breakStart,
                    breakEnd = it.breakEnd,
                    delete = it.delete
                )
            }
        )
        val result = repository.patchShift(normalizedCommand.userId, normalizedCommand.shiftId, patch)

        auditLogger.log(
            entityType = "SHIFT",
            entityId = normalizedCommand.shiftId,
            action = "PATCH",
            context = auditContext,
            before = before,
            after = result
        )
        return result
    }

    private fun normalizeBreakCommands(
        requested: List<BreakPatchCommand>?,
        existing: List<ShiftBreak>
    ): List<BreakPatchCommand>? {
        if (requested == null) return null
        val existingIds = existing.mapNotNull { it.id }
        if (requested.isEmpty()) {
            return existingIds.map { BreakPatchCommand(id = it, delete = true) }
        }
        val replacesAll = requested.all { it.id == null && !it.delete }
        if (!replacesAll) {
            return requested
        }
        val deleteAll = existingIds.map { BreakPatchCommand(id = it, delete = true) }
        return deleteAll + requested
    }

    private suspend fun validate(command: Command, before: Shift) {
        val violations = mutableListOf<FieldError>()

        val startProvided = command.startTime != null
        val endProvided = command.endTime != null
        if (startProvided xor endProvided) {
            violations += FieldError(
                field = "startTime/endTime",
                code = "BOTH_REQUIRED",
                message = "startTime and endTime must be provided together"
            )
        }
        if (startProvided && endProvided && command.startTime!! >= command.endTime!!) {
            violations += FieldError(
                field = "endTime",
                code = "INVALID_RANGE",
                message = "endTime must be after startTime"
            )
        }

        val futureStart = command.startTime ?: before.startTime
        val futureEnd = command.endTime ?: before.endTime
        val futureDate = command.workDate ?: before.workDate

        val resultingBreaks = applyBreakPatches(before.breaks, command.breaks, violations)
        validateBreakWindows(resultingBreaks, futureStart, futureEnd, violations)

        val overlap = repository.getShiftsOnDate(before.userId, futureDate)
            .filter { it.id != before.id }
            .any { it.startTime < futureEnd && futureStart < it.endTime }
        if (overlap) {
            violations += FieldError(
                field = "timeRange",
                code = "SHIFT_OVERLAP",
                message = "シフト時間帯が既存の勤務と重複しています"
            )
        }

        if (violations.isNotEmpty()) {
            throw DomainValidationException(violations)
        }
    }

    private fun applyBreakPatches(
        existing: List<ShiftBreak>,
        patches: List<BreakPatchCommand>?,
        violations: MutableList<FieldError>
    ): List<Pair<Long?, Pair<Instant, Instant>>> {
        val result = existing.filter { it.id != null }.associateBy { it.id!! }.toMutableMap()
        val newBreaks = mutableListOf<Pair<Long?, Pair<Instant, Instant>>>()

        patches?.forEachIndexed { index, patch ->
            when {
                patch.delete -> {
                    if (patch.id == null) {
                        violations += FieldError(
                            field = "breaks[$index].id",
                            code = "ID_REQUIRED_FOR_DELETE",
                            message = "break id is required when delete=true"
                        )
                    } else {
                        result.remove(patch.id)
                    }
                }
                patch.id == null -> {
                    val start = patch.breakStart
                    val end = patch.breakEnd
                    if (start == null || end == null) {
                        violations += FieldError(
                            field = "breaks[$index]",
                            code = "BREAK_TIME_REQUIRED",
                            message = "breakStart and breakEnd must both be provided"
                        )
                    } else {
                        newBreaks += null to (start to end)
                    }
                }
                else -> {
                    val current = result[patch.id]
                    if (current == null) {
                        violations += FieldError(
                            field = "breaks[$index].id",
                            code = "UNKNOWN_BREAK",
                            message = "指定された休憩IDが存在しません"
                        )
                    } else {
                        val start = patch.breakStart ?: current.breakStart
                        val end = patch.breakEnd ?: current.breakEnd
                        result[patch.id] = current.copy(breakStart = start, breakEnd = end)
                    }
                }
            }
        }

        return result.values.map { it.id to (it.breakStart to it.breakEnd) } + newBreaks
    }

    private fun validateBreakWindows(
        windows: List<Pair<Long?, Pair<Instant, Instant>>>,
        shiftStart: Instant,
        shiftEnd: Instant,
        violations: MutableList<FieldError>
    ) {
        windows.forEach { (_, window) ->
            val (start, end) = window
            if (start >= end) {
                violations += FieldError(
                    field = "breaks",
                    code = "INVALID_BREAK_RANGE",
                    message = "breakEnd must be after breakStart"
                )
            }
            if (start < shiftStart) {
                violations += FieldError(
                    field = "breaks",
                    code = "BREAK_BEFORE_SHIFT",
                    message = "breakStart must not be before shift start"
                )
            }
            if (end > shiftEnd) {
                violations += FieldError(
                    field = "breaks",
                    code = "BREAK_AFTER_SHIFT",
                    message = "breakEnd must not exceed shift end"
                )
            }
        }

        val sorted = windows.map { it.second }.sortedBy { it.first }
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val current = sorted[i]
            if (current.first < prev.second) {
                violations += FieldError(
                    field = "breaks",
                    code = "BREAK_OVERLAP",
                    message = "休憩時間が互いに重複しています"
                )
                break
            }
        }
    }
}

