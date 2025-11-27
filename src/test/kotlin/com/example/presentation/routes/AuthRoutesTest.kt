package com.example.presentation.routes

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.common.error.ErrorResponse
import com.example.common.error.FieldError
import com.example.presentation.dto.RegisterRequest
import com.example.presentation.dto.ValidationMessagesResponse
import com.example.security.LoginAttemptTracker
import com.example.usecase.auth.LoginUserUseCase
import com.example.usecase.auth.RefreshAccessTokenUseCase
import com.example.usecase.user.CreateUserUseCase
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthRoutesTest : RoutesTestBase() {

    override fun loginAttemptTracker() = LoginAttemptTracker(firstThreshold = 2, secondThreshold = 3)

    @Test
    fun `register user returns 201`() = testApplication {
        val user = TestFixtures.user()
        coEvery { createUserUseCase(any<CreateUserUseCase.Command>()) } returns user
        installRoutes()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterRequest(
                        name = "Alice",
                        nickname = "ali",
                        storeName = "Store",
                        prefectureCode = "01",
                        email = "alice@example.com",
                        password = "SecretPass123!",
                        passwordConfirm = "SecretPass123!"
                    )
                )
            )
        }
        assertTrue(response.status == HttpStatusCode.Created)
        coVerify { createUserUseCase(any()) }
    }

    @Test
    fun `password mismatch returns 400`() = testApplication {
        coEvery { createUserUseCase(any<CreateUserUseCase.Command>()) } throws DomainValidationException(
            violations = listOf(
                FieldError(
                    field = "passwordConfirm",
                    code = "PASSWORD_NOT_MATCH",
                    message = "確認用パスワードが一致しません"
                )
            )
        )
        installRoutes()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterRequest(
                        name = "Alice",
                        nickname = "ali",
                        storeName = "Store",
                        prefectureCode = "01",
                        email = "alice@example.com",
                        password = "SecretPass123!",
                        passwordConfirm = "Mismatch123!"
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.decodeFromString<ValidationMessagesResponse>(response.bodyAsText())
        assertTrue(body.errors.any { it.message.contains("確認用パスワード") })
    }

    @Test
    fun `login issues token`() = testApplication {
        val result = LoginUserUseCase.Result(
            TestFixtures.user(),
            "token",
            "refresh-token",
            kotlinx.datetime.Clock.System.now()
        )
        coEvery { loginUserUseCase.invoke(any()) } returns result
        installRoutes()

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user1@example.com","password":"pw"}""")
        }
        assertTrue(response.status == HttpStatusCode.OK)
        assertTrue(response.bodyAsText().contains("token"))
        coVerify { loginUserUseCase.invoke(any()) }
    }

    @Test
    fun `refresh endpoint issues new tokens`() = testApplication {
        val result = LoginUserUseCase.Result(
            TestFixtures.user(),
            "token2",
            "refresh-token-2",
            kotlinx.datetime.Clock.System.now()
        )
        coEvery { refreshAccessTokenUseCase.invoke(any<RefreshAccessTokenUseCase.Command>()) } returns result
        installRoutes()

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"old-token","userId":1}""")
        }
        assertTrue(response.status == HttpStatusCode.OK)
        val body = response.bodyAsText()
        assertTrue(body.contains("accessToken"))
        assertTrue(body.contains("refreshToken"))
        assertTrue(body.contains("token2"))
        coVerify { refreshAccessTokenUseCase.invoke(any()) }
    }

    @Test
    fun `refresh invalid token returns 401`() = testApplication {
        coEvery { refreshAccessTokenUseCase.invoke(any<RefreshAccessTokenUseCase.Command>()) } throws IllegalArgumentException("invalid")
        installRoutes()

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"bad","userId":1}""")
        }
        assertTrue(response.status == HttpStatusCode.Unauthorized)
    }

    @Test
    fun `login failure returns generic message`() = testApplication {
        coEvery { loginUserUseCase.invoke(any()) } throws LoginUserUseCase.InvalidCredentialsException()
        installRoutes()

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user1@example.com","password":"pw"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val raw = response.bodyAsText()
        val body = Json.decodeFromString<ErrorResponse>(raw)
        assertEquals("ID またはパスワードが不正です。", body.message, "Actual response: $raw")
    }

    @Test
    fun `login lock returns 429 after repeated failures`() = testApplication {
        coEvery { loginUserUseCase.invoke(any()) } throws LoginUserUseCase.InvalidCredentialsException()
        installRoutes()

        repeat(2) {
            client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"user1@example.com","password":"pw"}""")
            }
        }
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user1@example.com","password":"pw"}""")
        }
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertTrue(response.bodyAsText().contains("一定回数ログインに失敗したため、しばらくログインできません。"))
    }
}
