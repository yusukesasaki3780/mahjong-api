package com.example.usecase.user

import com.example.common.error.DomainValidationException
import com.example.common.error.FieldError

/**
 * パスワード強度チェックをまとめたユーティリティ。
 * 8文字以上かつ4カテゴリ中3種類以上を含むことを検証します。
 */
object PasswordPolicy {

    private const val MIN_LENGTH = 8
    private val uppercase = Regex("[A-Z]")
    private val lowercase = Regex("[a-z]")
    private val digits = Regex("[0-9]")
    private val symbols = Regex("""[!@#${'$'}%^&*()_+\-={}\[\]|\\:;"'<>,.?/~`]""")
    private const val ERROR_MESSAGE =
        "パスワードは8文字以上で、英大文字・英小文字・数字・記号のうち3種類以上を含めてください。"

    /**
     * パスワードが最低文字数と多様性を満たしているかを検証する。
     */
    fun validate(password: String) {
        val categories = listOf(
            uppercase.containsMatchIn(password),
            lowercase.containsMatchIn(password),
            digits.containsMatchIn(password),
            symbols.containsMatchIn(password)
        ).count { it }
        if (password.length < MIN_LENGTH || categories < 3) {
            throw DomainValidationException(
                violations = listOf(
                    FieldError(
                        field = "password",
                        code = "WEAK_PASSWORD",
                        message = ERROR_MESSAGE
                    )
                ),
                message = ERROR_MESSAGE
            )
        }
    }
}
