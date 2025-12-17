package com.example.common.error

/**
 * 権限不足を通知する例外。
 * StatusPages 側で 403 を返すために利用する。
 */
class AccessDeniedException(message: String) : RuntimeException(message)

