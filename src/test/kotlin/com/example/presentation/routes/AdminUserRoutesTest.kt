package com.example.presentation.routes

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError
import com.example.presentation.dto.AdminPasswordResetRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
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

class AdminUserRoutesTest : RoutesTestBase() {

    @Test
    fun `non admin access is forbidden`() = testApplication {
        coEvery { getUserUseCase(1) } returns TestFixtures.user(1, isAdmin = false)
        installRoutes()

        val response = client.get("/admin/users") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `admin can list general users`() = testApplication {
        coEvery { getUserUseCase(1) } returns TestFixtures.user(1, isAdmin = true)
        coEvery { listGeneralUsersUseCase() } returns listOf(TestFixtures.user(2))
        installRoutes()

        val response = client.get("/admin/users") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { listGeneralUsersUseCase() }
    }

    @Test
    fun `admin delete user success`() = testApplication {
        coEvery { getUserUseCase(1) } returns TestFixtures.user(1, isAdmin = true)
        coEvery { adminDeleteUserUseCase(1, 2, any()) } returns true
        installRoutes()

        val response = client.delete("/admin/users/2") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        coVerify { adminDeleteUserUseCase(1, 2, any()) }
    }

    @Test
    fun `admin delete user handles validation error`() = testApplication {
        coEvery { getUserUseCase(1) } returns TestFixtures.user(1, isAdmin = true)
        coEvery { adminDeleteUserUseCase(1, 2, any()) } throws DomainValidationException(
            violations = listOf(FieldError("userId", "ADMIN_DELETE_FORBIDDEN", "管理者は削除できません。"))
        )
        installRoutes()

        val response = client.delete("/admin/users/2") {
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `admin reset password success`() = testApplication {
        coEvery { getUserUseCase(1) } returns TestFixtures.user(1, isAdmin = true)
        coEvery { adminResetUserPasswordUseCase(2, any()) } returns true
        installRoutes()

        val response = client.post("/admin/users/2/password-reset") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AdminPasswordResetRequest(newPassword = "StrongPass123!")))
            withAuth(userId = 1)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { adminResetUserPasswordUseCase(2, any()) }
    }
}

