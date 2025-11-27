package com.example.domain.repository

import com.example.domain.model.GameSettings

/**
 * ゲーム設定や給与計算設定を扱うリポジトリ。
 */
interface GameSettingsRepository {

    /**
     * ユーザの設定を取得し、未登録なら null を返す。
     */
    suspend fun getSettings(userId: Long): GameSettings?

    /**
     * ユーザの設定を更新して保存結果を返す。
     */
    suspend fun updateSettings(userId: Long, settings: GameSettings): GameSettings

    suspend fun patchSettings(userId: Long, patch: GameSettingsPatch): GameSettings
    suspend fun findById(id: Long): GameSettings?
}
