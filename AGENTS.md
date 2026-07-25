<!-- Compatibility copy. AGENTS.md is the canonical repository-level agent instruction file. Keep both files identical when updating. -->

# AGENTS.md — SharedHouse Engineering Agent Contract

## Purpose

This file is the operating contract for Codex, Claude Code, GitHub agents, local coding assistants, and human contributors. It applies to the entire repository unless a deeper `AGENTS.md` explicitly narrows a module. A nested file may add constraints but may not weaken security, privacy, tenancy, financial-integrity, accessibility, or store-compliance rules.

## Core behaviour

1. Read before writing. Inspect the existing repository, documentation, tests, migrations, API schemas, and recent memory entries before proposing changes.
2. Do not invent. Never assume a function, endpoint, table, event, permission, state, or feature exists without verifying it.
3. Keep scope strict. Implement the assigned task completely, but do not refactor unrelated modules or add speculative features.
4. Preserve history. Financial records, chore completion history, membership history, subscription transactions, audit events, and consent records are append-oriented. Corrections use reversal/supersession, not destructive rewriting.
5. Prefer explicit code. Business rules must be named, testable, and located in the domain layer rather than hidden in UI callbacks or controllers.
6. Fail securely. Authorisation failure, invalid household scope, stale invitation, mismatched currency, duplicate webhook, or invalid store signature must deny the operation and create safe telemetry.
7. Write migrations deliberately. Every schema change needs compatibility analysis, backfill strategy, indexing plan, and deployment ordering.
8. Keep the product honest. Do not claim a payment occurred merely because a checkbox changed; store who declared it, when, the method/reference if supplied, and whether another member confirmed it.

## Required architecture boundaries

- `apps/android/` contains Android-specific UI and platform adapters.
- `apps/ios/` contains SwiftUI presentation and Apple platform adapters.
- `shared/` contains portable domain models, use cases, validation, sync rules, API clients, and database abstractions suitable for KMP.
- `services/api/` contains NestJS HTTP APIs and application orchestration.
- `services/workers/` contains scheduled jobs, notification fan-out, store webhook processing, exports, and deletion jobs.
- `apps/admin-web/` contains the platform administration portal.
- `packages/contracts/` contains versioned API/event schemas generated or shared across services.
- `packages/localization/` contains canonical English and Romanian message keys where cross-platform sharing is practical.
- `infra/` contains reproducible environments, not secrets.

Do not create a generic `utils` dumping ground. Shared code must have a clear domain or infrastructure owner.

## Financial integrity rules

- Store monetary values as signed 64-bit integer minor units and a currency code.
- A household billing cycle has exactly one settlement currency in MVP.
- An expense can use equal, fixed, percentage, weighted, usage-based, or custom split methods.
- Split rows must reconcile exactly to the expense total. Rounding differences are allocated deterministically and tested.
- `Mark as paid` creates a payment/declaration record. It does not delete the charge.
- Editing a settled expense creates a revision and balance adjustment; it does not silently change historical totals.
- Users may dispute or request correction without losing the original audit trail.
- Never mix app subscription revenue with household ledger money.

## Permission rules

- Platform roles and household roles are separate namespaces.
- A platform administrator is not automatically a member or administrator of a household.
- Household administrators cannot view platform commercial data beyond their own subscription and entitlement status.
- Support access to household content is denied by default, time-limited, consent-based where possible, and audited.
- Every backend command checks authentication, account state, household membership, role capability, resource ownership, and current version where optimistic locking is used.

## Mobile rules

- Offline actions use an outbox and idempotency keys.
- Never make the UI appear synced when the server has rejected a change.
- Sensitive tokens use Android Keystore and iOS Keychain.
- Use the system photo picker for avatars; do not request broad media-library access.
- Do not request contacts, location, microphone, call logs, SMS, or background execution unless a separately approved feature requires it.
- Notification permission is requested contextually after explaining value, not immediately on first launch.
- High-priority household alerts use normal platform notification capabilities. Do not request Apple Critical Alerts entitlement for the MVP and do not mimic public-warning visuals, sounds, or language.

## Backend rules

- All externally triggered operations are idempotent where retries are possible.
- Apple and Google purchase notifications must be signature/credential verified and reconciled against store APIs.
- Webhook payloads are retained only as needed, encrypted where appropriate, and stripped from normal logs.
- Background jobs use explicit retry policies, dead-letter handling, and deduplication.
- Rate-limit login, invite acceptance, password reset, export, deletion, support, and notification broadcast endpoints.
- PostgreSQL constraints enforce invariants in addition to application validation.

## UI/UX rules

- The app must remain usable with large text, screen readers, reduced motion, high contrast, and one-handed navigation.
- Do not encode status using colour alone.
- Confirmation dialogs describe consequences, especially for payment declarations, member removal, household deletion, subscription changes, and data erasure.
- The default dashboard shows the next amount due, next household task, upcoming calendar items, and unresolved requests without overwhelming the user.
- User-facing language is plain, respectful, and non-accusatory. Prefer “Payment not recorded” to “You have not paid”.

## Localisation rules

- English is the source language; Romanian is required at feature completion.
- No concatenated sentences or hard-coded plural grammar.
- Dates, numbers, currency, first day of week, and measurement units use locale-aware formatting.
- A user may override the detected device language in Settings.
- Household timezone and currency are independent from UI language.

## Testing obligations

At minimum, test split reconciliation and rounding; 14-day and calendar-month cycle generation; DST transitions; join/leave proration; duplicate sync events; invitation expiry; role escalation; offline payment conflicts; chore swap/help/postpone; subscription renewal/cancellation/refund/chargeback; account export/deletion; localisation completeness; and accessibility semantics.

## Documentation and memory

Update the existing authoritative document instead of creating parallel notes. At task completion, add a factual record under `memoryagent/progress/` and update `memoryagent/INDEX.md`. The record must include outcome, files, schema/API changes, decisions, tests, security/privacy review, limitations, and the next task.

## Prohibited actions

- Do not commit secrets, `.env` values, signing keys, store credentials, database dumps, user exports, or screenshots containing personal data.
- Do not bypass tests to make CI green.
- Do not disable certificate validation, signature verification, authorisation, or encryption.
- Do not add advertising trackers without explicit product, privacy and store review.
- Do not implement wallet balances, custody, pooled funds, credit, automatic rent collection, or peer-to-peer transfer under the household ledger.
- Do not state that the product is legally compliant, certified, audited, or production-ready without evidence and sign-off.
