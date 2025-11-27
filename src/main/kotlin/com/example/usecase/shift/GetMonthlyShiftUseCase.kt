package com.example.usecase.shift

/**
 * ### このファイルの役割
 * - ユーザーと年月をもとに、その月のシフト一覧を取得する薄いユースケースです。
 * - 画面層からは yearMonth だけを渡せばよい形にラップしています。
 */

import com.example.domain.model.Shift
import com.example.domain.repository.ShiftRepository
import java.time.YearMonth

/**
 * 指定年月のシフト一覧を取得するユースケース。
 */
class GetMonthlyShiftUseCase(
    private val repository: ShiftRepository
) {

    suspend operator fun invoke(userId: Long, yearMonth: YearMonth): List<Shift> =
        repository.getMonthlyShifts(userId, yearMonth)
}

