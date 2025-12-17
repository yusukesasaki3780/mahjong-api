package com.example.usecase.shift

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.domain.model.Shift
import com.example.domain.model.ShiftRequirement
import com.example.domain.model.ShiftSlotType
import com.example.domain.model.User
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.ShiftRequirementRepository
import com.example.domain.repository.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class GetShiftBoardUseCase(
    private val userRepository: UserRepository,
    private val shiftRepository: ShiftRepository,
    private val shiftRequirementRepository: ShiftRequirementRepository,
    private val contextProvider: ShiftContextProvider,
    private val permissionService: ShiftPermissionService,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {

    data class Command(
        val actorId: Long,
        val storeId: Long,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val includeDeletedUsers: Boolean
    )

    data class Result(
        val storeId: Long,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val users: List<UserSummary>,
        val shifts: List<ShiftSummary>,
        val requirements: List<RequirementSummary>,
        val editable: Boolean
    )

    data class UserSummary(
        val id: Long,
        val name: String,
        val nickname: String,
        val zooId: Int,
        val isDeleted: Boolean
    )

    data class ShiftSummary(
        val id: Long,
        val userId: Long,
        val workDate: LocalDate,
        val shiftType: ShiftSlotType,
        val startTime: kotlinx.datetime.Instant,
        val endTime: kotlinx.datetime.Instant,
        val memo: String?
    )

    data class RequirementSummary(
        val id: Long?,
        val targetDate: LocalDate,
        val shiftType: ShiftSlotType,
        val startRequired: Int,
        val endRequired: Int,
        val startActual: Int,
        val endActual: Int,
        val editable: Boolean
    )

    suspend operator fun invoke(command: Command): Result {
        requireRange(command.startDate, command.endDate)
        val viewContext = contextProvider.forStoreView(command.actorId, command.storeId)
        permissionService.ensureCanView(viewContext)
        val targetStore = viewContext.primaryStore ?: error("Target store missing.")
        val targetStoreId = targetStore.id
        val actor = viewContext.actor

        val includeDeleted = command.includeDeletedUsers && viewContext.canIncludeDeletedUsers
        val baseUsers = userRepository.listNonAdminUsers(
            storeId = targetStoreId,
            includeDeleted = includeDeleted
        )
        val userSummaries = baseUsers.mapNotNull { user ->
            val id = user.id ?: return@mapNotNull null
            UserSummary(
                id = id,
                name = user.name,
                nickname = user.nickname,
                zooId = user.zooId,
                isDeleted = user.isDeleted
            )
        }.toMutableList()

        val shifts = shiftRepository
            .getShiftsByStore(targetStoreId, command.startDate, command.endDate)
            .map(::toSummary)

        val existingUserIds = userSummaries.map { it.id }.toMutableSet()
        val shiftUserIds = shifts.map { it.userId }.toSet()
        val missingUserIds = shiftUserIds - existingUserIds
        if (missingUserIds.isNotEmpty()) {
            val helperUsers = userRepository.findByIds(missingUserIds)
            helperUsers.forEach { helper ->
                val id = helper.id ?: return@forEach
                existingUserIds += id
                userSummaries += UserSummary(
                    id = id,
                    name = helper.name,
                    nickname = helper.nickname,
                    zooId = helper.zooId,
                    isDeleted = helper.isDeleted
                )
            }
        }

        maybeIncludeActor(actor, targetStoreId, userSummaries)
        val users = userSummaries.sortedBy { it.id }

        val requirementActuals = calculateRequirementActuals(shifts)
        val requirementEntities = shiftRequirementRepository
            .findByStoreAndDateRange(targetStoreId, command.startDate, command.endDate)
            .associateBy { RequirementKey(it.targetDate, it.shiftType) }

        val requirements = buildList {
            var date = command.startDate
            while (date <= command.endDate) {
                ShiftSlotType.values().forEach { type ->
                    val key = RequirementKey(date, type)
                    val entity = requirementEntities[key]
                    val actual = requirementActuals[key] ?: RequirementActual.ZERO
                    add(
                        toRequirementSummary(
                            targetDate = date,
                            shiftType = type,
                            requirement = entity,
                            isAdmin = viewContext.editable,
                            actual = actual
                        )
                    )
                }
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }

        return Result(
            storeId = targetStoreId,
            startDate = command.startDate,
            endDate = command.endDate,
            users = users,
            shifts = shifts,
            requirements = requirements,
            editable = viewContext.editable
        )
    }

    private fun requireRange(start: LocalDate, end: LocalDate) {
        if (end < start) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "endDate",
                        code = "INVALID_RANGE",
                        message = "endDate は startDate 以降の日付を指定してください。"
                    )
                )
            )
        }
    }

    private fun toSummary(shift: Shift): ShiftSummary =
        ShiftSummary(
            id = shift.id ?: error("Shift id missing"),
            userId = shift.userId,
            workDate = shift.workDate,
            shiftType = classifySlot(shift),
            startTime = shift.startTime,
            endTime = shift.endTime,
            memo = shift.memo
        )

    private fun toRequirementSummary(
        targetDate: LocalDate,
        shiftType: ShiftSlotType,
        requirement: ShiftRequirement?,
        isAdmin: Boolean,
        actual: RequirementActual
    ): RequirementSummary {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val editable = isAdmin && targetDate >= today
        val defaults = defaultRequirement(targetDate, shiftType)
        return RequirementSummary(
            id = requirement?.id,
            targetDate = targetDate,
            shiftType = shiftType,
            startRequired = requirement?.startRequired ?: defaults.startRequired,
            endRequired = requirement?.endRequired ?: defaults.endRequired,
            startActual = actual.startActual,
            endActual = actual.endActual,
            editable = editable
        )
    }

    private fun defaultRequirement(date: LocalDate, type: ShiftSlotType): RequirementDefaults {
        val day = date.dayOfWeek
        return when (type) {
            ShiftSlotType.EARLY -> {
                if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                    RequirementDefaults(startRequired = 4, endRequired = 4)
                } else {
                    RequirementDefaults(startRequired = 3, endRequired = 4)
                }
            }

            ShiftSlotType.LATE -> {
                if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
                    RequirementDefaults(startRequired = 5, endRequired = 4)
                } else {
                    RequirementDefaults(startRequired = 4, endRequired = 3)
                }
            }
        }
    }

    private fun calculateRequirementActuals(shifts: List<ShiftSummary>): Map<RequirementKey, RequirementActual> {
        if (shifts.isEmpty()) return emptyMap()
        val windowsByKey = shifts
            .map(::toShiftWindow)
            .groupBy { RequirementKey(it.date, it.type) }
        return windowsByKey.mapValues { (key, windows) ->
            val checkpoints = checkpointDateTimes(key.date, key.type)
            RequirementActual(
                startActual = windows.count { window -> window.covers(checkpoints.start) },
                endActual = windows.count { window -> window.covers(checkpoints.end) }
            )
        }
    }

    private fun toShiftWindow(shift: ShiftSummary): ShiftWindow {
        val startLocalTime = shift.startTime.toLocalDateTime(timeZone).time
        val endLocalTime = shift.endTime.toLocalDateTime(timeZone).time
        val start = LocalDateTime(shift.workDate, startLocalTime)
        var end = LocalDateTime(shift.workDate, endLocalTime)
        while (end <= start) {
            end = end.plusDays(1)
        }
        return ShiftWindow(
            date = shift.workDate,
            type = shift.shiftType,
            start = start,
            end = end
        )
    }

    private fun checkpointDateTimes(date: LocalDate, type: ShiftSlotType): CheckpointDateTimes = when (type) {
        ShiftSlotType.EARLY -> CheckpointDateTimes(
            start = LocalDateTime(date, EARLY_START_TIME),
            end = LocalDateTime(date, EARLY_END_TIME)
        )

        ShiftSlotType.LATE -> CheckpointDateTimes(
            start = LocalDateTime(date, LATE_START_TIME),
            end = LocalDateTime(date.plus(1, DateTimeUnit.DAY), LATE_END_TIME)
        )
    }

    private data class RequirementKey(
        val date: LocalDate,
        val type: ShiftSlotType
    )

    private data class RequirementActual(
        val startActual: Int,
        val endActual: Int
    ) {
        companion object {
            val ZERO = RequirementActual(0, 0)
        }
    }

    private data class ShiftWindow(
        val date: LocalDate,
        val type: ShiftSlotType,
        val start: LocalDateTime,
        val end: LocalDateTime
    ) {
        fun covers(checkpoint: LocalDateTime): Boolean = start <= checkpoint && checkpoint <= end
    }

    private data class CheckpointDateTimes(
        val start: LocalDateTime,
        val end: LocalDateTime
    )

    private data class RequirementDefaults(
        val startRequired: Int,
        val endRequired: Int
    )

    private fun LocalDateTime.plusDays(days: Int): LocalDateTime =
        LocalDateTime(this.date.plus(days, DateTimeUnit.DAY), this.time)

    private fun classifySlot(shift: Shift): ShiftSlotType {
        val startLocal = shift.startTime.toLocalDateTime(timeZone)
        val referenceMidnight = startLocal.date.atStartOfDayIn(timeZone)
        val startMinutes = (shift.startTime - referenceMidnight).inWholeMinutes
        var endMinutes = (shift.endTime - referenceMidnight).inWholeMinutes
        while (endMinutes <= startMinutes) {
            endMinutes += MINUTES_PER_DAY.toLong()
        }
        val totalMinutes = endMinutes - startMinutes
        val earlyMinutes = overlapWithBand(
            start = startMinutes,
            end = endMinutes,
            bandStart = EARLY_START_MINUTE.toLong(),
            bandEnd = EARLY_END_MINUTE.toLong()
        )
        val lateMinutes = totalMinutes - earlyMinutes
        return if (lateMinutes > earlyMinutes) ShiftSlotType.LATE else ShiftSlotType.EARLY
    }

    private fun maybeIncludeActor(actor: User, targetStoreId: Long, summaries: MutableList<UserSummary>) {
        val actorId = actor.id ?: return
        if (actor.storeId != targetStoreId) return
        if (actor.isDeleted) return
        val alreadyListed = summaries.any { it.id == actorId }
        if (alreadyListed) return
        summaries += UserSummary(
            id = actorId,
            name = actor.name,
            nickname = actor.nickname,
            zooId = actor.zooId,
            isDeleted = actor.isDeleted
        )
    }

    private fun overlapWithBand(start: Long, end: Long, bandStart: Long, bandEnd: Long): Long {
        var total = 0L
        var dayStart = (start / MINUTES_PER_DAY.toLong()) * MINUTES_PER_DAY.toLong()
        while (dayStart < end) {
            val windowStart = dayStart + bandStart
            val windowEnd = dayStart + bandEnd
            total += overlapRange(start, end, windowStart, windowEnd)
            dayStart += MINUTES_PER_DAY.toLong()
        }
        return total
    }

    private fun overlapRange(start: Long, end: Long, windowStart: Long, windowEnd: Long): Long {
        val overlapStart = maxOf(start, windowStart)
        val overlapEnd = minOf(end, windowEnd)
        return (overlapEnd - overlapStart).coerceAtLeast(0L)
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val EARLY_START_MINUTE = 10 * 60
        private const val EARLY_END_MINUTE = 22 * 60
        private val EARLY_START_TIME = LocalTime(hour = 10, minute = 0)
        private val EARLY_END_TIME = LocalTime(hour = 22, minute = 0)
        private val LATE_START_TIME = LocalTime(hour = 22, minute = 0)
        private val LATE_END_TIME = LocalTime(hour = 10, minute = 0)
    }
}
