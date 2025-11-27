package com.example.presentation.routes

import com.example.presentation.dto.StoreResponse
import com.example.usecase.store.GetStoreListUseCase
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * ### このファイルの役割
 * - 店舗マスターの一覧を返す `/stores` エンドポイントを定義します。
 * - 認証前の画面でも利用できるように、単純な GET ルートとして実装しています。
 */
fun Route.installStoreRoutes(
    getStoreListUseCase: GetStoreListUseCase
) {
    get("/stores") {
        val stores = getStoreListUseCase()
        call.respond(stores.map(StoreResponse::from))
    }
}
