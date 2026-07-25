# EPIC-01-01 — Repository foundation

**Date:** 2026-07-25  
**Area:** mobile / backend / admin-web / infrastructure / documentation  
**Status:** completed

## Objective

Create the first executable SharedHouse engineering foundation without implementing product-domain
features out of task order. Establish reproducible workspaces, a public health boundary, portable
KMP structure, synthetic local services and automated checks.

## What changed

- Added npm workspaces for `services/api`, `services/workers`, `apps/admin-web` and
  `packages/contracts`, with a committed dependency lockfile.
- Added a NestJS `GET /v1/health` endpoint and OpenAPI 3.1 contract.
- Added a structured worker startup health record.
- Added an accessible React administration shell that labels unfinished areas as not configured.
- Added a Kotlin Multiplatform `shared/domain` module with Android, iOS and JVM targets.
- Added Gradle 9.5 wrapper files with the official distribution checksum.
- Added synthetic-only PostgreSQL, Redis and S3-compatible local service definitions.
- Added strict TypeScript, lint, formatting, tests, contract validation, dependency audit, CI and
  Dependabot configuration.
- Added Android and iOS ownership/boundary readmes; installable applications are not created yet.

## Business and security rules preserved

- No household, financial, payment, entitlement or permission behaviour was invented.
- The API exposes operational health only and contains no user or household data.
- Local infrastructure defaults are explicitly synthetic and are not production credentials.
- The admin shell does not imply authentication, household access or operational readiness.
- No money movement, custody or destructive ledger behaviour was introduced.

## Implementation notes

- TypeScript services use npm workspaces and the shared `@sharedhouse/contracts` package.
- Nest CLI was deliberately excluded after its development dependency chain produced high-severity
  audit findings; API build and watch use TypeScript and `tsx` directly.
- Kotlin 2.4.10, AGP 9.1 and Gradle 9.5 follow the documented compatibility range.
- The mobile application projects are deferred until identifiers, minimum runtime versions and
  signing/capability decisions are approved.

## Validation

- `npm run lint` — passed.
- `npm run typecheck` — passed for contracts, API, workers and admin web.
- `npm test` — passed; API 6 tests, workers 1 test and contracts 2 source tests.
- `npm run build` — passed, including the Vite production bundle.
- `npm run contracts:check` — passed for OpenAPI 3.1 version 1.0.0.
- `npm audit --audit-level=high` — passed with 0 vulnerabilities.
- `.\gradlew.bat :shared:domain:jvmTest` — passed on Windows/JDK 17.
- Docker infrastructure runtime validation was not run because Docker is not installed.
- iOS targets were not compiled because macOS/Xcode is required.

## Migrations and compatibility

There are no database migrations or existing clients. API version `v1` is established only for the
health boundary. Future contract additions must remain additive or use the documented versioning
process. The Gradle wrapper verifies the downloaded distribution checksum.

## Remaining work

- EPIC-01-02: approve application identifiers, minimum Android/iOS versions and signing boundaries,
  then create installable native application shells.
- EPIC-01-03: add database migrations and deterministic synthetic fixtures.
- EPIC-01-04: add SBOM, secret scanning, licence scanning and generated-contract drift checks.
- Validate `infra/compose.yaml` on a machine with Docker.
- Run iOS compilation and tests on macOS/Xcode.

## Documentation updated

- `README.md`
- `docs/09-delivery/task-master.md`
- `memoryagent/INDEX.md`
