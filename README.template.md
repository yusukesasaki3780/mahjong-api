# Mahjong App <!-- omit in toc -->

> 最終更新: {{ date }}

スマホ入力前提の UI/UX を備えた麻雀店スタッフ向け勤怠・成績管理 Web アプリです。  
勤務シフトの登録や日跨ぎ対応、詳細な成績入力、ランキング・統計ダッシュボードなど現場運用を意識した機能を提供します。

---

## 🧭 目次
1. [プロジェクト紹介](#-プロジェクト紹介)
2. [機能一覧](#-機能一覧)
3. [システム構成図](#-システム構成図)
4. [技術スタック](#-技術スタック)
5. [セットアップ方法](#-セットアップ方法)
6. [ディレクトリ構造](#-ディレクトリ構造)
7. [API 仕様](#-api-仕様)

---

## 🐲 プロジェクト紹介
- **Mahjong App** は麻雀店スタッフの勤怠・給与・成績を一元管理する Web アプリです。
- **スマホで片手入力**できるように UI/UX を最適化。
- シフト登録、勤務統計、ランキング、成績詳細を一つのダッシュボードで閲覧可能。
- 成績は自動で収支計算・深夜時間の抽出・月次統計化を行い、ランキングや KPI をリアルタイムに反映します。

## ✨ 機能一覧
| カテゴリ | 詳細 |
| --- | --- |
| 認証 | JWT ベースのログイン・リフレッシュ、試行制限、強固なパスワードポリシー |
| プロフィール管理 | ユーザー基本情報、営業時間・所属店舗情報の編集 |
| シフト管理 | 日跨ぎシフト、部分更新、休憩時間管理、統計（totalHours / nightHours / avgHours / count） |
| 成績管理 | ゲーム種別ごとの収支計算、チップ単価、深夜調整、前借控除、CSV エクスポート |
| ダッシュボード | 月次勤務統計、収支グラフ、未入力アラート、通知バッジ |
| ランキング | 期間・種別フィルタ、平均着順・総収支・対局数ランキング |
| ゲーム設定 | ゲーム代・チップ単価・給与形態（時給/固定/バック/交通費）設定 |
| バリデーション | すべてのフォームで日本語エラー表示、パスワード強度チェック、日付/時刻整合性 |
| スマホ最適化 | Naive UI + モバイルファーストレイアウト、スワイプ・入力補助、テーマ対応 |

## 🏗️ システム構成図
```
┌──────────┐      ┌───────────┐      ┌───────────┐      ┌────────────┐
│  Frontend │──→── │   API GW   │──→── │  Backend   │──→──│ PostgreSQL │
│ (mahjong- │ TLS  │ (Ktor HTTP │ JWT │ (Ktor +     │ SQL │  (RDS etc) │
│   front)  │      │   Routing)  │     │  Exposed)   │     │            │
└──────────┘      └───────────┘      └───────────┘      └────────────┘
        ▲                                   │
        └───────────── CI/CD (GitHub Actions + IaC) ───────►
```

## 🛠️ 技術スタック
| 層 | 使用技術 |
| --- | --- |
| フロントエンド | Vue 3, TypeScript, Vite, Naive UI, Pinia, Vue Router, dayjs, Axios |
| バックエンド | Kotlin, Ktor, Exposed, PostgreSQL, Flyway, kotlinx.serialization, kotlinx.datetime, JWT, Swagger (OpenAPI) |
| インフラ | Docker, Docker Compose, AWS (予定), Redis or Memory Cache (login attempts) |
| CI/CD | GitHub Actions, Gradle, pnpm, lint/test workflow, README 自動生成 |

## ⚙️ セットアップ方法

### 1. リポジトリの取得
```bash
git clone https://github.com/your-org/mahjong-app.git
cd mahjong-app
```

### 2. フロントエンド (mahjong-front)
```bash
cd mahjong-front
pnpm install
pnpm run dev
```

### 3. バックエンド (mahjong-api)
`.env`（または `application.conf`）例:
```env
DB_URL=jdbc:postgresql://localhost:5432/mahjong
DB_USER=mahjong
DB_PASSWORD=secret
JWT_SECRET=change-me
JWT_AUDIENCE=mahjong-users
JWT_ISSUER=mahjong-api
REDIS_URL=redis://localhost:6379
```

起動:
```bash
cd mahjong-api
./gradlew clean run
```

### 4. Swagger / OpenAPI
- バックエンド起動後 `http://localhost:8080/swagger` を参照。

## 📁 ディレクトリ構造（抜粋）
```
mahjong-app/
├── mahjong-front/
│   ├── src/
│   │   ├── components/
│   │   ├── stores/
│   │   ├── views/
│   │   └── api/
│   ├── public/
│   └── vite.config.ts
└── mahjong-api/
    ├── src/main/kotlin/com/example/
    │   ├── presentation/routes/
    │   ├── usecase/
    │   ├── domain/
    │   ├── infrastructure/
    │   └── security/
    ├── resources/
    └── build.gradle.kts
```

## 📡 API 仕様
- **Swagger UI:** `http://localhost:8080/swagger`

| エンドポイント | メソッド | 説明 |
| --- | --- | --- |
| `/auth/register` | POST | ユーザー登録 |
| `/auth/login` | POST | ログイン（ロック制御） |
| `/auth/refresh` | POST | アクセストークン再発行 |
| `/users/{id}` | GET/PUT/PATCH | プロフィール取得・更新 |
| `/users/{id}/shifts` | GET/POST | シフト一覧・登録 |
| `/users/{id}/shifts/{shiftId}` | PUT/PATCH/DELETE | シフト更新・部分更新・削除 |
| `/users/{id}/shifts/stats` | GET | 勤務統計 |
| `/users/{id}/results` | GET/POST | 成績一覧・登録 |
| `/users/{id}/results/{resultId}` | GET/PUT/PATCH/DELETE | 成績詳細・更新・削除 |
| `/ranking` | GET | 種別・期間フィルタ付きランキング |
| `/users/{id}/settings` | GET/PUT/PATCH | ゲーム設定取得・更新 |
| `/users/{id}/advance/{yearMonth}` | GET/PUT | 前借管理 |
| `/users/{id}/dashboard/summary` | GET | ダッシュボードサマリ |

Swagger で完全なスキーマ・レスポンス例を確認できます。

---

一緒に Mahjong App をより良くしていきましょう！ 🚀
