package com.example.presentation.routes

import com.example.presentation.dto.PrefectureResponse
import com.example.usecase.prefecture.GetPrefectureListUseCase
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * ### このファイルの役割
 * - 都道府県マスターを返す `/prefectures` エンドポイントを定義します。
 * - フロントエンドが 47 都道府県のリストを取得する用途を想定しています。
 */
fun Route.installPrefectureRoutes(
    getPrefectureListUseCase: GetPrefectureListUseCase
) {
    get("/prefectures") {
        val prefectures = getPrefectureListUseCase()
        call.respond(prefectures.map(PrefectureResponse::from))
    }
}
