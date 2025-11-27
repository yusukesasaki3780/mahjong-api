package com.example.presentation.routes

import com.example.TestFixtures
import com.example.presentation.dto.StoreResponse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ### このファイルの役割
 * - `/stores` エンドポイントの挙動を確認するルートテストです。
 * - Store 一覧が正しく JSON で返るかを検証します。
 */
class StoreRoutesTest : RoutesTestBase() {

    @Test
    fun `stores endpoint returns list`() = testApplication {
        coEvery { getStoreListUseCase() } returns listOf(
            TestFixtures.store(id = 1, name = "ZOO新宿"),
            TestFixtures.store(id = 2, name = "ZOO池袋")
        )
        installRoutes()

        val response = client.get("/stores")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.decodeFromString<List<StoreResponse>>(response.bodyAsText())
        assertEquals(listOf(1L, 2L), body.map { it.id })
    }
}
