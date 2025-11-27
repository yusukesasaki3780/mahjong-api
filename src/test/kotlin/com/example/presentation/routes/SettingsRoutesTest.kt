package com.example.presentation.routes

import com.example.TestFixtures
import com.example.presentation.dto.PatchGameSettingsRequest
import com.example.presentation.dto.UpdateGameSettingsRequest
import com.example.usecase.settings.GetGameSettingsUseCase
import com.example.usecase.settings.PatchGameSettingsUseCase
import com.example.usecase.settings.UpdateGameSettingsUseCase
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsRoutesTest : RoutesTestBase() {

    @Test
    fun `get settings returns data`() = testApplication {
        coEvery { getGameSettingsUseCase(1) } returns TestFixtures.gameSettings()
        installRoutes()
        val response = client.get("/users/1/settings") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `put settings updates all fields`() = testApplication {
        coEvery { updateGameSettingsUseCase(any<UpdateGameSettingsUseCase.Command>(), any()) } returns TestFixtures.gameSettings()
        installRoutes()
        val response = client.put("/users/1/settings") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                UpdateGameSettingsRequest(
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
                    transportPerShift = 500
                )
            ))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { updateGameSettingsUseCase(any(), any()) }
    }

    @Test
    fun `patch settings updates partial fields`() = testApplication {
        coEvery { patchGameSettingsUseCase(any<PatchGameSettingsUseCase.Command>(), any()) } returns TestFixtures.gameSettings()
        installRoutes()
        val response = client.patch("/users/1/settings") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(PatchGameSettingsRequest(yonmaGameFee = 450)))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { patchGameSettingsUseCase(any(), any()) }
    }
}

