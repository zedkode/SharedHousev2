# EPIC-02-09-12A - Android calendar, onboarding and settings experience

**Date:** 2026-08-01  
**Area:** Android / calendar API / contracts / notifications / documentation  
**Status:** in progress

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

Pending implementation and verification.
