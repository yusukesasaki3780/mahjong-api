package com.example.usecase.auth

import com.example.TestFixtures
import com.example.config.JwtTokenResult
import com.example.config.JwtProvider
import com.example.domain.model.RefreshToken
import com.example.domain.repository.RefreshTokenRepository
import com.example.domain.repository.UserCredentialRepository
import com.example.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class LoginUserUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val credentialRepository = mockk<UserCredentialRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val jwtProvider = mockk<JwtProvider>()
    private val useCase = LoginUserUseCase(
        userRepository,
        credentialRepository,
        refreshTokenRepository,
        jwtProvider,
        refreshTokenTtlDays = 30
    )

    @Test
    fun `login succeeds with email`() = runTest {
        val user = TestFixtures.user()
        coEvery { userRepository.findByEmail("user1@example.com") } returns user
        coEvery { credentialRepository.verifyPassword(user.id!!, "pw") } returns true
        coEvery { refreshTokenRepository.deleteExpired(any()) } returns Unit
        coEvery { refreshTokenRepository.deleteAllForUser(user.id!!) } returns Unit
        coEvery { refreshTokenRepository.create(any(), any(), any()) } returns RefreshToken(
            id = 1,
            userId = user.id!!,
            tokenHash = "hash",
            expiresAt = Clock.System.now(),
            createdAt = Clock.System.now()
        )
        every { jwtProvider.generateToken(user.id!!) } returns JwtTokenResult("token", Clock.System.now())

        val result = useCase(LoginUserUseCase.Command(email = "user1@example.com", password = "pw"))

        assertEquals("token", result.token)
        assertEquals(user.id, result.user.id)
        coVerify { userRepository.findByEmail("user1@example.com") }
        coVerify { credentialRepository.verifyPassword(user.id!!, "pw") }
    }

    @Test
    fun `invalid password throws error`() = runTest {
        val user = TestFixtures.user()
        coEvery { userRepository.findByEmail("user1@example.com") } returns user
        coEvery { credentialRepository.verifyPassword(user.id!!, "bad") } returns false

        assertFailsWith<LoginUserUseCase.InvalidCredentialsException> {
            useCase(LoginUserUseCase.Command(email = "user1@example.com", password = "bad"))
        }
    }

    @Test
    fun `deleted account throws dedicated error`() = runTest {
        val user = TestFixtures.user(isDeleted = true)
        coEvery { userRepository.findByEmail("user1@example.com") } returns user

        assertFailsWith<LoginUserUseCase.DeletedAccountException> {
            useCase(LoginUserUseCase.Command(email = "user1@example.com", password = "pw"))
        }
    }
}
