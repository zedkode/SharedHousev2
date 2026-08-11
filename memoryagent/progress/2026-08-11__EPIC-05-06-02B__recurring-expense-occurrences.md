# EPIC-05/06-02B — recurring expense occurrences

Status: completed and deployed to production.

## Outcome

- Added forward-only migration `0008_recurring_expense_occurrences.sql` with schedule anchors,
  generated-occurrence provenance and a partial unique template/date key.
- Replaced the workers placeholder with a bounded PostgreSQL polling service. It selects due active
  templates using each household's IANA timezone, locks work with `FOR UPDATE SKIP LOCKED`, creates
  an approved exact equal-split expense and advances the schedule in one transaction.
- Preserved original month-end and leap-year anchors instead of drifting after clamped dates.
- Records system-generated status, audit and outbox evidence without impersonating the member who
  configured the schedule. Manual/replayed occurrence creation remains idempotent.
- Added a hardened non-root workers production container with no published port, read-only root
  filesystem, dropped capabilities, bounded logs and file-backed PostgreSQL credentials.
- Extended API/OpenAPI/KMP models and Android Money UI with generated source/date provenance and an
  explicit statement that SharedHouse recorded no payment.
- Updated EN/RO schedule wording and member/admin/deployment guidance. **Add extra** is explicitly an
  independent manual cost so it is not confused with the scheduled occurrence.
- Adopted production-only distribution: removed all named debug/testing APK packaging tasks and
  documented that only an optimized, owner-signed `publicRelease` APK/AAB may reach users. Internal
  debug variants remain solely for mandatory lint and unit-test gates.

## Acceptance evidence

- Root TypeScript/OpenAPI gate: `npm run check` passed (API 37, workers 9, contracts 7).
- API database and tenant suite: 37/37 passed, including all migrations.
- Workers: 9/9 passed for recurrence, invalid-date, leap-year, exact-allocation, environment and
  idempotent repair coverage.
- Android local-debug lint passed; Android JVM tests 47/47 and KMP network tests 11/11 passed.
- Named APK generated at
  `apps/android/app/build/outputs/apk/testing/SharedHouse-v0.1.0-local-testing-signed.apk` and
  verified with APK Signature Scheme v2. SHA-256:
  `D60DDFAFC3C4B65C6E8EEDA4FFACDB9F1E203BEBF540AFDBF184FD668674B1DA`.
- Production backup `sharedhouse-20260811T091506Z.dump` was created before deployment.
- Migration `0008_recurring_expense_occurrences.sql` is applied on the live PostgreSQL database.
- API and workers run with immutable image tag `0.1.0-money-recurring-20260811`, zero restarts; public
  liveness/readiness returned HTTP 200 in 0.153/0.145 seconds.
- Worker completed its first live poll with zero failures. The live uniqueness audit found zero
  duplicate occurrences. The deployment isolation gate confirmed every non-SharedHouse container
  was unchanged.

## Remaining ledger scope

- fixed, percentage, weighted, usage and member-selected custom splits;
- payment declaration, confirmation, dispute and correction events;
- operational reconciliation/repair command and production metrics/alerts;
- a reviewed VPS backup, migration, worker log and real due-occurrence live gate.
