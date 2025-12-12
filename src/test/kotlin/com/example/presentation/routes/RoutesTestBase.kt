package com.example.presentation.routes

import com.example.config.JwtConfig
import com.example.config.JwtProvider
import com.example.configureMonitoring
import com.example.security.LoginAttemptTracker
import com.example.usecase.auth.LoginUserUseCase
import com.example.usecase.auth.RefreshAccessTokenUseCase
import com.example.usecase.advance.GetAdvancePaymentUseCase
import com.example.usecase.advance.UpsertAdvancePaymentUseCase
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import com.example.usecase.game.DeleteGameResultUseCase
import com.example.usecase.game.EditGameResultUseCase
import com.example.usecase.game.GetGameResultUseCase
import com.example.usecase.game.GetRankingUseCase
import com.example.usecase.game.GetMyRankingUseCase
import com.example.usecase.game.GetUserStatsUseCase
import com.example.usecase.game.PatchGameResultUseCase
import com.example.usecase.game.RecordGameResultUseCase
import com.example.usecase.game.StartSimpleBatchUseCase
import com.example.usecase.game.FinishSimpleBatchUseCase
import com.example.usecase.game.DeleteSimpleBatchResultsUseCase
import com.example.usecase.prefecture.GetPrefectureListUseCase
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import com.example.usecase.settings.CreateSpecialHourlyWageUseCase
import com.example.usecase.settings.DeleteSpecialHourlyWageUseCase
import com.example.usecase.settings.GetGameSettingsUseCase
import com.example.usecase.settings.ListSpecialHourlyWagesUseCase
import com.example.usecase.settings.PatchGameSettingsUseCase
import com.example.usecase.settings.UpdateGameSettingsUseCase
import com.example.usecase.shift.DeleteShiftUseCase
import com.example.usecase.shift.EditShiftUseCase
import com.example.usecase.shift.GetDailyShiftUseCase
import com.example.usecase.shift.GetMonthlyShiftUseCase
import com.example.usecase.shift.GetShiftRangeUseCase
import com.example.usecase.shift.GetShiftStatsUseCase
import com.example.usecase.shift.PatchShiftUseCase
import com.example.usecase.shift.RegisterShiftUseCase
import com.example.usecase.store.GetStoreListUseCase
import com.example.usecase.user.AdminDeleteUserUseCase
import com.example.usecase.user.AdminResetUserPasswordUseCase
import com.example.usecase.user.CreateUserUseCase
import com.example.usecase.user.DeleteMyAccountUseCase
import com.example.usecase.user.DeleteUserUseCase
import com.example.usecase.user.GetUserUseCase
import com.example.usecase.user.ListGeneralUsersUseCase
import com.example.usecase.user.PatchUserUseCase
import com.example.usecase.user.UpdateUserUseCase
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import kotlinx.serialization.json.Json

/**
 * ルートテストで全 UseCase モックをまとめて扱うためのベースクラス。
 */
abstract class RoutesTestBase {

    protected val json = Json

    private val testJwtConfig = JwtConfig(
        secret = "change-me-in-prod",
        issuer = "mahjong-api",
        audience = "mahjong-users",
        realm = "mahjong-auth",
        expiresInSec = 3600
    )
    private val testJwtProvider = JwtProvider(testJwtConfig)

    protected val createUserUseCase: CreateUserUseCase = mockk(relaxed = true)
    protected val loginUserUseCase: LoginUserUseCase = mockk(relaxed = true)
    protected val refreshAccessTokenUseCase: RefreshAccessTokenUseCase = mockk(relaxed = true)
    protected val getUserUseCase: GetUserUseCase = mockk(relaxed = true)
    protected val updateUserUseCase: UpdateUserUseCase = mockk(relaxed = true)
    protected val patchUserUseCase: PatchUserUseCase = mockk(relaxed = true)
    protected val deleteUserUseCase: DeleteUserUseCase = mockk(relaxed = true)
    protected val deleteMyAccountUseCase: DeleteMyAccountUseCase = mockk(relaxed = true)
    protected val listGeneralUsersUseCase: ListGeneralUsersUseCase = mockk(relaxed = true)
    protected val adminDeleteUserUseCase: AdminDeleteUserUseCase = mockk(relaxed = true)
    protected val adminResetUserPasswordUseCase: AdminResetUserPasswordUseCase = mockk(relaxed = true)

    protected val getGameSettingsUseCase: GetGameSettingsUseCase = mockk(relaxed = true)
    protected val updateGameSettingsUseCase: UpdateGameSettingsUseCase = mockk(relaxed = true)
    protected val patchGameSettingsUseCase: PatchGameSettingsUseCase = mockk(relaxed = true)
    protected val listSpecialHourlyWagesUseCase: ListSpecialHourlyWagesUseCase = mockk(relaxed = true)
    protected val createSpecialHourlyWageUseCase: CreateSpecialHourlyWageUseCase = mockk(relaxed = true)
    protected val deleteSpecialHourlyWageUseCase: DeleteSpecialHourlyWageUseCase = mockk(relaxed = true)

    protected val recordGameResultUseCase: RecordGameResultUseCase = mockk(relaxed = true)
    protected val editGameResultUseCase: EditGameResultUseCase = mockk(relaxed = true)
    protected val patchGameResultUseCase: PatchGameResultUseCase = mockk(relaxed = true)
    protected val deleteGameResultUseCase: DeleteGameResultUseCase = mockk(relaxed = true)
    protected val getGameResultUseCase: GetGameResultUseCase = mockk(relaxed = true)
    protected val getUserStatsUseCase: GetUserStatsUseCase = mockk(relaxed = true)
    protected val startSimpleBatchUseCase: StartSimpleBatchUseCase = mockk(relaxed = true)
    protected val finishSimpleBatchUseCase: FinishSimpleBatchUseCase = mockk(relaxed = true)
    protected val deleteSimpleBatchResultsUseCase: DeleteSimpleBatchResultsUseCase = mockk(relaxed = true)
    protected val getRankingUseCase: GetRankingUseCase = mockk(relaxed = true)
    protected val getMyRankingUseCase: GetMyRankingUseCase = mockk(relaxed = true)

    protected val registerShiftUseCase: RegisterShiftUseCase = mockk(relaxed = true)
    protected val editShiftUseCase: EditShiftUseCase = mockk(relaxed = true)
    protected val patchShiftUseCase: PatchShiftUseCase = mockk(relaxed = true)
    protected val deleteShiftUseCase: DeleteShiftUseCase = mockk(relaxed = true)
    protected val getMonthlyShiftUseCase: GetMonthlyShiftUseCase = mockk(relaxed = true)
    protected val getDailyShiftUseCase: GetDailyShiftUseCase = mockk(relaxed = true)
    protected val getShiftRangeUseCase: GetShiftRangeUseCase = mockk(relaxed = true)
    protected val getShiftStatsUseCase: GetShiftStatsUseCase = mockk(relaxed = true)

    protected val calculateMonthlySalaryUseCase: CalculateMonthlySalaryUseCase = mockk(relaxed = true)
    protected val getDashboardSummaryUseCase: GetDashboardSummaryUseCase = mockk(relaxed = true)
    protected val getStoreListUseCase: GetStoreListUseCase = mockk(relaxed = true)
    protected val getPrefectureListUseCase: GetPrefectureListUseCase = mockk(relaxed = true)
    protected val getAdvancePaymentUseCase: GetAdvancePaymentUseCase = mockk(relaxed = true)
    protected val upsertAdvancePaymentUseCase: UpsertAdvancePaymentUseCase = mockk(relaxed = true)

    protected fun ApplicationTestBuilder.installRoutes() {
        environment {
            config = MapApplicationConfig()
        }
        application {
            if (pluginOrNull(ContentNegotiation) == null) {
                install(ContentNegotiation) { json() }
            }
            configureMonitoring()
            if (pluginOrNull(Authentication) == null) {
                install(Authentication) {
                    jwt("auth-jwt") {
                        realm = testJwtConfig.realm
                        verifier(testJwtProvider.verifier)
                        validate { credential ->
                            val userId = credential.payload.getClaim("userId").asLong()
                            if (userId != null) JWTPrincipal(credential.payload) else null
                        }
                    }
                }
            }

            val rootRoutes = RootRoutes(
                createUserUseCase = createUserUseCase,
                loginUserUseCase = loginUserUseCase,
                getUserUseCase = getUserUseCase,
                updateUserUseCase = updateUserUseCase,
                patchUserUseCase = patchUserUseCase,
                deleteUserUseCase = deleteUserUseCase,
                deleteMyAccountUseCase = deleteMyAccountUseCase,
                listGeneralUsersUseCase = listGeneralUsersUseCase,
                adminDeleteUserUseCase = adminDeleteUserUseCase,
                adminResetUserPasswordUseCase = adminResetUserPasswordUseCase,
                getGameSettingsUseCase = getGameSettingsUseCase,
                updateGameSettingsUseCase = updateGameSettingsUseCase,
                patchGameSettingsUseCase = patchGameSettingsUseCase,
                listSpecialHourlyWagesUseCase = listSpecialHourlyWagesUseCase,
                createSpecialHourlyWageUseCase = createSpecialHourlyWageUseCase,
                deleteSpecialHourlyWageUseCase = deleteSpecialHourlyWageUseCase,
                recordGameResultUseCase = recordGameResultUseCase,
                editGameResultUseCase = editGameResultUseCase,
                patchGameResultUseCase = patchGameResultUseCase,
                deleteGameResultUseCase = deleteGameResultUseCase,
                getGameResultUseCase = getGameResultUseCase,
                getUserStatsUseCase = getUserStatsUseCase,
                startSimpleBatchUseCase = startSimpleBatchUseCase,
                finishSimpleBatchUseCase = finishSimpleBatchUseCase,
                deleteSimpleBatchResultsUseCase = deleteSimpleBatchResultsUseCase,
                getRankingUseCase = getRankingUseCase,
                getMyRankingUseCase = getMyRankingUseCase,
                registerShiftUseCase = registerShiftUseCase,
                editShiftUseCase = editShiftUseCase,
                patchShiftUseCase = patchShiftUseCase,
                deleteShiftUseCase = deleteShiftUseCase,
                getMonthlyShiftUseCase = getMonthlyShiftUseCase,
                getDailyShiftUseCase = getDailyShiftUseCase,
                getShiftRangeUseCase = getShiftRangeUseCase,
                getShiftStatsUseCase = getShiftStatsUseCase,
                calculateMonthlySalaryUseCase = calculateMonthlySalaryUseCase,
                getDashboardSummaryUseCase = getDashboardSummaryUseCase,
                getStoreListUseCase = getStoreListUseCase,
                getPrefectureListUseCase = getPrefectureListUseCase,
                loginAttemptTracker = loginAttemptTracker(),
                refreshAccessTokenUseCase = refreshAccessTokenUseCase,
                accessTokenExpiresInSec = testJwtConfig.expiresInSec,
                getAdvancePaymentUseCase = getAdvancePaymentUseCase,
                upsertAdvancePaymentUseCase = upsertAdvancePaymentUseCase
            )

            routing {
                with(rootRoutes) { installAll(authName = "auth-jwt") }
            }
        }
    }

    protected fun HttpRequestBuilder.withAuth(userId: Long = 1L) {
        header(HttpHeaders.Authorization, "Bearer ${testJwtProvider.generateToken(userId).token}")
    }

    protected open fun loginAttemptTracker(): LoginAttemptTracker =
        LoginAttemptTracker(firstThreshold = 1000, secondThreshold = 2000)
}
