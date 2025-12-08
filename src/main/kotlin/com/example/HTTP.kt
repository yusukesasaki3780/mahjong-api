package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

// /**
//  * ### このファイルの役割
//  * - フロントエンドからのクロスオリジンアクセスを許可するための CORS 設定をまとめています。
//  * - プリフライト（OPTIONS）や認証ヘッダーを含むリクエストでも弾かれないよう詳細に許可を設定しています。
//  * - 開発環境では `anyHost()` で固定し、本番環境ではオリジンを絞り込む前提です。
//  */
// fun Application.configureHTTP() {
//     install(CORS) {
//         allowMethod(HttpMethod.Options)
//         allowMethod(HttpMethod.Get)
//         allowMethod(HttpMethod.Post)
//         allowMethod(HttpMethod.Put)
//         allowMethod(HttpMethod.Patch)
//         allowMethod(HttpMethod.Delete)

//         allowHeader(HttpHeaders.Authorization)
//         allowHeader(HttpHeaders.ContentType)
//         allowHeader(HttpHeaders.Accept)
//         allowHeader(HttpHeaders.Origin)
//         allowHeader(HttpHeaders.AcceptLanguage)
//         allowHeader(HttpHeaders.AcceptEncoding)

//         allowCredentials = true
//         allowNonSimpleContentTypes = true

//         anyHost() // TODO: 本番環境では許可するオリジンを限定してください。
//     }
// }
