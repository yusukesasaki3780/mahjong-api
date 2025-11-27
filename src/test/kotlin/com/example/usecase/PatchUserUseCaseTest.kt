package com.example.usecase

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import com.example.usecase.user.PatchUserUseCase
import com.example.usecase.TestAuditSupport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException
import kotlin.test.assertFailsWith

class PatchUserUseCaseTest {

    private val repository = mockk<UserRepository>()
    private val useCase = PatchUserUseCase(repository, TestAuditSupport.auditLogger)

    @Test
    fun `updates only provided fields`() = runTest {
        val existing = sampleUser(name = "Old", nickname = "nick")
        coEvery { repository.findById(1) } returns existing
        coEvery { repository.patchUser(eq(1), any()) } answers { existing.copy(name = "New", storeName = "Store2") }

        val result = useCase(
            PatchUserUseCase.Command(
                userId = 1,
                name = "New",
                storeName = "Store2"
            ),
            TestAuditSupport.auditContext
        )

        assertEquals("New", result.name)
        assertEquals("Store2", result.storeName)
        coVerify { repository.patchUser(eq(1), any()) }
    }

    @Test
    fun `throws when user missing`() = runTest {
        coEvery { repository.findById(99) } returns null

        try {
            useCase(PatchUserUseCase.Command(userId = 99, name = "X"), TestAuditSupport.auditContext)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `invalid prefecture format fails`() = runTest {
        coEvery { repository.findById(1) } returns sampleUser(name = "Old", nickname = "nick")

        assertFailsWith<ConstraintViolationException> {
            useCase(PatchUserUseCase.Command(userId = 1, prefectureCode = "ABC"), TestAuditSupport.auditContext)
        }
    }

    private fun sampleUser(
        id: Long = 1,
        name: String,
        nickname: String,
        store: String = "Store",
        prefecture: String = "01",
        email: String = "user$id@example.com"
    ): User = User(
        id = id,
        name = name,
        nickname = nickname,
        storeName = store,
        prefectureCode = prefecture,
        email = email,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
