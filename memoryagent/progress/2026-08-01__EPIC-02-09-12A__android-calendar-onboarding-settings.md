# EPIC-02-09-12A - Android calendar, onboarding and settings experience

**Date:** 2026-08-08
**Area:** Android / calendar API / contracts / notifications / documentation  
**Status:** completed slice

## Objective

Advance the existing authenticated household vertical with a production-shaped, honest Material 3
experience: an interactive server-backed calendar, a skippable first-run tutorial, persistent
appearance/accessibility/notification settings, contextual guidance and an original launcher icon.
This is a reviewable slice of EPIC-02, EPIC-09 and EPIC-12; it does not complete those epics.

## Expected modules

- `packages/contracts` and OpenAPI v1 calendar resources.
- `services/api` tenant-scoped calendar storage and endpoints.
- `shared/network` calendar DTOs and Ktor client operations.
- `apps/android/app` responsive Material 3 shell, calendar, tutorial, settings, guidance,
  notifications and launcher resources.
- Delivery documentation and verification evidence.

## Invariants and boundaries

- Calendar records must come from the authenticated household API; empty states must not invent
  events, money, tasks, members or sync success.
- Calendar writes remain tenant-scoped, idempotent where applicable and version-protected.
- The first implementation supports one-off events only; recurrence is not simulated.
- Notification permission is requested only after an explicit value explanation and user action.
- Notifications use normal Android channels and never imitate emergency or exact-alarm behaviour.
- User preferences contain no authentication tokens or household secrets.
- English and Romanian resources remain in parity and accessibility descriptions include dates and
  event counts.
- The standard Gradle debug artifact remains named `app-debug.apk`; no alternate export flow or
  renamed APK is introduced.

## Planned validation

- Contract/OpenAPI validation and TypeScript format, lint, typecheck and tests.
- API calendar CRUD, idempotency, optimistic concurrency, role and cross-household isolation tests.
- KMP MockEngine coverage for calendar requests and failures.
- Android unit tests for calendar period math/forms and preference rules.
- Android lint, debug unit tests, debug assembly and release manifest/build-config checks.
- APK inspection for exact name, launcher resources and declared permissions.
- English/Romanian key-parity validation.

## Completion evidence

- OpenAPI `1.2.0`, TypeScript formatting/lint/typecheck/build and all 21 Node tests pass; `npm audit`
  reports zero known vulnerabilities.
- Calendar API coverage verifies CRUD, idempotency, version conflicts, role rules and tenant
  isolation. The runtime smoke proves identity, household and calendar persistence across API
  restart, then update and deletion.
- KMP JVM tests pass: 1 domain test and 7 network tests. Android passes 34 unit tests and a clean
  lint report (`No issues found`).
- Debug assembly and release manifest/build-config processing pass against SDK 36. The inspected
  artifact is `apps/android/app/build/outputs/apk/debug/app-debug.apk` (23,392,394 bytes,
  SHA-256 `D987ABD302259DD2E6A15A8366F9B011295AF1F32AB249EE2BFEB5BCBCDFC90A`).
- APK inspection confirms application ID `com.sharedhouse.android`, min SDK 26, target SDK 36,
  adaptive launcher icon, and only Internet, notification and AndroidX non-exported-receiver
  permissions.
- English and Romanian Android resources are in exact key parity (469 resource keys each).

## Remaining boundaries

- Events are manual and one-off; recurrence and generated bill/chore/shopping occurrences remain.
- Reminder preferences and offsets are stored, but no worker/FCM remote delivery exists yet.
- Keystore-backed session persistence was completed subsequently in `EPIC-03-01B`; device
  management and recent-authentication remain.
- Money and Tasks intentionally show unavailable states until their authoritative backend verticals
  exist. Manual emulator/device, TalkBack, foldable and iOS validation remain separate release work.
