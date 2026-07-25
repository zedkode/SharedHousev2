# Offline Synchronisation and Conflict Resolution

## Goals

Users must be able to read recent household information and queue common actions when connectivity is poor. The system must never fabricate confirmation or silently overwrite a concurrent financial change.

## Local model

- server-origin records with `id`, `version`, `updated_at`, `deleted_at` where appropriate;
- local outbox commands with idempotency key, creation time, dependency references and retry state;
- sync cursor per household and data stream;
- local projection for dashboard and calendar.

## Offline-capable actions

- create a draft/proposed expense;
- record own payment declaration;
- complete a chore;
- request help/swap/postpone;
- update shopping items;
- edit own profile settings.

High-risk administrative changes such as ownership transfer, member removal, subscription override and household deletion require live server confirmation.

## Conflict policies

- Payment declarations are append-only; concurrent declarations coexist and reconciliation detects overpayment.
- Expense edits use optimistic locking. A stale editor receives a comparison and must reapply changes.
- Chore completion is append-only; duplicate completion command is idempotent.
- Shopping item text can use last-write-wins only for non-financial fields, with server version and change history.
- Role and membership changes are server-authoritative.

## User feedback

Every queued action shows `Waiting for connection`. A server rejection displays the reason and restores the authoritative state. The app never shows a final green confirmation solely because the local database accepted a write.
