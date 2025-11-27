package com.example

import com.example.config.JwtConfig
import com.example.config.JwtProvider
import com.example.database.DatabaseFactory
import com.example.infrastructure.db.repository.ExposedAdvancePaymentRepository
import com.example.infrastructure.db.repository.ExposedAuditRepository
import com.example.infrastructure.db.repository.ExposedGameResultRepository
import com.example.infrastructure.db.repository.ExposedGameSettingsRepository
import com.example.infrastructure.db.repository.ExposedPrefectureRepository
import com.example.infrastructure.db.repository.ExposedRefreshTokenRepository
import com.example.infrastructure.db.repository.ExposedShiftRepository
import com.example.infrastructure.db.repository.ExposedStoreMasterRepository
import com.example.infrastructure.db.repository.ExposedUserCredentialRepository
import com.example.infrastructure.db.repository.ExposedUserRepository
import com.example.infrastructure.logging.AuditLogger
import com.example.presentation.routes.RootRoutes
import com.example.security.LoginAttemptTracker
import com.example.usecase.advance.GetAdvancePaymentUseCase
import com.example.usecase.advance.UpsertAdvancePaymentUseCase
import com.example.usecase.auth.LoginUserUseCase
import com.example.usecase.auth.RefreshAccessTokenUseCase
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import com.example.usecase.game.DeleteGameResultUseCase
import com.example.usecase.game.EditGameResultUseCase
import com.example.usecase.game.GetGameResultUseCase
import com.example.usecase.game.GetRankingUseCase
import com.example.usecase.game.GetUserStatsUseCase
import com.example.usecase.game.PatchGameResultUseCase
import com.example.usecase.game.RecordGameResultUseCase
import com.example.usecase.prefecture.GetPrefectureListUseCase
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import com.example.usecase.settings.CreateDefaultGameSettingsUseCase
import com.example.usecase.settings.GetGameSettingsUseCase
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
import com.example.usecase.user.CreateUserUseCase
import com.example.usecase.user.DeleteUserUseCase
import com.example.usecase.user.GetUserUseCase
import com.example.usecase.user.PatchUserUseCase
import com.example.usecase.user.UpdateUserUseCase
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json

/**
 * ### このファイルの役割
 * - Ktor アプリのメインエントリとして、DI・プラグイン設定・ルーティング登録を一括で行います。
 * - Repository と UseCase の依存関係をここで構築し、presentation 層に渡す配線を一本化しています。
 * - DB 初期化や JWT 認証などアプリ全体に影響する設定を Application 起動時にまとめて呼び出します。
 */
fun Application.module() {
    initializeDatabase()

    val jwtConfig = JwtConfig.from(environment.config)
    val jwtProvider = JwtProvider(jwtConfig)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(jwtProvider.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asLong()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
        }
    }

    val userRepository = ExposedUserRepository()
    val userCredentialRepository = ExposedUserCredentialRepository()
    val refreshTokenRepository = ExposedRefreshTokenRepository()
    val auditRepository = ExposedAuditRepository()
    val auditLogger = AuditLogger(auditRepository, Json { encodeDefaults = true })
    val gameSettingsRepository = ExposedGameSettingsRepository()
    val gameResultRepository = ExposedGameResultRepository()
    val shiftRepository = ExposedShiftRepository()
    val storeMasterRepository = ExposedStoreMasterRepository()
    val prefectureRepository = ExposedPrefectureRepository()
    val advancePaymentRepository = ExposedAdvancePaymentRepository()

    val createDefaultGameSettingsUseCase = CreateDefaultGameSettingsUseCase(gameSettingsRepository)
    val createUserUseCase = CreateUserUseCase(userRepository, userCredentialRepository, createDefaultGameSettingsUseCase)
    val loginUserUseCase = LoginUserUseCase(userRepository, userCredentialRepository, refreshTokenRepository, jwtProvider)
    val refreshAccessTokenUseCase = RefreshAccessTokenUseCase(userRepository, refreshTokenRepository, jwtProvider)
    val getUserUseCase = GetUserUseCase(userRepository)
    val updateUserUseCase = UpdateUserUseCase(userRepository, auditLogger)
    val patchUserUseCase = PatchUserUseCase(userRepository, auditLogger)
    val deleteUserUseCase = DeleteUserUseCase(userRepository, auditLogger)

    val getGameSettingsUseCase = GetGameSettingsUseCase(gameSettingsRepository)
    val updateGameSettingsUseCase = UpdateGameSettingsUseCase(gameSettingsRepository, auditLogger)
    val patchGameSettingsUseCase = PatchGameSettingsUseCase(gameSettingsRepository, auditLogger)

    val recordGameResultUseCase = RecordGameResultUseCase(gameResultRepository, gameSettingsRepository)
    val editGameResultUseCase = EditGameResultUseCase(gameResultRepository, gameSettingsRepository, auditLogger)
    val patchGameResultUseCase = PatchGameResultUseCase(gameResultRepository, gameSettingsRepository, auditLogger)
    val deleteGameResultUseCase = DeleteGameResultUseCase(gameResultRepository, auditLogger)
    val getGameResultUseCase = GetGameResultUseCase(gameResultRepository)
    val getUserStatsUseCase = GetUserStatsUseCase(gameResultRepository)
    val getRankingUseCase = GetRankingUseCase(userRepository)
    val getAdvancePaymentUseCase = GetAdvancePaymentUseCase(advancePaymentRepository)
    val upsertAdvancePaymentUseCase = UpsertAdvancePaymentUseCase(advancePaymentRepository, auditLogger)

    val registerShiftUseCase = RegisterShiftUseCase(shiftRepository)
    val editShiftUseCase = EditShiftUseCase(shiftRepository, auditLogger)
    val patchShiftUseCase = PatchShiftUseCase(shiftRepository, auditLogger)
    val deleteShiftUseCase = DeleteShiftUseCase(shiftRepository, auditLogger)
    val getMonthlyShiftUseCase = GetMonthlyShiftUseCase(shiftRepository)
    val getDailyShiftUseCase = GetDailyShiftUseCase(shiftRepository)
    val getShiftRangeUseCase = GetShiftRangeUseCase(shiftRepository)
    val getShiftStatsUseCase = GetShiftStatsUseCase(shiftRepository)

    val calculateMonthlySalaryUseCase = CalculateMonthlySalaryUseCase(
        shiftRepository = shiftRepository,
        settingsRepository = gameSettingsRepository,
        gameResultRepository = gameResultRepository,
        advancePaymentRepository = advancePaymentRepository,
        timeZone = TimeZone.currentSystemDefault()
    )
    val getDashboardSummaryUseCase = GetDashboardSummaryUseCase(
        shiftRepository = shiftRepository,
        calculateMonthlySalaryUseCase = calculateMonthlySalaryUseCase,
        getUserStatsUseCase = getUserStatsUseCase,
        timeZone = TimeZone.currentSystemDefault()
    )

    val loginAttemptTracker = LoginAttemptTracker()
    val getStoreListUseCase = GetStoreListUseCase(storeMasterRepository)
    val getPrefectureListUseCase = GetPrefectureListUseCase(prefectureRepository)

    val rootRoutes = RootRoutes(
        createUserUseCase = createUserUseCase,
        loginUserUseCase = loginUserUseCase,
        getUserUseCase = getUserUseCase,
        updateUserUseCase = updateUserUseCase,
        patchUserUseCase = patchUserUseCase,
        deleteUserUseCase = deleteUserUseCase,
        getGameSettingsUseCase = getGameSettingsUseCase,
        updateGameSettingsUseCase = updateGameSettingsUseCase,
        patchGameSettingsUseCase = patchGameSettingsUseCase,
        recordGameResultUseCase = recordGameResultUseCase,
        editGameResultUseCase = editGameResultUseCase,
        patchGameResultUseCase = patchGameResultUseCase,
        deleteGameResultUseCase = deleteGameResultUseCase,
        getGameResultUseCase = getGameResultUseCase,
        getUserStatsUseCase = getUserStatsUseCase,
        getRankingUseCase = getRankingUseCase,
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
        loginAttemptTracker = loginAttemptTracker,
        refreshAccessTokenUseCase = refreshAccessTokenUseCase,
        accessTokenExpiresInSec = jwtConfig.expiresInSec,
        getAdvancePaymentUseCase = getAdvancePaymentUseCase,
        upsertAdvancePaymentUseCase = upsertAdvancePaymentUseCase
    )

    configureMonitoring()
    configureHTTP()
    configureSerialization()

    routing {
        get("/") {
            call.respondText("Mahjong staff API is running")
        }
        with(rootRoutes) { installAll(authName = "auth-jwt") }
    }
}

private fun Application.initializeDatabase() {
    if (environment.config.propertyOrNull("ktor.database.url") != null) {
        DatabaseFactory.init(environment)
    } else {
        environment.log.warn("ktor.database configuration not found. Skipping DatabaseFactory.init for this environment.")
    }
}
