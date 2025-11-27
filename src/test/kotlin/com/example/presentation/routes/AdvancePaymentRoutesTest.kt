package com.example.presentation.routes

import com.example.presentation.dto.UpsertAdvancePaymentRequest
import com.example.usecase.advance.GetAdvancePaymentUseCase
import com.example.usecase.advance.UpsertAdvancePaymentUseCase
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import java.time.YearMonth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdvancePaymentRoutesTest : RoutesTestBase() {

    @Test
    fun `get advance payment returns amount`() = testApplication {
        coEvery { getAdvancePaymentUseCase(1, YearMonth.parse("2025-11")) } returns
            GetAdvancePaymentUseCase.Result(userId = 1, yearMonth = YearMonth.parse("2025-11"), amount = 1500.0)
        installRoutes()

        val response = client.get("/users/1/advance/2025-11") {
            withAuth(userId = 1)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assert(response.bodyAsText().contains("1500"))
    }

    @Test
    fun `put advance payment upserts amount`() = testApplication {
        coEvery { upsertAdvancePaymentUseCase(any()) } returns
            UpsertAdvancePaymentUseCase.Result(userId = 1, yearMonth = YearMonth.parse("2025-11"), amount = 1200.0)
        installRoutes()

        val response = client.put("/users/1/advance/2025-11") {
            withAuth(userId = 1)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpsertAdvancePaymentRequest(amount = 1200.0)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { upsertAdvancePaymentUseCase(any()) }
    }
}
