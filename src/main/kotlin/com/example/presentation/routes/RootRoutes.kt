package com.example.presentation.routes

/**
 * ### このファイルの役割
 * - 全ルートを束ねて Ktor の routing に組み込むためのエントリポイントです。
 * - DI 済み UseCase を受け取って、認証が必要なブロックとそうでないブロックを定義します。
 */

import com.example.security.LoginAttemptTracker
import com.example.usecase.auth.LoginUserUseCase
import com.example.usecase.auth.RefreshAccessTokenUseCase
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import com.example.usecase.game.DeleteGameResultUseCase
import com.example.usecase.game.DeleteSimpleBatchResultsUseCase
import com.example.usecase.game.EditGameResultUseCase
import com.example.usecase.game.FinishSimpleBatchUseCase
import com.example.usecase.game.GetGameResultUseCase
import com.example.usecase.game.GetRankingUseCase
import com.example.usecase.advance.GetAdvancePaymentUseCase
import com.example.usecase.advance.UpsertAdvancePaymentUseCase
import com.example.usecase.game.GetUserStatsUseCase
import com.example.usecase.game.PatchGameResultUseCase
import com.example.usecase.game.RecordGameResultUseCase
import com.example.usecase.game.StartSimpleBatchUseCase
import com.example.usecase.prefecture.GetPrefectureListUseCase
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import com.example.usecase.settings.GetGameSettingsUseCase
import com.example.usecase.settings.PatchGameSettingsUseCase
import com.example.usecase.settings.UpdateGameSettingsUseCase
import com.example.usecase.settings.CreateSpecialHourlyWageUseCase
import com.example.usecase.settings.DeleteSpecialHourlyWageUseCase
import com.example.usecase.settings.ListSpecialHourlyWagesUseCase
import com.example.usecase.shift.DeleteShiftUseCase
import com.example.usecase.shift.EditShiftUseCase
import com.example.usecase.shift.GetDailyShiftUseCase
import com.example.usecase.shift.GetMonthlyShiftUseCase
import com.example.usecase.shift.GetShiftRangeUseCase
import com.example.usecase.shift.GetShiftStatsUseCase
import com.example.usecase.shift.PatchShiftUseCase
import com.example.usecase.shift.RegisterShiftUseCase
import com.example.usecase.store.GetStoreListUseCase
import com.example.usecase.user.CreateUserUseCase
import com.example.usecase.user.DeleteUserUseCase
import com.example.usecase.user.GetUserUseCase
import com.example.usecase.user.PatchUserUseCase
import com.example.usecase.user.UpdateUserUseCase
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route

/**
 * Route 入口。全ての機能ルートをまとめてインストールする。
 */
class RootRoutes(
    private val createUserUseCase: CreateUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val patchUserUseCase: PatchUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val getGameSettingsUseCase: GetGameSettingsUseCase,
    private val updateGameSettingsUseCase: UpdateGameSettingsUseCase,
    private val patchGameSettingsUseCase: PatchGameSettingsUseCase,
    private val listSpecialHourlyWagesUseCase: ListSpecialHourlyWagesUseCase,
    private val createSpecialHourlyWageUseCase: CreateSpecialHourlyWageUseCase,
    private val deleteSpecialHourlyWageUseCase: DeleteSpecialHourlyWageUseCase,
    private val recordGameResultUseCase: RecordGameResultUseCase,
    private val editGameResultUseCase: EditGameResultUseCase,
    private val patchGameResultUseCase: PatchGameResultUseCase,
    private val deleteGameResultUseCase: DeleteGameResultUseCase,
    private val getGameResultUseCase: GetGameResultUseCase,
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val startSimpleBatchUseCase: StartSimpleBatchUseCase,
    private val finishSimpleBatchUseCase: FinishSimpleBatchUseCase,
    private val deleteSimpleBatchResultsUseCase: DeleteSimpleBatchResultsUseCase,
    private val getRankingUseCase: GetRankingUseCase,
    private val registerShiftUseCase: RegisterShiftUseCase,
    private val editShiftUseCase: EditShiftUseCase,
    private val patchShiftUseCase: PatchShiftUseCase,
    private val deleteShiftUseCase: DeleteShiftUseCase,
    private val getMonthlyShiftUseCase: GetMonthlyShiftUseCase,
    private val getDailyShiftUseCase: GetDailyShiftUseCase,
    private val getShiftRangeUseCase: GetShiftRangeUseCase,
    private val getShiftStatsUseCase: GetShiftStatsUseCase,
    private val calculateMonthlySalaryUseCase: CalculateMonthlySalaryUseCase,
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getStoreListUseCase: GetStoreListUseCase,
    private val getPrefectureListUseCase: GetPrefectureListUseCase,
    private val loginAttemptTracker: LoginAttemptTracker,
    private val refreshAccessTokenUseCase: RefreshAccessTokenUseCase,
    private val accessTokenExpiresInSec: Long,
    private val getAdvancePaymentUseCase: GetAdvancePaymentUseCase,
    private val upsertAdvancePaymentUseCase: UpsertAdvancePaymentUseCase
) {

    /**
     * ルート配下にすべてのエンドポイントを構築する。
     */
    fun Route.installAll(authName: String = "auth-jwt") {
        installAuthRoutes(
            createUserUseCase = createUserUseCase,
            loginUserUseCase = loginUserUseCase,
            refreshAccessTokenUseCase = refreshAccessTokenUseCase,
            loginAttemptTracker = loginAttemptTracker,
            accessTokenExpiresInSec = accessTokenExpiresInSec
        )
        installStoreRoutes(getStoreListUseCase = getStoreListUseCase)
        installPrefectureRoutes(getPrefectureListUseCase = getPrefectureListUseCase)
        authenticate(authName) {
            installUserRoutes(
                getUserUseCase = getUserUseCase,
                updateUserUseCase = updateUserUseCase,
                patchUserUseCase = patchUserUseCase,
                deleteUserUseCase = deleteUserUseCase
            )
            installSettingsRoutes(
                getGameSettingsUseCase = getGameSettingsUseCase,
                updateGameSettingsUseCase = updateGameSettingsUseCase,
                patchGameSettingsUseCase = patchGameSettingsUseCase,
                listSpecialHourlyWagesUseCase = listSpecialHourlyWagesUseCase,
                createSpecialHourlyWageUseCase = createSpecialHourlyWageUseCase,
                deleteSpecialHourlyWageUseCase = deleteSpecialHourlyWageUseCase
            )
            installGameResultRoutes(
                recordGameResultUseCase = recordGameResultUseCase,
                editGameResultUseCase = editGameResultUseCase,
                patchGameResultUseCase = patchGameResultUseCase,
                deleteGameResultUseCase = deleteGameResultUseCase,
                getGameResultUseCase = getGameResultUseCase,
                getUserStatsUseCase = getUserStatsUseCase,
                startSimpleBatchUseCase = startSimpleBatchUseCase,
                finishSimpleBatchUseCase = finishSimpleBatchUseCase,
                deleteSimpleBatchResultsUseCase = deleteSimpleBatchResultsUseCase
            )
            installRankingRoutes(getRankingUseCase = getRankingUseCase)
            installShiftRoutes(
                getMonthlyShiftUseCase = getMonthlyShiftUseCase,
                getDailyShiftUseCase = getDailyShiftUseCase,
                getShiftRangeUseCase = getShiftRangeUseCase,
                getShiftStatsUseCase = getShiftStatsUseCase,
                registerShiftUseCase = registerShiftUseCase,
                editShiftUseCase = editShiftUseCase,
                patchShiftUseCase = patchShiftUseCase,
                deleteShiftUseCase = deleteShiftUseCase
            )
            installSalaryRoutes(calculateMonthlySalaryUseCase = calculateMonthlySalaryUseCase)
            installDashboardRoutes(getDashboardSummaryUseCase = getDashboardSummaryUseCase)
            installAdvancePaymentRoutes(
                getAdvancePaymentUseCase = getAdvancePaymentUseCase,
                upsertAdvancePaymentUseCase = upsertAdvancePaymentUseCase
            )
        }
    }
}

