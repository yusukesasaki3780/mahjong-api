package com.example.usecase

import com.example.TestFixtures
import com.example.domain.repository.AuditRepository
import com.example.domain.repository.GameResultRepository
import com.example.infrastructure.logging.AuditLogger
import com.example.usecase.game.DeleteGameResultUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteGameResultUseCaseTest {

    private val repository: GameResultRepository = mockk()
    private val auditRepository: AuditRepository = mockk(relaxed = true)
    private val auditLogger: AuditLogger = AuditLogger(auditRepository, kotlinx.serialization.json.Json { encodeDefaults = true })
    private val useCase = DeleteGameResultUseCase(repository, auditLogger)

    @Test
    fun `delete logs audit entry when removal succeeds`() = runTest {
        val before = TestFixtures.gameResult(id = 10)
        coEvery { repository.findById(10) } returns before
        coEvery { repository.deleteGameResult(10) } returns true

        val result = useCase(10, TestAuditSupport.auditContext)

        assertTrue(result)
        val recorded = slot<com.example.domain.model.AuditEntry>()
        coVerify { auditRepository.record(capture(recorded)) }
        val entry = recorded.captured
        assertEquals("GAME_RESULT", entry.entityType)
        assertEquals(10, entry.entityId)
        assertEquals("DELETE", entry.action)
        assertEquals(TestAuditSupport.auditContext.performedBy, entry.performedBy)
    }

    @Test
    fun `delete returns false and skips audit when record missing`() = runTest {
        coEvery { repository.findById(99) } returns null

        val result = useCase(99, TestAuditSupport.auditContext)

        assertFalse(result)
        coVerify(exactly = 0) { auditRepository.record(any()) }
    }
}
