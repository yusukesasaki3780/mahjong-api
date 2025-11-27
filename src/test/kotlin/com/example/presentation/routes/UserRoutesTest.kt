package com.example.presentation.routes

import com.example.TestFixtures
import com.example.presentation.dto.PatchUserRequest
import com.example.presentation.dto.UpdateUserRequest
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.common.error.ErrorResponse
import com.example.usecase.user.DeleteUserUseCase
import com.example.usecase.user.GetUserUseCase
import com.example.usecase.user.PatchUserUseCase
import com.example.usecase.user.UpdateUserUseCase
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserRoutesTest : RoutesTestBase() {

    @Test
    fun `get user success`() = testApplication {
        coEvery { getUserUseCase(1) } returns TestFixtures.user(1)
        installRoutes()

        val response = client.get("/users/1") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { getUserUseCase(1) }
    }

    @Test
    fun `get user not found`() = testApplication {
        coEvery { getUserUseCase(2) } returns null
        installRoutes()
        val response = client.get("/users/2") {
            withAuth(userId = 2)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `put user updates record`() = testApplication {
        coEvery { updateUserUseCase(any<UpdateUserUseCase.Command>(), any()) } returns TestFixtures.user()
        installRoutes()
        val response = client.put("/users/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateUserRequest("A", "B", "Store", "01", "a@example.com")))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { updateUserUseCase(any(), any()) }
    }

    @Test
    fun `patch user updates subset`() = testApplication {
        coEvery { patchUserUseCase(any<PatchUserUseCase.Command>(), any()) } returns TestFixtures.user().copy(name = "Patch")
        installRoutes()
        val response = client.patch("/users/1") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(PatchUserRequest(name = "Patch", email = "patch@example.com")))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { patchUserUseCase(any(), any()) }
    }

    @Test
    fun `delete user not found`() = testApplication {
        coEvery { deleteUserUseCase(3, any()) } returns false
        installRoutes()
        val response = client.delete("/users/3") {
            withAuth(userId = 3)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `invalid user id returns 400`() = testApplication {
        installRoutes()
        val response = client.get("/users/abc") {
            withAuth()
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertEquals("userId", error.errors.first().field)
    }

    @Test
    fun `missing token returns 401`() = testApplication {
        installRoutes()
        val response = client.get("/users/1")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `accessing other users resource returns 403`() = testApplication {
        installRoutes()
        val response = client.get("/users/1") {
            withAuth(userId = 2)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        val error = json.decodeFromString<ErrorResponse>(response.bodyAsText())
        assertEquals("FORBIDDEN", error.errorCode)
    }
}

