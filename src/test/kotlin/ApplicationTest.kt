package com.example

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to "test-secret",
                "ktor.jwt.issuer" to "test-issuer",
                "ktor.jwt.audience" to "test-audience",
                "ktor.jwt.realm" to "test-realm",
                "ktor.jwt.expiresInSec" to "3600"
            )
        }
        application { module() }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
