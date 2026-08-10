# EPIC-06-01A — Money expense ledger foundation

**Date:** 2026-08-08  
**Area:** contracts / API / database / Kotlin network / Android / documentation  
**Status:** completed first vertical; advanced splits and payments remain

## Objective

Replace the unavailable Money destination with a trustworthy first end-to-end ledger flow that is
simple for a new household member and preserves exact, auditable financial history.

## Delivered

- OpenAPI 1.5 and TypeScript/Kotlin contracts for money, expenses, allocations and reversal.
- Migration `0006_ledger_expenses.sql` with append-only status history, allocation records and a
  deferred database constraint that rejects totals which do not reconcile.
- Tenant-scoped Nest endpoints for list/get/create/approve/reverse, idempotent creation, optimistic
  concurrency, audit records and outbox events.
- Deterministic equal splitting across active members. Remainder minor units follow stable
  membership order; for example 1001 splits as 501 + 500, never as a floating-point approximation.
- Role workflow: owner/admin creation is approved, member creation is proposed, read-only creation
  is rejected, and reversal preserves the original record plus its reason.
- Material 3 Android Money surface with approved personal/household summaries, status filters,
  add-expense flow, detail sheet, allocation/rounding explanation, approval/reversal and EN/RO text.
- Account export includes expenses relevant to the account, with action capabilities disabled in
  the exported historical representation.

## Validation

- OpenAPI 1.5 validation, contracts build and API typecheck passed.
- Expense and account API suites passed 13/13; focused expense tests cover exact allocation,
  idempotency, approval, reversal, export, currency validation and tenant isolation.
- Kotlin network JVM tests passed, including request metadata and response decoding for Money.
- Android local debug Kotlin compilation and unit tests passed, including authoritative Money state
  mapping and read-only capability enforcement.

## Remaining EPIC-06 scope

- Fixed, percentage, weighted, usage-based and custom split editors.
- Payment declaration, confirmation, dispute and reversal; SharedHouse does not currently move money.
- Recurring expenses, cycle reports, settlement suggestions, receipt evidence and operational
  reconciliation tooling.
- Live VPS migration/API rollout and physical-device UX/accessibility validation are separate gates.
