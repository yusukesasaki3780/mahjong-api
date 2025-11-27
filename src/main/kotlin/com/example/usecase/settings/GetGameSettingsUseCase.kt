package com.example.usecase.settings

/**
 * ### このファイルの役割
 * - 指定ユーザーのゲーム設定を取得する単純なユースケースです。
 * - 存在しない場合は null を返し、ルート層で 404 を返せるようにしています。
 */

import com.example.domain.model.GameSettings
import com.example.domain.repository.GameSettingsRepository

/**
 * ゲーム設定取得ユースケース。
 */
class GetGameSettingsUseCase(
    private val repository: GameSettingsRepository
) {

    suspend operator fun invoke(userId: Long): GameSettings? =
        repository.getSettings(userId)
}

