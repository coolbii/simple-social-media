# Architecture Overview

This scaffold follows the repository layout and package naming from `SPEC.md`.

## Workspace layout

- `apps/web`: Vue 3 application generated with the Nx Vue plugin.
- `apps/api`: Spring Boot application generated with the Nx Spring Boot plugin.
- `libs/shared/api-contract`: generated TypeScript client from Springdoc OpenAPI.
- `libs/shared/utils`: browser-side shared environment helpers.
- `libs/web/*`: placeholder Vue feature libraries matching the spec structure.

## Current scaffold decisions

- Backend domain packages live inside `apps/api/src/main/java/com/example/social/*`.
- Frontend OpenAPI generation runs directly against the live Springdoc endpoint.
- SQL deliverables under `DB/` represent the target MySQL schema and stored procedures even though the running scaffold uses in-memory services.

## Immediate next steps

- Replace the in-memory services with MyBatis stored-procedure implementations.
- Remove the JDBC/MyBatis auto-config exclusions in `application.properties`.
- Point upload endpoints at real S3 or a local-compatible object storage service.
