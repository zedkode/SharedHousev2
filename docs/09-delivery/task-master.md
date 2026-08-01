# Task Master

The task IDs below are implementation epics. Break each into reviewable tasks with explicit acceptance criteria and a `memoryagent` completion entry.

## EPIC-01 Repository and engineering foundation

- [in progress] KMP modules, Android/iOS applications, backend, workers and admin web. The portable
  domain, API, workers, admin shell and installable Android foundation exist; the installable iOS
  application project remains.
- [in progress] Shared formatting, linting, tests and generated contracts. TypeScript and OpenAPI
  checks exist; contract generation and mobile formatting remain.
- [in progress] Local development environment with synthetic fixtures. Service definitions exist;
  fixtures and runtime validation remain.
- [in progress] CI/CD, SBOM, secret/dependency scanning and environment configuration. Foundation CI
  and dependency updates exist; SBOM, secret scanning and release environments remain.

## EPIC-02 Design system and onboarding

- [in progress] Design tokens, typography, spacing, semantic colours and component states. Android
  authentication and household forms now use the Material 3 product theme; the full component
  catalogue remains.
- [in progress] Native navigation and responsive layouts. The Android account/household gate is
  implemented; broader feature navigation and device-size validation remain.
- [in progress] Language detection and English/Romanian resources. The Android vertical has parity;
  remaining platforms and content still require coverage.
- [in progress] Appearance, accessibility settings and progressive tutorial. System light/dark and
  Android dynamic colour exist; user overrides and the tutorial remain.

## EPIC-03 Identity and account security

- [in progress] Registration, verification, sign-in, rotating sessions and devices. The API and
  Android vertical work locally; email delivery, secure mobile persistence and device management
  remain.
- Profile/avatar and secure media pipeline.
- Re-authentication, export and deletion request entry points.
- Admin MFA and policy-based RBAC.

## EPIC-04 Household and invitations

- [in progress] Household configuration and lifecycle. Authenticated create/list/get/update with
  idempotency and optimistic version checks are implemented.
- [in progress] Membership roles and owner-transfer invariant. Creation atomically provisions the
  owner membership; role management and transfer remain.
- Secure invitation creation, preview, acceptance, revocation and deep links.
- [in progress] Cross-household authorisation test suite. Read isolation is covered; broader
  mutation, role and invitation coverage remains.

## EPIC-05 Cycles and recurrence

- Local-date/IANA-timezone recurrence engine.
- Fourteen-day, monthly, weekly and custom rules.
- Deterministic occurrence keys, edits and series scope.
- DST and invalid-date property tests.

## EPIC-06 Ledger

- Money/currency value objects.
- Expenses, revisions, allocations and all split methods.
- Payment declaration/confirmation/dispute/reversal.
- Dashboard and explanation read models.
- Integrity checker and reconciliation tools.

## EPIC-07 Utilities

- Electricity/gas/internet categories.
- Meter accounts/readings/photos and estimation labels.
- Usage allocation with standing-charge handling.

## EPIC-08 Chores

- Templates, zones, recurrence and assignment strategies.
- Balanced/round-robin fairness and exemptions.
- Completion evidence and issue reporting.
- Swap/help/postpone request state machine.

## EPIC-09 Calendar

- Unified event query/read model.
- Agenda/week/month and filters.
- Avatar assignment rendering and amount/status display.
- Conflict, recurring-edit and action-sheet behaviours.

## EPIC-10 Shopping

- Lists/items, claiming, substitution and actual prices.
- Receipt media, approval policy and duplicate detection.
- Purchase-to-ledger reimbursement link.

## EPIC-11 Offline sync

- SQLDelight schema/migrations.
- Outbox with client operation IDs.
- Cursor pull, tombstones, retries and conflict UI.
- Removed-membership/offline safety tests.

## EPIC-12 Notifications

- Preferences, quiet hours, lead times and lock-screen privacy.
- Push tokens, notification jobs, delivery telemetry and inbox.
- High-priority household design without emergency-alert imitation.

## EPIC-13 Store commerce

- StoreKit and Google Play Billing adapters.
- Backend verification and account binding.
- Apple server notifications and Google RTDN.
- Entitlement timeline, grace/expiry/refund/revocation and restore.
- Catalogue/transaction/entitlement portal.

## EPIC-14 Privacy and compliance

- Data inventory, notices and consent records.
- Export generator and secure delivery.
- Account/household deletion and retention engine.
- Vendor/transfer records and release evidence.
- App Privacy/Data safety/deletion web page.

## EPIC-15 Platform administration

- Portal shell, RBAC and audit.
- User/household metadata views and safe support actions.
- Jobs/webhooks, release compatibility and health dashboards.
- Time-limited content-access escalation.

## EPIC-16 Quality and launch

- Mobile/admin E2E suites and accessibility checks.
- Load/performance and backup restoration.
- Penetration test, incident exercise and remediation.
- Store metadata/assets/review accounts and staged rollout.
