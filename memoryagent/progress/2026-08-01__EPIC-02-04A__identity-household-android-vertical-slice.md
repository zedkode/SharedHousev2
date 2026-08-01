# EPIC-02-04A — Identity and household Android vertical slice

**Date:** 2026-08-01  
**Area:** mobile / backend / contracts / infrastructure / documentation  
**Status:** completed slice

## Objective

Deliver a real local-development vertical from account registration through verified access and
household creation/editing, backed by authoritative APIs and a modern Android Material 3 UI. This
slice advances EPIC-02, EPIC-03 and EPIC-04 without marking those broader epics complete.

## What changed

- Expanded the TypeScript/OpenAPI v1 contract to version 1.1.0 for registration, email verification,
  sign-in, refresh, sign-out, account and household create/list/get/update.
- Added the initial PostgreSQL-compatible migration, a production `pg` adapter and persistent local
  PGlite adapter with migration checksums and transactions.
- Implemented NestJS identity and household modules, strict request parsing, RFC-style safe problem
  responses, opaque token hashing, refresh rotation/reuse detection, throttling, idempotency,
  optimistic version checks, tenant scoping, audit events and outbox events.
- Added a KMP Ktor client with safe Problem Details mapping and MockEngine tests.
- Replaced the static Android shell with Navigation Compose flows for registration, verification,
  sign-in, household gate, household creation/editing and authenticated Home.
- Added Material 3 light/dark/dynamic colour, matching EN/RO resources, honest loading/error/empty
  states and release HTTPS enforcement.
- Added an API runtime persistence smoke test and expanded CI gates for KMP and Android.

## Business and security rules preserved

- Household access requires an active verified account and current server session.
- Passwords use Node Argon2id when available, with the documented memory-hard scrypt compatibility
  fallback; raw passwords and raw stored tokens are never persisted.
- Access and refresh tokens are opaque; only SHA-256 hashes are stored. Refresh is rotated and reuse
  revokes the token family.
- Registration is generic when development-code exposure is disabled, and token-bearing responses
  are marked `no-store`.
- Household creation atomically writes the household, owner membership, audit and outbox records.
- Household reads are membership-scoped, cross-household access is hidden, create is idempotent and
  edit requires the current `If-Match` version.
- Android does not persist tokens insecurely or claim local success before server confirmation.
  Unimplemented feature areas remain explicit unavailable/zero-data states.
- Android release configuration rejects cleartext traffic and requires an HTTPS API base URL.

## Implementation notes

- Development defaults to persistent PGlite under ignored `tmp/`; production requires
  `DATABASE_URL` and uses `pg`.
- Local/test registration can expose the verification code so the flow is testable without an
  email provider. Production startup refuses this setting.
- Android access/refresh state intentionally remains in memory until a Keystore-backed refresh
  store is implemented. Process death therefore requires sign-in again.
- Lifecycle is fixed at 2.10.0 because 2.11.0 requires compile SDK 37 while the approved project
  baseline remains compile/target SDK 36.

## Validation

- `npm run check` — passed formatting, lint, typecheck, 19 TypeScript tests, all workspace builds and
  OpenAPI 1.1.0 validation.
- `npm audit --audit-level=high` — 0 vulnerabilities.
- API suite — 14/14 tests passed with PGlite files/workers serialized for stable CI execution.
- `node scripts/smoke-auth-household.mjs` — passed register, verify, create household, process
  restart, sign-in and persisted household discovery.
- Combined Gradle gate for `:shared:domain:jvmTest`, `:shared:network:jvmTest`, Android unit tests,
  lint and debug assembly — passed: 1 domain test, 6 network tests and 8 Android tests.
- Android lint — 0 errors and one existing `ObsoleteSdkInt` warning for the adaptive icon folder.
- Debug APK: `apps/android/app/build/outputs/apk/debug/app-debug.apk`, 20,958,556 bytes, SHA-256
  `B20A53EBF544F58B34488522413C7A19E152520B3324F8AE85EFCBAEB7AC235C`.
- Release manifest/build config tasks passed; merged release manifest disables cleartext and uses the
  deliberately invalid HTTPS placeholder until a deployment URL is supplied.
- EN/RO XML parsing confirmed 125 keys in each locale with no missing or extra keys.

## Migrations and compatibility

- `0001_identity_households.sql` is an additive initial schema covering identity credentials,
  verification challenges, sessions, households, memberships, idempotency, audit and outbox.
- Applied migrations are checksum-verified. The migration executed successfully on persistent
  PGlite and survived API restart; PostgreSQL 17 execution is not yet verified in this environment.
- OpenAPI remains on base path `/v1`; document version advanced from 1.0.0 to 1.1.0 additively.
- Android remains minimum SDK 26, compile/target SDK 36 and version code 1.

## Remaining work

- Integrate a real email sender plus resend-verification and password-reset flows.
- Add Android Keystore-backed refresh persistence, full device/session management and recent-auth.
- Validate migrations/concurrency on PostgreSQL 17 and replace in-memory throttling with distributed
  Redis-backed controls for multi-instance production.
- Implement household invitations, role changes, owner transfer and multi-household switching.
- Run Android emulator/device E2E, Compose UI, accessibility/TalkBack and screenshot tests.
- Configure a real release API endpoint, signing environment and remote CI execution.

## Documentation updated

- `README.md`
- `apps/android/README.md`
- `infra/README.md`
- `docs/09-delivery/task-master.md`
- `memoryagent/INDEX.md`
