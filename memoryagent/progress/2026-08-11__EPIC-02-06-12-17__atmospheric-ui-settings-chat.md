# EPIC-02/06/12/17 — Atmospheric UI, split settings and household chat

**Date:** 2026-08-11  
**Area:** Android / API / contracts / database / notifications / documentation  
**Status:** implementation, debug validation and VPS rollout completed; owner-signed Android release remains

## Outcome

- Removed the Android Material 3 dependency and all Material 3 source imports. Every existing
  Compose surface now uses SharedHouse-owned Foundation primitives. The earlier reference-derived
  styling was superseded by an original Deep Ink/Aurora system built directly in Compose, without
  Superdesign or another generated-design dependency.
- Rebuilt the dashboard as a responsive information surface: 2x2 metric modules on phones, compact
  action rows, a mint live-chat panel and a floating dark navigation dock. The circular metric and
  quick-action grids were removed.
- Reworked the global palette, typography, shape scale, elevation, fields, chips, segmented controls,
  buttons, floating actions, dialogs and navigation. Money summaries/expense rows and Chat composer
  received dedicated treatments; Calendar, House, Settings, onboarding and guidance inherit the
  same primitives and contrast rules.
- Added layered 3D elevation to cards, controls, selected chips and the navigation dock; fixed
  inherited foreground contrast, made selected states explicit and wrapped the Android root and
  custom overlays in safe drawing insets so system bars no longer mask content.
- Connected every dashboard metric to its relevant surface. Money and Requests now report real
  loading/error/ready state, and the Requests metric opens the filtered request board.
- Preserved and exposed the existing append-only Money workflows: authoritative personal and
  household totals, resident/couple roster, payment declarations/review/correction and finite or
  open-ended weekly, fortnightly, monthly, quarterly and yearly household costs.
- Split personal User settings from owner/admin Household creator settings. The latter links to real
  household identity, locale, cycle, member/role, invitation, finance, roster, chore schedule,
  communication and calendar controls. People and access now uses compact rows with a vertical
  action menu and server-derived capabilities.
- Expanded Calendar creation with a household-facing preview and expanded Money creation with an
  optional supplier, grouped details and explicit audit consequences. Owner/admin edits create an
  append-only expense revision; removal remains a reasoned reversal rather than deletion.
- Expanded Need guidance with separate user/admin articles for Money, Tasks, household controls and
  chat in both English and Romanian.
- Added append-only household chat with tenant-scoped history, idempotent send, authenticated SSE,
  Android reconnect/incremental fallback, live connection state and a glass conversation surface.
- Added a Chat notification category/channel and privacy-minimised foreground alerts for messages
  from other members when the conversation is not already open.
- Updated English and Romanian resources together and replaced the remaining quantity strings with
  locale-aware plurals. Android lint reports no errors.

## Main files

- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/atmosphere/AtmosphereComponents.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Theme.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Type.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Shape.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/home/HouseholdDashboardScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/settings/HouseholdCreatorSettingsScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/chat/HouseholdChatScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/app/SharedHouseViewModel.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/platform/notifications/SharedHouseNotifications.kt`
- `services/api/src/chat/`
- `services/api/migrations/0014_household_chat.sql`
- `services/api/migrations/0015_expense_supplier_revisions.sql`
- `packages/contracts/openapi/sharedhouse-v1.yaml`
- `shared/network/src/commonMain/kotlin/com/sharedhouse/network/SharedHouseApiClient.kt`

## Schema and API changes

- `0014_household_chat.sql` adds `household_chat_messages` with household/member foreign keys, a
  1–2000 character body constraint and incremental-read indexes. The feature has no edit/delete
  endpoint, so message history remains append-only.
- OpenAPI advanced from 1.12.0 to 1.13.0 and adds list/send/SSE chat paths plus versioned message and
  page contracts.
- `GET /v1/households/{householdId}/chat/messages` returns the latest or after-cursor page.
- `POST /v1/households/{householdId}/chat/messages` requires an idempotency key and appends one
  message.
- `GET /v1/households/{householdId}/chat/messages/stream` provides authenticated SSE. Android uses
  an infinite request/socket timeout for this stream, consumes heartbeat frames, deduplicates by
  message ID and reconnects through an incremental read.
- `0015_expense_supplier_revisions.sql` adds an optional supplier, immutable revision links and an
  append-only revision-event snapshot. `POST /v1/households/{householdId}/expenses/{expenseId}/revise`
  is owner/admin-only, idempotent, optimistic-versioned and refuses expenses with active payment
  declarations.

## Decisions and security/privacy review

- Material icons remain as vector assets, but no Material or Material 3 component/theme runtime is
  used. SharedHouse owns component behavior, tokens, elevation, focus, state and semantics.
- A single intentional dark identity replaces device/dynamic theme switching; text scaling, high
  contrast and reduced motion remain user controls.
- Every chat list/send/stream operation checks active account, active household and active
  membership. Tenant outsiders receive not-found; read-only members can read but cannot send.
- Message bodies are excluded from audit metadata, outbox events and notification previews.
  Notifications are private and use a generic public lock-screen version.
- Sending is retry-safe: Android keeps the draft and idempotency key after failure; the API replays
  the original result and rejects key reuse with a different body.
- Finance continues to store signed 64-bit minor units and does not claim that SharedHouse moved
  money. Expense removal is a reversal; editing creates a linked replacement and keeps the original
  amount, allocations and audit history. Settled/history records remain append-oriented.

## Validation

- `npm run check` passed: formatting, lint, typecheck, 48 API tests, 13 worker tests, 9 contract
  tests, all production package builds and OpenAPI 1.13.0 validation.
- `./gradlew :shared:domain:jvmTest :shared:network:jvmTest
  :apps:android:app:testPublicDebugUnitTest :apps:android:app:lintPublicDebug
  :apps:android:app:assemblePublicDebug --no-build-cache` passed.
- `npm run smoke:api` passed account verification, restart persistence, household recovery,
  calendar mutation and invitation acceptance against the migrated embedded database.
- Android public unit tests passed 54/54 after the finance gateway revision was added. Lint and
  public debug assembly passed after the final safe-area, calendar, settings and guidance changes;
  lint retained three non-blocking resource warnings and no errors.
- Material 3 dependency/source searches returned no current Android/shared/Gradle matches.
- Internal compiler artifact: `apps/android/app/build/outputs/apk/public/debug/app-public-debug.apk`.
  It is 33,467,464 bytes with SHA-256
  `1fb5e580cd5bd9d11e7e59a010f8721b5aeacc2a803718452a554f7e03c738b2`, uses a debug certificate
  and is suitable for direct testing, not store distribution.
- The public liveness and readiness endpoints both returned HTTP 200 with `status: ok` before the
  deployment decision.

## Limitations and deployment state

- Prior Superdesign artifacts are historical only and are not used as the current visual source.
  The active design system is implemented and reviewed directly in the Android Compose source.
- Provider-backed FCM/APNs background chat delivery, device-token registration, delivery telemetry,
  read receipts, reactions, media and iOS parity remain absent. Foreground SSE/local alerts must not
  be described as background push.
- Physical-device validation is still required for large text, TalkBack, reduced motion,
  notification permission, delivery, quick actions, process death and long-running SSE behavior.
- No signed Android release was built: this environment exposes none of the required
  `SHAREDHOUSE_RELEASE_*` material.
- Production was backed up to
  `/home/sharedhouse-backups/sharedhouse-20260811T210611Z.dump` before rollout. The guarded deploy
  applied migrations `0012_expense_recurrence_window.sql` through
  `0015_expense_supplier_revisions.sql` and published API/worker image tag
  `0.1.0-atmospheric-chat-20260811T2107Z`.
- The deployment isolation gate confirmed every pre-existing non-SharedHouse container was
  unchanged. PostgreSQL/API remained healthy, workers started without errors, and API/worker log
  scans were clean.
- Public liveness/readiness return HTTP 200. Unauthenticated chat history, chat SSE, expenses and
  expense revision now return HTTP 401 instead of the prior chat 404, confirming that the new routes
  are live and protected. A real-member send/receive device gate still requires an authenticated
  owner device and must not be inferred from the unauthenticated boundary check.

## Next task

Mount the existing owner signing material, build/install the optimized Android release, then use two
real authorised household devices to complete the physical-device accessibility, notification,
quick-action and chat send/receive/reconnect gate. Provider-backed FCM/APNs remains a separate
configuration and implementation requirement for background delivery while the application is
stopped.
