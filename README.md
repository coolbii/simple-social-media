# Simple Social Media

本專案是依據 [SPEC.md](./SPEC.md) 建立的 Nx monorepo 開發骨架。

## 技術棧

- `apps/web`: Vue 3 + TypeScript + Pinia + Vue Router + Vite
- `apps/api`: Spring Boot + Gradle + Spring Security + MyBatis + springdoc OpenAPI
- `libs/shared/api-contract`: 由 `/v3/api-docs` 產生的 API Client
- `DB/`: DDL、DML 與 Stored Procedure 腳本

## 常用指令

```sh
npm install
npm run dev:api
npm run sync:openapi
npm run dev:web
```

常用 Nx Target：

```sh
npx nx serve api
npx nx dev web
npx nx test web
npx nx test api
npx nx run api-contract:sync
```

## API Profile 與環境變數（#004）

API 預設使用 Spring profile `dev`，可透過下列方式覆寫：

```sh
SPRING_PROFILES_ACTIVE=dev npm run dev:api
```

目前 API 主要環境變數與預設值：

- `SPRING_PROFILES_ACTIVE=dev`
- `API_PORT=8080`
- `API_LOG_LEVEL=INFO`
- `AUTH_ACCESS_TOKEN_TTL_SECONDS=900`
- `AUTH_REFRESH_TOKEN_TTL_SECONDS=2592000`

## 骨架 Smoke Check（#003）

已驗證以下指令可於本機啟動（2026-03-28）：

- `npm run dev:api` -> `http://localhost:8080/swagger-ui.html`
- `npm run dev:web` -> `http://localhost:4200/`

## 本機資料庫（Local DB）

本專案目前使用 MySQL，而非 PostgreSQL。規格、SQL 腳本與後端執行相依皆以 MySQL 8 為目標。

啟動資料庫容器：

```sh
cp .env.db.example .env.db
docker compose -f docker-compose.db.yml up -d
```

停止資料庫容器：

```sh
docker compose -f docker-compose.db.yml down
```

容器首次啟動時，會自動從 `DB/` 初始化 schema 與 stored procedures。若需要完整重置：

```sh
docker compose -f docker-compose.db.yml down -v
```

預設連線參數：

- host: `127.0.0.1`
- port: `3306`
- database: `simple_social_media`
- user: `social_user`
- password: `social_password`
- root password: `root_password`

快速檢查：

```sh
docker compose -f docker-compose.db.yml ps
```

## DB Migration（Flyway）

`apps/api` 已接入 Flyway，migration SQL 位置：

- `apps/api/src/main/resources/db/migration`

目前 baseline migration 來自現有 `DB/ddl` 與 `DB/sp` 腳本整併，啟動 API 時預設會執行 migration（`DB_MIGRATION_ENABLED=true`）。

- 對於已由 Docker 初始化且已有資料表的 DB：會以 `baseline-on-migrate` 自動建置版本紀錄，不會重建既有結構
- 對於全新空資料庫：會從 `V1__init_schema_and_procedures.sql` 建立 schema 與 procedures

## OpenAPI 流程

1. 先啟動 backend（`http://127.0.0.1:8080`）
2. 執行 `npm run sync:openapi`
3. 產生的 Client 會落在 `libs/shared/api-contract/src/generated`

本專案不保存 OpenAPI snapshot。`openapi-typescript-codegen` 會直接從執行中的 Springdoc endpoint（`http://127.0.0.1:8080/v3/api-docs`）產生。

## Auth 規格文件

- [手機註冊與登入流程規格（#001）](./docs/auth/phone-auth-flow-spec.md)
- [Auth Token 生命週期與安全策略（#002）](./docs/auth/token-lifecycle-security-spec.md)

## 備註

- 後端功能服務目前仍有部分是 in-memory scaffold，DB-backed 的 MyBatis 服務會逐步遷移。
- `dev` profile 已啟用 JDBC/MyBatis auto-configuration；`DB/` 下的 SQL 仍是 schema 與 procedures 的唯一來源。
- Backend 啟動後可於 `/swagger-ui/index.html` 使用 Swagger UI。
- 目前尚未加入 CI/CD Dockerfile，此項目刻意延後處理。
