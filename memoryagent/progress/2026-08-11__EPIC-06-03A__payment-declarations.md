# EPIC-06-03A — auditable payment declarations

**Date:** 2026-08-11  
**Area:** contracts / PostgreSQL / Nest API / privacy export / KMP network / Android Money / docs  
**Status:** completed and deployed to production

## Outcome

- Members can declare their own positive allocation paid on an approved expense, with method,
  timestamp, optional reference and optional household note.
- Another active owner, administrator or member with write access can independently confirm or
  dispute the declaration. The declarer cannot review their own record; read-only and cross-tenant
  callers are denied.
- A declaration can be corrected through a reasoned reversal. Original declarations and every
  transition remain append-only; a corrected allocation becomes outstanding and can be declared
  again.
- Active payment declarations block reversal of the underlying expense, preventing an approved
  charge from disappearing beneath a payment history.
- Android shows outstanding/awaiting-confirmation/paid/disputed allocation states, full payment
  history and guarded actions. The personal outstanding total excludes only confirmed payments.
- SharedHouse records household evidence only. It does not hold, initiate or confirm movement of
  funds outside the independent member review.

## Schema and API

- Migration `0009_expense_payment_declarations.sql` adds versioned payment declarations, a partial
  unique active-declaration constraint per allocation, transition history and query indexes.
- OpenAPI 1.8 adds idempotent declaration plus optimistic confirm, dispute and reverse endpoints.
- Expense allocations now expose derived payment state, complete declaration history and
  server-authoritative action capabilities.
- Account export includes payment history with all mutation capabilities disabled.
- Audit and outbox records use `ledger.payment_declared/confirmed/disputed/reversed.v1` events.

## Security and integrity review

- Amount and currency are copied from the server allocation; clients cannot choose either value.
- Declaration is limited to the caller's allocation and approved expenses; zero shares, duplicate
  active declarations and future timestamps beyond the clock-skew allowance are rejected.
- Every retryable create uses an idempotency key and hash. Mutations require the current quoted
  payment version in `If-Match`.
- Tenant hiding returns not found for outsiders. Confirmation/dispute requires a different active
  writer; read-only members cannot mutate.
- No receipt upload, bank integration, wallet, custody, transfer or automatic collection was added.

## Verification

- Focused API Money suite: 4/4 passed.
- Full `npm run check` passed: API 38/38, workers 9/9, contracts 8/8, lint, typecheck,
  OpenAPI 1.8 validation and all production builds.
- KMP network JVM tests passed, including payment request metadata and payloads.
- Android release lint, public-debug unit tests, KMP tests and the R8 production package passed.
- Signed Android v0.3.0 APK: 4,677,539 bytes; SHA-256
  `98B4AC1CC99969C4713C8AD2F061D63B9A8B163D35CAD210813813310893CF19`; signature schemes v2/v3
  and the existing direct-release certificate were verified.

## Production rollout

- Pre-deploy database backup:
  `/home/sharedhouse-backups/sharedhouse-20260811T101843Z.dump`.
- API and workers use immutable images
  `sharedhouse-api:0.1.0-money-payments-20260811` and
  `sharedhouse-workers:0.1.0-money-payments-20260811`.
- Migration `0009_expense_payment_declarations.sql` and both payment tables are present live.
- Existing production data remained `1` user, `1` verified user and `1` household; no live payment
  declaration was fabricated during verification.
- API readiness returned HTTP 200; an unauthenticated payment transport probe returned the expected
  HTTP 401; API and worker logs showed successful startup with no migration/worker failure.
- The deployment isolation gate confirmed all non-SharedHouse containers were unchanged.

## Remaining scope

- Fixed, percentage, weighted, usage-based, member-selected and custom split editors.
- Receipt/evidence attachments with retention and malware/privacy controls.
- Cycle reconciliation reports, settlement suggestions and operational integrity tooling.
- Physical-device usability/accessibility verification of the payment dialogs.
