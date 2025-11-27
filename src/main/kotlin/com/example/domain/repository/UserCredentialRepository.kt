package com.example.domain.repository

/**
 * ユーザの認証情報を安全に管理するためのリポジトリ。
 */
interface UserCredentialRepository {

    /**
     * 新しいユーザ ID に対してハッシュ化済みパスワードを保存する。
     */
    suspend fun createCredentials(userId: Long, email: String, passwordHash: String)

    /**
     * 平文パスワードが保存済みハッシュと一致するか検証する。
     */
    suspend fun verifyPassword(userId: Long, password: String): Boolean

    /**
     * 既存ユーザのパスワードハッシュを更新する。
     */
    suspend fun updatePassword(userId: Long, newPassword: String)
}
