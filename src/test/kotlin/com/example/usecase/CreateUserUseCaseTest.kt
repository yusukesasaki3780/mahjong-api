package com.example.usecase

import com.example.TestFixtures
import com.example.common.error.DomainValidationException
import com.example.domain.repository.UserCredentialRepository
import com.example.domain.repository.UserRepository
import com.example.usecase.settings.CreateDefaultGameSettingsUseCase
import com.example.usecase.user.CreateUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import kotlin.test.assertFailsWith

class CreateUserUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val credentialRepository = mockk<UserCredentialRepository>()
    private val createDefaultSettingsUseCase = mockk<CreateDefaultGameSettingsUseCase>(relaxed = true)
    private val useCase = CreateUserUseCase(userRepository, credentialRepository, createDefaultSettingsUseCase)

    @Test
    fun `creates user when command valid`() = runTest {
        val created = TestFixtures.user()
        coEvery { userRepository.findByEmail("alice@example.com") } returns null
        coEvery { userRepository.findByZooId(1234) } returns null
        coEvery { userRepository.createUser(any()) } returns created
        coEvery { credentialRepository.createCredentials(created.id!!, "alice@example.com", any()) } returns Unit
        coEvery { createDefaultSettingsUseCase(created.id!!) } returns Unit

        val result = useCase(
            CreateUserUseCase.Command(
                name = "Alice",
                nickname = "ali",
                storeName = "Mahjong",
                prefectureCode = "13",
                email = "alice@example.com",
                zooId = 1234,
                password = "StrongPass123!",
                passwordConfirm = "StrongPass123!"
            )
        )

        assertEquals(created.id, result.id)
        coVerify { userRepository.createUser(any()) }
        coVerify { credentialRepository.createCredentials(created.id!!, "alice@example.com", any()) }
        coVerify { createDefaultSettingsUseCase(created.id!!) }
    }

    @Test
    fun `invalid prefecture throws validation error`() = runTest {
        coEvery { userRepository.findByEmail(any()) } returns null
        coEvery { userRepository.findByZooId(any()) } returns null
        assertFailsWith<ConstraintViolationException> {
            useCase(
                CreateUserUseCase.Command(
                    name = "Alice",
                    nickname = "ali",
                    storeName = "Mahjong",
                    prefectureCode = "ABC",
                    email = "alice@example.com",
                    zooId = 1234,
                    password = "StrongPass123!",
                    passwordConfirm = "StrongPass123!"
                )
            )
        }
    }

    @Test
    fun `password mismatch throws domain validation error`() = runTest {
        assertFailsWith<DomainValidationException> {
            useCase(
                CreateUserUseCase.Command(
                    name = "Alice",
                    nickname = "ali",
                    storeName = "Mahjong",
                    prefectureCode = "13",
                    email = "alice@example.com",
                    zooId = 1234,
                    password = "StrongPass123!",
                    passwordConfirm = "differentPass"
                )
            )
        }
    }

    @Test
    fun `duplicate email throws domain validation error`() = runTest {
        coEvery { userRepository.findByEmail("alice@example.com") } returns TestFixtures.user()
        coEvery { userRepository.findByZooId(any()) } returns null
        assertFailsWith<DomainValidationException> {
            useCase(
                CreateUserUseCase.Command(
                    name = "Alice",
                    nickname = "ali",
                    storeName = "Mahjong",
                    prefectureCode = "13",
                    email = "alice@example.com",
                    zooId = 1234,
                    password = "StrongPass123!",
                    passwordConfirm = "StrongPass123!"
                )
            )
        }
    }

    @Test
    fun `duplicate zoo id throws domain validation error`() = runTest {
        coEvery { userRepository.findByEmail(any()) } returns null
        coEvery { userRepository.findByZooId(1234) } returns TestFixtures.user()
        assertFailsWith<DomainValidationException> {
            useCase(
                CreateUserUseCase.Command(
                    name = "Alice",
                    nickname = "ali",
                    storeName = "Mahjong",
                    prefectureCode = "13",
                    email = "alice@example.com",
                    zooId = 1234,
                    password = "StrongPass123!",
                    passwordConfirm = "StrongPass123!"
                )
            )
        }
    }

    @Test
    fun `weak password throws domain validation error`() = runTest {
        coEvery { userRepository.findByEmail(any()) } returns null
        coEvery { userRepository.findByZooId(any()) } returns null
        assertFailsWith<DomainValidationException> {
            useCase(
                CreateUserUseCase.Command(
                    name = "Alice",
                    nickname = "ali",
                    storeName = "Mahjong",
                    prefectureCode = "13",
                    email = "alice@example.com",
                    zooId = 1234,
                    password = "alllowercase123",
                    passwordConfirm = "alllowercase123"
                )
            )
        }
    }
}
