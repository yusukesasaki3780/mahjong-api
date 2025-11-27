package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - ダッシュボードサマリー API のルーティングで、認証済みユーザー自身のデータのみ参照できるようにしています。
 * - yearMonth パラメータの取得やバリデーションを行い、UseCase からの結果を DTO に変換して返します。
 */

import com.example.presentation.dto.dashboard.DashboardSummaryResponse
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * ダッシュボード系ルート。
 */
fun Route.installDashboardRoutes(
    getDashboardSummaryUseCase: GetDashboardSummaryUseCase
) {
    get("/users/{userId}/dashboard/summary") {
        val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
        call.requireUserAccess(userId) ?: return@get
        val yearMonth = call.queryYearMonth() ?: return@get call.respondMissingYearMonth()

        val summary = getDashboardSummaryUseCase(userId, yearMonth)
        call.respond(DashboardSummaryResponse.from(summary))
    }
}

