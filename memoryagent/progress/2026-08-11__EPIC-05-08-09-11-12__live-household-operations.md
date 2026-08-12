# EPIC-05/08/09/11/12 — Live household operations

**Date:** 2026-08-11  
**Area:** mobile / backend / workers / contracts / documentation  
**Status:** implementation and VPS publication completed; signed release and physical-device gate remain

## Objective

Remove the Android close-and-reopen refresh requirement, make scheduled reminders and task quick
actions functional, expose household-management entry points, and support finite or open-ended rent,
bill and cleaning schedules that appear in Calendar.

## What changed

- Android refreshes the selected household's calendar, money, tasks and membership projections every
  five seconds while the authenticated household UI is in the foreground. In-flight loads/mutations
  are not cancelled and a transient silent-refresh failure keeps the last ready projection.
- WorkManager schedules timezone/lead-time/quiet-hours-aware reminders for known approved expenses
  and the signed-in member's tasks. Task reminders expose Start/Complete actions.
- The quick-action receiver is non-exported, pending intents are immutable, inputs are scoped and
  versioned, and Complete supplies the API-required localized completion note.
- Household owner/admin controls now link directly to Money schedules and cleaning schedules.
- Expense recurrence supports fortnightly cadence and an optional inclusive final date. The worker
  archives a finite schedule after its last generated occurrence and writes status evidence.
- Chores support weekly, fortnightly or monthly fixed-assignee schedules, optional inclusive final
  dates, exact series/date occurrence keys and a rolling 90-day worker horizon.
- Owner/admin can stop a chore schedule with a reason. Generation stops, later active occurrences
  are cancelled with per-task history/audit/outbox evidence, and completed history is retained.
- Android Calendar merges tenant-scoped one-off events with authoritative expense and task
  occurrences. Derived finance/task items are read-only calendar projections.
- OpenAPI advanced to 1.12.0 and the Kotlin network models were updated.

## Business and security rules preserved

- Every backend read/action still authenticates and checks active household membership; task
  management remains owner/admin-only and optimistic versions reject stale quick actions.
- Expense generation keeps exact signed 64-bit minor units, household currency, deterministic
  allocation and append-only payment/expense history. Ending a schedule never rewrites an expense.
- Task schedule stopping and system generation retain history rather than deleting occurrences.
- Notification lock-screen public versions use the app name and a generic body. The private version
  uses normal reminder channels and never claims that payment occurred.
- Quick actions load the Keystore-encrypted session, rotate an expired access token when possible,
  use an idempotency key and let the server re-check capability and current version.

## Implementation notes

- The current sync transport is a bounded authenticated foreground poll, not WebSocket/SSE and not
  background realtime. This deliberately fixes stale foreground screens without claiming instant
  push semantics.
- Recurring task rows keep their original fixed assignee. Future fairness/rotation requires an
  explicit assignment policy rather than silently changing the selected member.
- Expense and task date rules use local dates/anchors; notification instants are resolved through
  the household IANA timezone and then adjusted for quiet hours.

## Validation

- `npm run check` passed: formatting, lint, typecheck, all production package builds, OpenAPI 1.12.0,
  API 45/45, workers 13/13 and contracts 9/9 tests.
- `./gradlew :shared:domain:jvmTest :shared:network:jvmTest
  :apps:android:app:testPublicDebugUnitTest :apps:android:app:lintPublicDebug
  :apps:android:app:assemblePublicDebug --no-build-cache` passed.
- Android public unit tests passed 53/53, including foreground refresh, DST/quiet-hour reminder
  calculation and notification action validation. Lint had zero errors and eight non-blocking
  resource warnings.
- `npm run smoke:api` passed registration/verification, restart persistence, household recovery,
  calendar mutation and invitation acceptance against the migrated embedded database.
- Internal compiler artifact: `apps/android/app/build/outputs/apk/public/debug/app-public-debug.apk`
  (33,222,857 bytes). It is a debug artifact and must not be distributed.
- Public liveness and readiness both returned HTTP 200 before publication was attempted.

## Migrations and compatibility

- `0012_expense_recurrence_window.sql` adds a nullable expense end date, expands the cadence check,
  permits system-authored schedule status events and adds an active-window index.
- `0013_recurring_household_tasks.sql` adds nullable recurrence/series fields, a default-false
  completion flag, exact series/date uniqueness, generation indexing and nullable system history
  actors. Existing tasks remain non-recurring without backfill or history mutation.
- Apply migrations in filename order after a production backup. They are forward-only; an applied
  schema must be repaired forward rather than deleting migration records.

## Remaining work

- No signed 0.6.0 release was produced. The release gate correctly refused because the four
  `SHAREDHOUSE_RELEASE_*` signing variables/material were unavailable in this Linux environment.
- VPS deployment completed after a dedicated scoped SSH identity was authorised. Backup
  `/home/sharedhouse-backups/sharedhouse-20260811T210611Z.dump` preceded the guarded rollout;
  migrations `0012_expense_recurrence_window.sql` and `0013_recurring_household_tasks.sql` are
  recorded, the isolation gate passed, public readiness is HTTP 200 and worker/API error scans are
  clean under image tag `0.1.0-atmospheric-chat-20260811T2107Z`.
- Provider-backed FCM/APNs push tokens, background remote delivery telemetry and notification inbox
  remain absent. Local reminders work for data already refreshed by Android; a server change cannot
  wake an app that has not refreshed it.
- Physical-device notification permission, delivery, quick-action, process-death, large-text,
  TalkBack and reduced-motion validation remain. iOS parity is not implemented.
- Recurring chore schedule editing, rotating/balanced assignment, exemptions and reusable named
  chore templates remain.

## Documentation updated

- `README.md`
- `docs/02-mobile/notifications.md`
- `docs/03-backend/jobs-and-realtime.md`
- `docs/08-admin-docs/household-admin-guide.md`
- `docs/09-delivery/task-master.md`
- `memoryagent/INDEX.md`
