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
  authentication, dashboard, calendar, household and settings surfaces use the expanded Material 3
  product theme; the full cross-platform component catalogue remains.
- [in progress] Native navigation and responsive layouts. The Android account/household gate is
  followed by a phone navigation bar or large-screen navigation rail, and multi-household accounts
  can switch their active home; physical device-size and foldable validation remain.
- [in progress] Language detection and English/Romanian resources. The Android vertical has parity;
  remaining platforms and content still require coverage.
- [in progress] Appearance, accessibility settings and progressive tutorial. System light/dark and
  dynamic colour overrides, text scale, high contrast, reduced motion and a skippable persisted
  first-run tutorial are implemented on Android; iOS parity remains.

## EPIC-03 Identity and account security

- [in progress] Registration, verification, sign-in, rotating sessions and devices. The API and
  Android vertical work locally and have a production Resend verification path with replacement
  codes, encrypted transactional outbox, bounded retries and generic enumeration-safe responses.
  Android persists/rotates sessions through AES-256-GCM with a non-exportable Keystore key;
  live-provider validation, recent-authentication and device management remain.
- Profile/avatar and secure media pipeline.
- [in progress] Re-authentication, export and deletion. Android can save a password-confirmed JSON
  export and delete the account; deletion revokes sessions, anonymises identity, closes sole-member
  homes and transfers shared-home ownership to the longest-standing eligible admin/member. Password
  reset, device management and subscription linkage remain.
- Admin MFA and policy-based RBAC.

## EPIC-04 Household and invitations

- [in progress] Household configuration and lifecycle. Authenticated create/list/get/update with
  idempotency and optimistic version checks are implemented.
- [in progress] Membership roles and owner-transfer invariant. Creation atomically provisions the
  owner membership; role management and transfer remain.
- [in progress] Secure invitation creation, preview, acceptance, revocation and deep links. Hashed,
  expiring, single-use and optionally email-restricted codes plus the Android management/join flows
  are implemented; provider email delivery and Android/iOS App Links remain.
- [in progress] Cross-household authorisation test suite. Household and invitation reads/mutations,
  inviter role boundaries, token replay and tenant hiding are covered; broader membership role and
  owner-transfer coverage remains.

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

- [in progress] Unified event query/read model. Authenticated one-off household events are stored,
  tenant-isolated and exposed through versioned CRUD; generated chore/bill/shopping events remain.
- [in progress] Agenda/week/month and filters. Android now provides interactive week, month,
  quarter and year views, Today/period navigation, day selection and event detail/create/edit/delete
  sheets; advanced type/member filters remain.
- Avatar assignment rendering and amount/status display.
- [in progress] Conflict, recurring-edit and action-sheet behaviours. Role-aware actions,
  idempotent creation and optimistic conflict feedback exist; recurrence is intentionally not yet
  supported.

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

- [in progress] Preferences, quiet hours, lead times and lock-screen privacy. Android persists
  category toggles, quiet hours, reminder lead time, sound/vibration and creates six scoped channels;
  lock-screen content policy still requires implementation.
- Push tokens, notification jobs, delivery telemetry and inbox.
- [in progress] High-priority household design without emergency-alert imitation. Android uses a
  contextual runtime-permission explanation, system settings shortcut and local test notification;
  remote delivery remains absent.

## EPIC-13 Store commerce

- [in progress] Android monetisation/measurement foundation. Firebase Analytics/Crashlytics and GMA
  Next-Gen with UMP are integrated behind local opt-in and release configuration gates; Play Billing,
  entitlements and production provider validation remain.
- StoreKit and Google Play Billing adapters.
- Backend verification and account binding.
- Apple server notifications and Google RTDN.
- Entitlement timeline, grace/expiry/refund/revocation and restore.
- Catalogue/transaction/entitlement portal.

## EPIC-14 Privacy and compliance

- [in progress] Data inventory, notices and consent records. Android optional analytics, crash and ad
  choices persist locally and default off; production notices, server consent history and withdrawal
  evidence remain.
- [in progress] Export generator and secure delivery. The current implemented account, household,
  calendar, consent, session and invitation data is generated synchronously and saved through the
  Android system document picker; large future media/ledger exports still require worker storage.
- [in progress] Account/household deletion and retention engine. Immediate credential removal,
  session revocation, identity anonymisation, household closure/ownership succession and request
  evidence are implemented; vendor propagation and future financial retention rules remain.
- Vendor/transfer records and release evidence.
- [in progress] App Privacy/Data safety/deletion web page. A same-origin, password-confirmed public
  deletion route exists at `/account-deletion`; privacy/terms/support pages and live deployment remain.

## EPIC-15 Platform administration

- Portal shell, RBAC and audit.
- User/household metadata views and safe support actions.
- Jobs/webhooks, release compatibility and health dashboards.
- Time-limited content-access escalation.

## EPIC-16 Quality and launch

- Mobile/admin E2E suites and accessibility checks.
- Load/performance and backup restoration.
- Penetration test, incident exercise and remediation.
- [in progress] Store metadata/assets/review accounts and staged rollout. The detailed Google Play,
  Firebase, AdMob, signing and Data safety runbook exists; real accounts/assets and closed-track
  evidence remain.
