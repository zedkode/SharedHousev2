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
  no longer depends on Material 3 components. `UI-002` now has a SharedHouse-owned premium-v2
  Foundation system with the authored `#0B0C16` base, violet–purple–pink hero gradient, layered
  cards, floating animated navigation and a 24 × 24 rounded-line primary/domain/status icon family.
  Home, Money, Tasks, Calendar, House and household chat use the compact v2 hierarchy while
  preserving actions, permissions, financial/status semantics and EN/RO behaviour. Kotlin compile
  and public-debug lint evidence exists; physical-device large-text/TalkBack/reduced-motion evidence,
  an optimized owner-signed artifact, iOS parity and the full cross-platform catalogue remain.
- [in progress] Native navigation and responsive layouts. The Android account/household gate is
  followed by a phone navigation bar or large-screen navigation rail, and multi-household accounts
  can switch their active home. Root content and custom dialogs respect safe drawing insets;
  physical device-size and foldable validation remain.
- [in progress] Language detection and English/Romanian resources. The Android vertical has parity;
  remaining platforms and content still require coverage.
- [in progress] Appearance, accessibility settings and progressive tutorial. The Android product
  supports a branded light/dark/system identity with text scale, high contrast, reduced motion and a
  skippable persisted first-run tutorial. The retained dynamic-colour parameter is intentionally
  ignored so device colours cannot override financial/status semantics. Physical-device
  accessibility validation and iOS parity remain.

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
- [completed] Membership roles and owner-transfer invariant. Creation atomically provisions the
  owner membership. The production API and Android Household panel now list members, enforce
  owner/admin delegation boundaries, change roles, suspend/reactivate/remove access, retain an
  append-only membership history and transfer ownership atomically with optimistic locking.
- [in progress] Secure invitation creation, preview, acceptance, revocation and deep links. Hashed,
  expiring, single-use and optionally email-restricted codes plus the Android management/join flows
  are implemented; provider email delivery and Android/iOS App Links remain.
- [in progress] Cross-household authorisation test suite. Household, invitation and membership
  reads/mutations, inviter/admin delegation boundaries, suspension, token/action replay, optimistic
  conflicts, owner transfer and tenant hiding are covered; household closure remains.

## EPIC-05 Cycles and recurrence

- [in progress] Local-date/IANA-timezone recurrence engine. Expense schedules now evaluate due dates
  in the household timezone and preserve month-end/yearly anchors. Expense and fixed-assignee chore
  schedules can be open-ended or stop after an inclusive final date; reusable series editing remains.
- [in progress] Fourteen-day, monthly, weekly and custom rules. Expenses support weekly,
  fortnightly, monthly, quarterly and yearly cadence; chores support weekly, fortnightly and monthly.
- [in progress] Deterministic occurrence keys, edits and series scope. Recurring expenses have an
  exact-once template/date key and future-only template edits. Chores have an exact-once series/date
  key and rolling 90-day generation; whole-series chore editing remains.
- DST and invalid-date property tests.

## EPIC-06 Ledger

- [in progress] Money/currency value objects. API and Kotlin use exact minor-unit values and the
  configured household ISO currency; floating-point arithmetic is not used.
- [in progress] Expenses, revisions, allocations and all split methods. The first production-shaped
  vertical supports one-off expenses, an optional supplier, deterministic equal allocation, member
  proposals, owner/admin approval, append-only reasoned reversal, audited owner/admin revision of
  unsettled expenses and exact-once generated occurrences. Fixed, percentage, weighted, usage and
  custom splits remain.
- [in progress] Recurring cost administration. Owner/admin can create, edit and archive reusable
  weekly/fortnightly/monthly/quarterly/yearly rent, bill and custom-category templates with an
  optional inclusive final date. A production worker creates each due occurrence transactionally,
  advances or finishes the schedule, preserves local date anchors and exposes its generated origin
  in Android.
- [completed] Payment declaration/confirmation/dispute/reversal. Members declare only their own
  approved allocation; another active writer confirms or disputes it, corrections are append-only,
  active payments block charge reversal, and Android/export expose the complete history. Android
  labels the current declaration status with the responsible declarer/confirmer/disputer/reverser,
  retains original declarer attribution and displays dispute/reversal reasons. OpenAPI 1.13.0
  exposes the server-derived display name for each of those payment actors. An installed internal
  public-debug check confirmed that a real history keeps the declarer separate from the actor of a
  later transition; no personal data is retained in this delivery record.
- [in progress] Dashboard and explanation read models. Android shows authoritative personal and
  household approved totals, filters, exact allocations and rounding explanations in EN/RO.
- [in progress] Integrity checker and reconciliation tools. The database has a deferred allocation
  total constraint and API tests cover remainder allocation, role checks and tenant isolation;
  operational reconciliation commands remain.

## EPIC-07 Utilities

- Electricity/gas/internet categories.
- Meter accounts/readings/photos and estimation labels.
- Usage allocation with standing-charge handling.

## EPIC-08 Chores

- [in progress] Task occurrences, zones and assignment. Owner/admin creation, active-member
  assignment, priority, local deadline, estimate, start/complete/cancel/reopen, fixed-assignee
  weekly/fortnightly/monthly recurrence, reasoned series stopping and Android task-board filters are
  implemented; reusable templates, schedule editing and assignment rotation remain.
- Balanced/round-robin fairness and exemptions.
- [in progress] Completion evidence and issue reporting. Completion notes and issue reports are
  append-audited; photo proof and explicit supply/safety classifications remain.
- [completed] Swap/help/postpone request state machine. Assignees submit typed pending requests;
  owner/admin decisions are version-checked and only approved swap/postpone requests alter the task.
  OpenAPI 1.13.0 exposes `createdByDisplayName` and `resolvedByDisplayName`, and Android presents the
  requester separately from the decision actor. API tests cover help, swap, postpone, stale versions,
  roles, exports and tenant isolation.

## EPIC-09 Calendar

- [in progress] Unified event query/read model. Authenticated one-off household events are stored,
  tenant-isolated and exposed through versioned CRUD; Android merges authoritative expense and task
  occurrences into the calendar. A server-side unified cursor and shopping events remain.
- [in progress] Agenda/week/month and filters. Android now provides interactive week, month,
  quarter and year views, Today/period navigation, day selection and event detail/create/edit/delete
  sheets. Creation includes type, local date, all-day or start/end time, reminder, description and a
  live household-facing preview; advanced type/member filters remain.
- Avatar assignment rendering and amount/status display.
- [in progress] Conflict, recurring-edit and action-sheet behaviours. Role-aware actions,
  idempotent creation and optimistic conflict feedback exist; whole-series calendar editing is not
  yet supported.

## EPIC-10 Shopping

- Lists/items, claiming, substitution and actual prices.
- Receipt media, approval policy and duplicate detection.
- Purchase-to-ledger reimbursement link.

## EPIC-11 Offline sync

- [in progress] Foreground refresh. Android refreshes authoritative calendar, money, tasks and
  membership projections every five seconds without clearing ready content on transient failure;
  durable local projections and push/SSE invalidation remain.
- SQLDelight schema/migrations.
- Outbox with client operation IDs.
- Cursor pull, tombstones, retries and conflict UI.
- Removed-membership/offline safety tests.

## EPIC-12 Notifications

- [in progress] Preferences, quiet hours, lead times and lock-screen privacy. Android persists
  category toggles, quiet hours, reminder lead time, sound/vibration, creates seven scoped channels
  and uses a generic public lock-screen version for scheduled money/task and foreground chat
  notifications.
- Push tokens, notification jobs, delivery telemetry and inbox.
- [in progress] High-priority household design without emergency-alert imitation. Android uses a
  contextual runtime-permission explanation and system settings shortcut. WorkManager schedules
  known money/task reminders and exposes optimistic-version Start/Complete actions for assigned
  tasks; provider-backed remote delivery remains absent.

## EPIC-17 Household chat

- [in progress] Append-only tenant-scoped household messages, idempotent send, incremental history,
  authenticated SSE delivery and an interactive Android conversation surface are implemented.
  Android shows the real connection state, groups consecutive messages with sender/avatar/date/time,
  preserves failed-send drafts and falls back to a bounded incremental refresh on reconnect.
  Migration and protected list/send/SSE routes remain live under image tag
  `0.1.0-premium-v2-actor-20260811T2223Z`; a real send was confirmed from the installed internal
  public-debug build. A two-device authenticated receive/reconnect gate, provider-backed background
  push, read receipts, reactions, media and iOS parity remain.

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
- [in progress] Production-only distribution gate. Debug variants remain available strictly for
  compiler/lint/unit-test coverage, but named debug APK packaging has been removed; user delivery
  accepts only an optimized public APK/AAB. A stable direct-install signing identity and fail-closed
  production build support core operation with Firebase/AdMob explicitly disabled. The premium-v2
  internal public-debug build is installed and visually verified, including a real chat send and the
  separation of payment declarer and transition actor. This is not the owner-signed release gate;
  accessibility, notification/quick-action, multi-device chat and optimized APK/AAB acceptance still
  remain. The owner signing gate, Play upload identity, Firebase production file and real AdMob IDs
  remain store-channel blockers.
