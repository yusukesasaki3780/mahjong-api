package com.example

import com.example.config.JwtConfig
import com.example.config.JwtProvider
import com.example.database.DatabaseFactory
import com.example.infrastructure.db.repository.*
import com.example.infrastructure.logging.AuditLogger
import com.example.presentation.routes.RootRoutes
import com.example.security.LoginAttemptTracker
import com.example.usecase.*
import com.example.usecase.advance.GetAdvancePaymentUseCase
import com.example.usecase.advance.UpsertAdvancePaymentUseCase
import com.example.usecase.auth.LoginUserUseCase
import com.example.usecase.auth.RefreshAccessTokenUseCase
import com.example.usecase.dashboard.GetDashboardSummaryUseCase
import com.example.usecase.game.*
import com.example.usecase.prefecture.GetPrefectureListUseCase
import com.example.usecase.salary.CalculateMonthlySalaryUseCase
import com.example.usecase.settings.*
import com.example.usecase.shift.*
import com.example.usecase.notification.DeleteNotificationUseCase
import com.example.usecase.notification.GetNotificationsUseCase
import com.example.usecase.notification.GetUnreadNotificationCountUseCase
import com.example.usecase.notification.MarkAllNotificationsReadUseCase
import com.example.usecase.notification.MarkNotificationReadUseCase
import com.example.usecase.store.GetAccessibleStoresUseCase
import com.example.usecase.store.GetStoreListUseCase
import com.example.usecase.user.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json

fun Application.module() {

    // ===========================================
    //  DB 初期化
    // ===========================================
    DatabaseFactory.init(environment)

    val jwtConfig = JwtConfig.from(environment.config)
    val jwtProvider = JwtProvider(jwtConfig)

    // ===========================================
    //  ★ CORS 設定（CloudFront SPA 対応 完全版）
    // ===========================================
    install(CORS) {
        // 本番 CloudFront ドメインを許可
        allowHost("app.zooappgames.com", schemes = listOf("https"))

        // ローカル開発の許可（必要なら）
        allowHost("localhost:5173", schemes = listOf("http"))

        allowCredentials = true

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.AccessControlAllowCredentials)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.UserAgent)
        allowHeader(HttpHeaders.CacheControl)
        allowHeader(HttpHeaders.Pragma)

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        // 最終的に必要なら anyHost を残す
        // ★ フロントの CloudFront に限定するなら OFF 推奨
        // anyHost() 
    }

    // ===========================================
    //  JWT 認証
    // ===========================================
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

    // ===========================================
    //  Repository & UseCase 初期化
    // ===========================================
    val userRepository = ExposedUserRepository()
    val userCredentialRepository = ExposedUserCredentialRepository()
    val refreshTokenRepository = ExposedRefreshTokenRepository()
    val auditRepository = ExposedAuditRepository()
    val auditLogger = AuditLogger(auditRepository, Json { encodeDefaults = true })
    val gameSettingsRepository = ExposedGameSettingsRepository()
    val gameResultRepository = ExposedGameResultRepository()
    val shiftRepository = ExposedShiftRepository()
    val specialHourlyWageRepository = ExposedSpecialHourlyWageRepository()
    val shiftRequirementRepository = ExposedShiftRequirementRepository()
    val notificationRepository = ExposedNotificationRepository()
    val adminPrivilegeGateway = ExposedAdminPrivilegeGateway()
    val storeMasterRepository = ExposedStoreMasterRepository()
    val prefectureRepository = ExposedPrefectureRepository()
    val advancePaymentRepository = ExposedAdvancePaymentRepository()

    val createDefaultGameSettingsUseCase = CreateDefaultGameSettingsUseCase(gameSettingsRepository)
    val createUserUseCase = CreateUserUseCase(
        userRepository,
        userCredentialRepository,
        storeMasterRepository,
        createDefaultGameSettingsUseCase
    )
    val loginUserUseCase = LoginUserUseCase(userRepository, userCredentialRepository, refreshTokenRepository, jwtProvider)
    val refreshAccessTokenUseCase = RefreshAccessTokenUseCase(userRepository, refreshTokenRepository, jwtProvider)
    val getUserUseCase = GetUserUseCase(userRepository)
    val updateUserUseCase = UpdateUserUseCase(userRepository, userCredentialRepository, auditLogger)
    val patchUserUseCase = PatchUserUseCase(userRepository, userCredentialRepository, auditLogger)
    val deleteUserUseCase = DeleteUserUseCase(userRepository, auditLogger)
    val deleteMyAccountUseCase = DeleteMyAccountUseCase(
        refreshTokenRepository = refreshTokenRepository,
        deleteUserUseCase = deleteUserUseCase
    )
    val listGeneralUsersUseCase = ListGeneralUsersUseCase(userRepository)
    val adminDeleteUserUseCase = AdminDeleteUserUseCase(userRepository, deleteUserUseCase)
    val adminResetUserPasswordUseCase = AdminResetUserPasswordUseCase(userRepository, userCredentialRepository)
    val adminRestoreUserUseCase = AdminRestoreUserUseCase(userRepository, auditLogger)
    val adminUpdateUserAdminFlagUseCase = AdminUpdateUserAdminFlagUseCase(
        userRepository,
        adminPrivilegeGateway
    )

    val getGameSettingsUseCase = GetGameSettingsUseCase(gameSettingsRepository)
    val updateGameSettingsUseCase = UpdateGameSettingsUseCase(gameSettingsRepository, auditLogger)
    val patchGameSettingsUseCase = PatchGameSettingsUseCase(gameSettingsRepository, auditLogger)

    val recordGameResultUseCase = RecordGameResultUseCase(gameResultRepository, gameSettingsRepository)
    val editGameResultUseCase = EditGameResultUseCase(gameResultRepository, gameSettingsRepository, auditLogger)
    val patchGameResultUseCase = PatchGameResultUseCase(gameResultRepository, gameSettingsRepository, auditLogger)
    val deleteGameResultUseCase = DeleteGameResultUseCase(gameResultRepository, auditLogger)
    val getGameResultUseCase = GetGameResultUseCase(gameResultRepository)
    val getUserStatsUseCase = GetUserStatsUseCase(gameResultRepository)
    val startSimpleBatchUseCase = StartSimpleBatchUseCase(storeMasterRepository)
    val finishSimpleBatchUseCase = FinishSimpleBatchUseCase(gameResultRepository, gameSettingsRepository)
    val deleteSimpleBatchResultsUseCase = DeleteSimpleBatchResultsUseCase(gameResultRepository)
    val getRankingUseCase = GetRankingUseCase(userRepository)
    val getMyRankingUseCase = GetMyRankingUseCase(userRepository)
    val getAdvancePaymentUseCase = GetAdvancePaymentUseCase(advancePaymentRepository)
    val upsertAdvancePaymentUseCase = UpsertAdvancePaymentUseCase(advancePaymentRepository, auditLogger)

    val shiftNotificationService = ShiftNotificationService(userRepository, notificationRepository)
    val shiftPermissionService = ShiftPermissionService()
    val shiftContextProvider = ShiftContextProvider(userRepository, shiftRepository, storeMasterRepository)
    val registerShiftUseCase = RegisterShiftUseCase(shiftRepository, specialHourlyWageRepository, shiftContextProvider, shiftNotificationService, shiftPermissionService)
    val editShiftUseCase = EditShiftUseCase(shiftRepository, specialHourlyWageRepository, auditLogger, shiftNotificationService, shiftContextProvider, shiftPermissionService)
    val patchShiftUseCase = PatchShiftUseCase(shiftRepository, specialHourlyWageRepository, auditLogger, shiftNotificationService, shiftContextProvider, shiftPermissionService)
    val deleteShiftUseCase = DeleteShiftUseCase(shiftRepository, auditLogger, shiftNotificationService, shiftContextProvider, shiftPermissionService)
    val getMonthlyShiftUseCase = GetMonthlyShiftUseCase(shiftRepository, shiftContextProvider, shiftPermissionService)
    val getDailyShiftUseCase = GetDailyShiftUseCase(shiftRepository, shiftContextProvider, shiftPermissionService)
    val getShiftRangeUseCase = GetShiftRangeUseCase(shiftRepository, shiftContextProvider, shiftPermissionService)
    val getShiftStatsUseCase = GetShiftStatsUseCase(shiftRepository, TimeZone.currentSystemDefault(), shiftContextProvider, shiftPermissionService)
    val getShiftBoardUseCase = GetShiftBoardUseCase(userRepository, shiftRepository, shiftRequirementRepository, shiftContextProvider, shiftPermissionService)
    val upsertShiftRequirementUseCase = UpsertShiftRequirementUseCase(shiftRequirementRepository)
    val getNotificationsUseCase = GetNotificationsUseCase(notificationRepository)
    val markNotificationReadUseCase = MarkNotificationReadUseCase(notificationRepository)
    val markAllNotificationsReadUseCase = MarkAllNotificationsReadUseCase(notificationRepository)
    val deleteNotificationUseCase = DeleteNotificationUseCase(notificationRepository)
    val getUnreadNotificationCountUseCase = GetUnreadNotificationCountUseCase(notificationRepository)

    val listSpecialHourlyWagesUseCase = ListSpecialHourlyWagesUseCase(specialHourlyWageRepository)
    val createSpecialHourlyWageUseCase = CreateSpecialHourlyWageUseCase(specialHourlyWageRepository)
    val deleteSpecialHourlyWageUseCase = DeleteSpecialHourlyWageUseCase(specialHourlyWageRepository)

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
    val getAccessibleStoresUseCase = GetAccessibleStoresUseCase(userRepository, storeMasterRepository, shiftRepository)
    val getPrefectureListUseCase = GetPrefectureListUseCase(prefectureRepository)

    val rootRoutes = RootRoutes(
        createUserUseCase,
        loginUserUseCase,
        getUserUseCase,
        updateUserUseCase,
        patchUserUseCase,
        deleteUserUseCase,
        deleteMyAccountUseCase,
        listGeneralUsersUseCase,
        adminDeleteUserUseCase,
        adminResetUserPasswordUseCase,
        adminRestoreUserUseCase,
        adminUpdateUserAdminFlagUseCase,
        getGameSettingsUseCase,
        updateGameSettingsUseCase,
        patchGameSettingsUseCase,
        listSpecialHourlyWagesUseCase,
        createSpecialHourlyWageUseCase,
        deleteSpecialHourlyWageUseCase,
        recordGameResultUseCase,
        editGameResultUseCase,
        patchGameResultUseCase,
        deleteGameResultUseCase,
        getGameResultUseCase,
        getUserStatsUseCase,
        startSimpleBatchUseCase,
        finishSimpleBatchUseCase,
        deleteSimpleBatchResultsUseCase,
        getRankingUseCase,
        getMyRankingUseCase,
        registerShiftUseCase,
        editShiftUseCase,
        patchShiftUseCase,
        deleteShiftUseCase,
        getMonthlyShiftUseCase,
        getShiftBoardUseCase,
        getDailyShiftUseCase,
        getShiftRangeUseCase,
        getShiftStatsUseCase,
        upsertShiftRequirementUseCase,
        getNotificationsUseCase,
        markNotificationReadUseCase,
        markAllNotificationsReadUseCase,
        deleteNotificationUseCase,
        getUnreadNotificationCountUseCase,
        calculateMonthlySalaryUseCase,
        getDashboardSummaryUseCase,
        getStoreListUseCase,
        getAccessibleStoresUseCase,
        getPrefectureListUseCase,
        loginAttemptTracker,
        refreshAccessTokenUseCase,
        jwtConfig.expiresInSec,
        getAdvancePaymentUseCase,
        upsertAdvancePaymentUseCase
    )

    // ===========================================
    //  その他 Ktor 設定
    // ===========================================
    configureMonitoring()
    // configureHTTP()
    configureSerialization()

    routing {
        get("/") {
            call.respondText("Mahjong staff API is running")
        }
        with(rootRoutes) { installAll(authName = "auth-jwt") }
    }
}
