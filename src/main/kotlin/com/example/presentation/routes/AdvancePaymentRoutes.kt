package com.example.presentation.routes

import com.example.presentation.dto.UpsertAdvancePaymentRequest
import com.example.presentation.dto.AdvancePaymentResponse
import com.example.usecase.advance.GetAdvancePaymentUseCase
import com.example.usecase.advance.UpsertAdvancePaymentUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

fun Route.installAdvancePaymentRoutes(
    getAdvancePaymentUseCase: GetAdvancePaymentUseCase,
    upsertAdvancePaymentUseCase: UpsertAdvancePaymentUseCase
) {
    route("/users/{userId}/advance") {
        get("/{yearMonth}") {
            val userId = call.userIdOrNull() ?: return@get call.respondInvalidUserId()
            call.requireUserAccess(userId) ?: return@get
            val yearMonth = call.pathYearMonthOrNull("yearMonth")
                ?: return@get call.respondInvalidYearMonth("yearMonth")

            val result = getAdvancePaymentUseCase(userId, yearMonth)
            call.respond(AdvancePaymentResponse.from(result))
        }

        put("/{yearMonth}") {
            val userId = call.userIdOrNull() ?: return@put call.respondInvalidUserId()
            val auditContext = call.requireAuditContext(userId) ?: return@put
            val yearMonth = call.pathYearMonthOrNull("yearMonth")
                ?: return@put call.respondInvalidYearMonth("yearMonth")
            val request = call.receive<UpsertAdvancePaymentRequest>()

            val result = upsertAdvancePaymentUseCase(
                UpsertAdvancePaymentUseCase.Command(
                    userId = userId,
                    yearMonth = yearMonth,
                    amount = request.amount,
                    auditContext = auditContext
                )
            )
            call.respond(HttpStatusCode.OK, AdvancePaymentResponse.from(result))
        }
    }
}
