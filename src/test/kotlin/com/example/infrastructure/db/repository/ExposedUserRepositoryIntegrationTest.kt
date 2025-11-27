package com.example.infrastructure.db.repository

import com.example.domain.model.User
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExposedUserRepositoryIntegrationTest : RepositoryTestBase() {

    private val repository = ExposedUserRepository()

    @Test
    fun `create and fetch user`() = runDbTest {
        val created = repository.createUser(sampleUser(name = "Alice"))
        val fetched = repository.findById(created.id!!)
        assertEquals("Alice", fetched?.name)
        assertEquals(created.id, fetched?.id)
    }

    @Test
    fun `patch user updates only provided fields`() = runDbTest {
        val created = repository.createUser(sampleUser(name = "Bob", nickname = "b", store = "A"))
        val patched = repository.patchUser(
            created.id!!,
            com.example.domain.repository.UserPatch(
                name = "Bobby",
                nickname = null,
                storeName = "NewStore"
            )
        )
        assertEquals("Bobby", patched.name)
        assertEquals("b", patched.nickname) // unchanged
        assertEquals("NewStore", patched.storeName)
    }

    @Test
    fun `delete removes user`() = runDbTest {
        val created = repository.createUser(sampleUser(name = "Charlie"))
        val createdId = created.id!!
        val deleted = repository.deleteUser(createdId)
        assertTrue(deleted)
        val fetched = repository.findById(createdId)
        assertNull(fetched)
    }

    private fun sampleUser(
        name: String,
        nickname: String = "nick",
        store: String = "Store",
        prefecture: String = "01",
        email: String = "$name@example.com"
    ): User {
        val now = Clock.System.now()
        return User(
            id = null,
            name = name,
            nickname = nickname,
            storeName = store,
            prefectureCode = prefecture,
            email = email,
            createdAt = now,
            updatedAt = now
        )
    }
}
