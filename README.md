# Simple Social Media

Nx monorepo scaffold for the specification in [SPEC.md](./SPEC.md).

## Stack

- `apps/web`: Vue 3 + TypeScript + Pinia + Vue Router + Vite
- `apps/api`: Spring Boot + Gradle + Spring Security + MyBatis + springdoc OpenAPI
- `libs/shared/api-contract`: generated client from `/v3/api-docs`
- `DB/`: DDL, DML, and stored procedure scripts

## Commands

```sh
npm install
npm run dev:api
npm run sync:openapi
npm run dev:web
```

Useful Nx targets:

```sh
npx nx serve api
npx nx dev web
npx nx test web
npx nx test api
npx nx run api-contract:sync
```

## Local DB

This repo is currently MySQL-based, not PostgreSQL-based. The spec, SQL scripts, and backend runtime dependency all target MySQL 8.

Start the database container:

```sh
cp .env.db.example .env.db
docker compose -f docker-compose.db.yml up -d
```

Stop it:

```sh
docker compose -f docker-compose.db.yml down
```

The container will initialize the schema and stored procedures from `DB/` on first startup. If you need a clean reset:

```sh
docker compose -f docker-compose.db.yml down -v
```

Default connection values:

- host: `127.0.0.1`
- port: `3306`
- database: `simple_social_media`
- user: `social_user`
- password: `social_password`
- root password: `root_password`

Quick check:

```sh
docker compose -f docker-compose.db.yml ps
```

## OpenAPI flow

1. Start the backend on `http://127.0.0.1:8080`
2. Run `npm run sync:openapi`
3. Generated client files land in `libs/shared/api-contract/src/generated`

The repo does not keep an OpenAPI snapshot. `openapi-typescript-codegen` generates directly from the live Springdoc endpoint at `http://127.0.0.1:8080/v3/api-docs`, following the same style used in `/Users/hezibin/bindev/ucup-front`.

## Auth Spec

- [手機註冊與登入流程規格（#001）](./docs/auth/phone-auth-flow-spec.md)
- [Auth Token 生命週期與安全策略（#002）](./docs/auth/token-lifecycle-security-spec.md)

## Notes

- The backend is intentionally scaffolded with in-memory services so it can boot before MySQL is wired.
- JDBC/MyBatis auto-configuration is excluded for now; the SQL assets under `DB/` match the final target schema and stored procedure plan.
- Swagger UI is available at `/swagger-ui/index.html` once the backend is running.
- I did not add CI/CD Dockerfiles yet. That follow-up is intentionally deferred.
