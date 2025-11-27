package com.example.presentation.routes

import com.example.TestFixtures
import com.example.presentation.dto.PrefectureResponse
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
 * - `/prefectures` エンドポイントの挙動を確認するルートテストです。
 * - API が都道府県コードと名称を JSON で返すことを検証します。
 */
class PrefectureRoutesTest : RoutesTestBase() {

    @Test
    fun `prefectures endpoint returns list`() = testApplication {
        coEvery { getPrefectureListUseCase() } returns listOf(
            TestFixtures.prefecture("01", "北海道"),
            TestFixtures.prefecture("13", "東京都")
        )
        installRoutes()

        val response = client.get("/prefectures")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.decodeFromString<List<PrefectureResponse>>(response.bodyAsText())
        assertEquals(listOf("01", "13"), body.map { it.code })
    }
}
