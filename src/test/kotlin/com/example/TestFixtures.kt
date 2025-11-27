package com.example

import com.example.domain.model.GameResult
import com.example.domain.model.GameSettings
import com.example.domain.model.GameType
import com.example.domain.model.RankingEntry
import com.example.domain.model.Shift
import com.example.domain.model.ShiftBreak
import com.example.domain.model.StatsRange
import com.example.domain.model.StoreMaster
import com.example.domain.model.User
import com.example.domain.model.Prefecture
import com.example.usecase.game.GetUserStatsUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object TestFixtures {
    private val now = Clock.System.now()

    fun user(id: Long = 1) = User(
        id = id,
        name = "User$id",
        nickname = "nickname$id",
        storeName = "Store",
        prefectureCode = "01",
        email = "user$id@example.com",
        createdAt = now,
        updatedAt = now
    )

    fun gameSettings(userId: Long = 1) = GameSettings(
        userId = userId,
        yonmaGameFee = 400,
        sanmaGameFee = 250,
        sanmaGameFeeBack = 0,
        yonmaTipUnit = 100,
        sanmaTipUnit = 50,
        wageType = com.example.domain.model.WageType.HOURLY,
        hourlyWage = 1200,
        fixedSalary = 300000,
        nightRateMultiplier = 1.25,
        baseMinWage = 1200,
        incomeTaxRate = 0.1,
        transportPerShift = 500,
        createdAt = now,
        updatedAt = now
    )

    fun gameResult(id: Long = 10, userId: Long = 1) = GameResult(
        id = id,
        userId = userId,
        gameType = GameType.SANMA,
        playedAt = now,
        place = 1,
        baseIncome = 1000,
        tipCount = 1,
        tipIncome = 100,
        totalIncome = 1100,
        note = "memo",
        createdAt = now,
        updatedAt = now
    )

    fun userStats(userId: Long = 1) = GetUserStatsUseCase.Result(
        userId = userId,
        range = StatsRange(now, now),
        averagePlace = 1.5,
        totalGames = 2,
        totalIncome = 2000,
        results = listOf(gameResult())
    )

    fun shift(id: Long = 5, userId: Long = 1): Shift {
        val start = now
        val end = now.plus(2, DateTimeUnit.HOUR)
        val breakStart = start.plus(30, DateTimeUnit.MINUTE)
        val breakEnd = breakStart.plus(15, DateTimeUnit.MINUTE)
        return Shift(
            id = id,
            userId = userId,
            workDate = LocalDate(2025, 1, 1),
            startTime = start,
            endTime = end,
            memo = "memo",
            breaks = listOf(
                ShiftBreak(
                    id = 100,
                    shiftId = id,
                    breakStart = breakStart,
                    breakEnd = breakEnd
                )
            ),
            createdAt = now,
            updatedAt = now
        )
    }

    fun store(id: Long = 1, name: String = "Store$id") = StoreMaster(
        id = id,
        storeName = name,
        createdAt = now,
        updatedAt = now
    )

    fun prefecture(code: String = "01", name: String = "Prefecture") = Prefecture(
        code = code,
        name = name
    )

    fun rankingEntry(
        userId: Long = 1,
        name: String = "User$userId",
        totalIncome: Long = 5000,
        gameCount: Int = 10,
        averagePlace: Double? = 2.0
    ) = RankingEntry(
        userId = userId,
        name = name,
        totalIncome = totalIncome,
        gameCount = gameCount,
        averagePlace = averagePlace
    )
}
