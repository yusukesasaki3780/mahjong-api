package com.example.usecase.store

import com.example.TestFixtures
import com.example.domain.repository.ShiftRepository
import com.example.domain.repository.StoreMasterRepository
import com.example.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GetAccessibleStoresUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val storeRepository = mockk<StoreMasterRepository>()
    private val shiftRepository = mockk<ShiftRepository>()
    private val useCase = GetAccessibleStoresUseCase(userRepository, storeRepository, shiftRepository)

    @Test
    fun `admin receives all stores`() = runTest {
        val admin = TestFixtures.user(id = 1, storeId = 1, isAdmin = true)
        coEvery { userRepository.findById(1) } returns admin
        coEvery { storeRepository.getAll() } returns listOf(
            TestFixtures.store(id = 1, name = "StoreA"),
            TestFixtures.store(id = 2, name = "StoreB")
        )

        val result = useCase(1)
        assertEquals(2, result.size)
    }

    @Test
    fun `general user gets own and help stores`() = runTest {
        val user = TestFixtures.user(id = 2, storeId = 10, isAdmin = false)
        coEvery { userRepository.findById(2) } returns user
        coEvery { shiftRepository.getStoreIdsForUser(2) } returns setOf(11, 12)
        coEvery { storeRepository.findByIds(setOf(10, 11, 12)) } returns listOf(
            TestFixtures.store(id = 11, name = "HelpA"),
            TestFixtures.store(id = 10, name = "Home")
        )

        val result = useCase(2)
        assertEquals(listOf(11L, 10L), result.map { it.id })
    }
}
