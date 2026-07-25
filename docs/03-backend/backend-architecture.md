# Backend Architecture

## 1. Purpose and boundaries

The backend is the authoritative coordination service for identity, households, membership, financial ledgers, chores, calendar events, shopping, notifications, subscriptions, support operations, audit evidence and privacy workflows. Mobile clients may calculate previews offline, but the server validates and commits every authoritative mutation.

The platform must remain a household coordination product. The initial release does not hold funds, initiate bank transfers, provide credit, collect debts, calculate taxes, or make landlord/tenant legal decisions.

## 2. Recommended stack

- TypeScript with NestJS and strict compiler settings.
- PostgreSQL as the system of record.
- Redis for short-lived locks, rate limits, job coordination and cache invalidation.
- S3-compatible object storage for receipts, meter images, avatars and exports.
- A durable job queue such as BullMQ initially; migrate to a broker only when scale requires it.
- OpenAPI 3.1 generated from contracts and checked in CI.
- OpenTelemetry traces, structured logs and metrics.
- Containerised deployment with separate API, worker and scheduled-job processes.

## 3. Logical services

Begin as a modular monolith with independently testable modules. Do not split into microservices before operational or scaling evidence requires it.

| Module | Responsibilities |
|---|---|
| Identity | Registration, sign-in, email verification, sessions, devices, MFA-ready controls |
| Households | Household settings, membership, roles, invitations, lifecycle |
| Ledger | Expenses, allocations, payment declarations, confirmations, adjustments, totals |
| Recurrence | 14-day, monthly, weekly and custom occurrence generation |
| Chores | Templates, assignments, completion, swaps, help and postponement |
| Calendar | Read model joining money, chores, shopping and household events |
| Shopping | Lists, items, purchases, reimbursements and approvals |
| Media | Upload intents, validation, scanning, metadata and signed access |
| Notifications | Preferences, templates, device tokens, delivery and in-app inbox |
| Billing | Apple/Google purchase evidence, entitlements, server notifications and reconciliation |
| Privacy | Consent records, exports, deletion, retention and legal holds |
| Administration | Platform RBAC, support tooling, moderation, feature flags and audit |
| Audit | Append-only business and privileged-operation events |

## 4. Deployment topology

```text
Mobile apps / Admin web
          |
   CDN + WAF + TLS
          |
      API gateway
          |
  NestJS API instances
    |       |       |
Postgres  Redis  Object storage
    |       |
 Read replica  Queue workers
          |
 Push providers / Email / Apple / Google
```

Use at least separate development, staging and production environments. Production secrets, databases, storage buckets, notification credentials and store credentials must not be shared with lower environments.

## 5. Household tenancy

Every household-owned table includes `household_id`. The authorisation layer derives accessible households from the authenticated membership, never from an untrusted request alone. Repository methods require a tenancy scope object and tests must prove that one household cannot read or mutate another household.

Platform support access is separate from household membership. Support agents receive time-limited, reason-bound access only to the minimum metadata required. Content access requires an elevated audited workflow.

## 6. Command and query pattern

- Commands validate identity, membership, role, current aggregate version and idempotency key.
- Domain services apply state transitions and produce events.
- The transaction persists records, outbox events and audit evidence atomically.
- Query endpoints read purpose-built views optimised for dashboards and calendar screens.
- Clients receive a new sync cursor and affected resource versions.

Use optimistic concurrency for frequently edited records. Return a structured conflict response containing the latest safe representation and allowed recovery actions.

## 7. Reliability requirements

- All externally retried writes require idempotency keys.
- Store-provider notifications are deduplicated by provider event identifier and signed payload hash.
- Outbox messages are delivered at least once; consumers must be idempotent.
- Scheduled occurrence generation uses deterministic keys so retries cannot create duplicates.
- Financial totals are regenerated from ledger sources and periodically checked against materialised summaries.
- Backup restoration is tested, not assumed.

## 8. Availability targets

Initial production targets:

- API monthly availability objective: 99.9% excluding announced maintenance.
- Read p95: below 400 ms for normal household queries.
- Write p95: below 700 ms excluding media upload.
- Push scheduling delay: normally below 60 seconds.
- Recovery point objective: 15 minutes or better.
- Recovery time objective: 4 hours or better for a regional service incident.

These are engineering objectives, not contractual service-level agreements until commercial terms define them.

## 9. Configuration and feature flags

Configuration is typed, validated at startup and separated by environment. Feature flags require an owner, purpose, rollout scope, expiry/review date and default-safe behaviour. A flag may not bypass permission checks, privacy disclosure, purchase validation, audit logging or data migration requirements.

## 10. API lifecycle

Use `/v1` for the first public contract. Additive fields are preferred. Breaking changes require a new version or an explicit compatibility mechanism. Deprecations must be measured through client-version telemetry, announced in release notes and retained for the documented support window.
