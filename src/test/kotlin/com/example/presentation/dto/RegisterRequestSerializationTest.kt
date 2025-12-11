package com.example.presentation.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterRequestSerializationTest {

    @Test
    fun `serializes all fields including passwordConfirm`() {
        val request = RegisterRequest(
            name = "佐々木由紀子",
            nickname = "ゆっこ",
            storeName = "麻雀ZOO 新宿本店",
            prefectureCode = "13",
            email = "sasaki@example.com",
            zooId = 1234,
            password = "SecurePass123!",
            passwordConfirm = "SecurePass123!"
        )

        val json = Json.encodeToString(request)

        assertTrue(json.contains("passwordConfirm"))
        assertTrue(json.contains("sasaki@example.com"))
        assertTrue(!json.contains("unknown"))
    }
}

