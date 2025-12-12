package com.example.usecase

import com.example.TestFixtures
import com.example.domain.repository.UserCredentialRepository
import com.example.domain.repository.UserRepository
import com.example.usecase.user.UpdateUserUseCase
import com.example.usecase.TestAuditSupport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import kotlin.test.assertFailsWith

class UpdateUserUseCaseTest {

    private val repository = mockk<UserRepository>()
    private val credentialRepository = mockk<UserCredentialRepository>()
    private val useCase = UpdateUserUseCase(repository, credentialRepository, TestAuditSupport.auditLogger)

    @Test
    fun `updates user when valid`() = runTest {
        val current = TestFixtures.user()
        coEvery { repository.findById(1) } returns current
        coEvery { repository.updateUser(any()) } returns current.copy(name = "Bob")

        val result = useCase(
            UpdateUserUseCase.Command(
                userId = 1,
                name = "Bob",
                nickname = "bob",
                storeName = "Store",
                prefectureCode = "13",
                email = current.email
            ),
            TestAuditSupport.auditContext
        )

        assertEquals("Bob", result.name)
        coVerify { repository.updateUser(any()) }
    }

    @Test
    fun `invalid prefecture throws`() = runTest {
        coEvery { repository.findById(1) } returns TestFixtures.user()

        assertFailsWith<ConstraintViolationException> {
            useCase(
                UpdateUserUseCase.Command(
                    userId = 1,
                    name = "Bob",
                    nickname = "bob",
                    storeName = "Store",
                    prefectureCode = "ABC",
                    email = "bob@example.com"
                ),
                TestAuditSupport.auditContext
            )
        }
    }
}
