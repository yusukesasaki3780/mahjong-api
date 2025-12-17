package com.example.usecase.user

import com.example.TestFixtures
import com.example.domain.model.AuditContext
import com.example.domain.model.NotificationType
import com.example.domain.repository.AdminPrivilegeGateway
import com.example.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdminUpdateUserAdminFlagUseCaseTest {

    private val userRepository = mockk<UserRepository>()
    private val adminPrivilegeGateway = mockk<AdminPrivilegeGateway>()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-01-01T00:00:00Z")
    }
    private val useCase = AdminUpdateUserAdminFlagUseCase(
        userRepository = userRepository,
        adminPrivilegeGateway = adminPrivilegeGateway,
        clock = fixedClock
    )

    @Test
    fun `granting admin flag delegates to gateway with expected parameters`() = runTest {
        val admin = TestFixtures.user(id = 1, storeId = 10, isAdmin = true)
        val target = TestFixtures.user(id = 2, storeId = 10, isAdmin = false)
        val updated = target.copy(isAdmin = true)
        coEvery { userRepository.findById(target.id!!) } returns target
        coEvery {
            adminPrivilegeGateway.updateAdminFlag(
                adminId = admin.id!!,
                targetUserId = target.id!!,
                isAdmin = true,
                action = "USER_ADMIN_GRANTED",
                beforeJson = any(),
                afterJson = any(),
                notificationType = NotificationType.ADMIN_ROLE_GRANTED,
                message = any(),
                auditContext = any(),
                occurredAt = fixedClock.now()
            )
        } returns updated

        val result = useCase(
            adminId = admin.id!!,
            adminName = admin.name,
            adminStoreId = admin.storeId,
            targetUserId = target.id!!,
            isAdmin = true,
            auditContext = AuditContext(
                performedBy = admin.id!!,
                path = "/admin/users/${target.id}",
                ipAddress = "127.0.0.1"
            )
        )

        assertEquals(updated, result)
        coVerify {
            adminPrivilegeGateway.updateAdminFlag(
                adminId = admin.id!!,
                targetUserId = target.id!!,
                isAdmin = true,
                action = "USER_ADMIN_GRANTED",
                beforeJson = """{"is_admin":false}""",
                afterJson = """{"is_admin":true}""",
                notificationType = NotificationType.ADMIN_ROLE_GRANTED,
                message = "管理者 ${admin.name}によって、あなたに管理者権限が付与されました。",
                auditContext = any(),
                occurredAt = fixedClock.now()
            )
        }
    }

    @Test
    fun `revoking admin flag includes admin name in message`() = runTest {
        val admin = TestFixtures.user(id = 5, storeId = 20, isAdmin = true)
        val target = TestFixtures.user(id = 6, storeId = 20, isAdmin = true)
        val updated = target.copy(isAdmin = false)
        coEvery { userRepository.findById(target.id!!) } returns target
        coEvery { adminPrivilegeGateway.updateAdminFlag(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns updated

        useCase(
            adminId = admin.id!!,
            adminName = admin.name,
            adminStoreId = admin.storeId,
            targetUserId = target.id!!,
            isAdmin = false,
            auditContext = AuditContext(
                performedBy = admin.id!!,
                path = "/admin/users/${target.id}",
                ipAddress = "127.0.0.1"
            )
        )

        coVerify {
            adminPrivilegeGateway.updateAdminFlag(
                adminId = admin.id!!,
                targetUserId = target.id!!,
                isAdmin = false,
                action = "USER_ADMIN_REVOKED",
                beforeJson = """{"is_admin":true}""",
                afterJson = """{"is_admin":false}""",
                notificationType = NotificationType.ADMIN_ROLE_REVOKED,
                message = "管理者 ${admin.name}によって、あなたの管理者権限が解除されました。",
                auditContext = any(),
                occurredAt = fixedClock.now()
            )
        }
    }
}
