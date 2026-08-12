# UI-002 — Premium v2 UI, household chat and ledger attribution

**Date:** 2026-08-11  
**Area:** Android UI/UX / household chat / Money presentation / documentation  
**Status:** internal public-debug installed and visually verified; owner-signed release and full device gates remain

## Outcome

- Replaced the superseded Android visual direction with the SharedHouse-owned premium-v2 system:
  exact `#0B0C16` base, violet–purple–pink hero gradient, two card depth levels, explicit status
  colours, custom typography/shape scales and a floating animated navigation dock.
- Reworked the five primary household destinations around compact, information-first hierarchy so
  primary actions do not obscure lists or bottom navigation.
- Added a coherent 24 × 24, 1.8 dp rounded-line vector family for primary navigation, household
  categories and status/action glyphs.
- Rebuilt the household conversation presentation around the already-supported chat functions:
  localized day dividers and timestamps, short-window consecutive-sender grouping, deterministic
  initials avatars, real connection status, non-disruptive latest-message scrolling, read-only state,
  2,000-character composer, send loading and explicit load/send recovery with preserved draft.
- Kept chat claims honest: there is no UI for delivery/read receipts, typing, reactions, attachments
  or edits because no such server model or endpoint exists.
- Made payment history attribution explicit in Android: status icon + label + responsible
  declarer/confirmer/disputer/reverser, original declarer context and dispute/reversal reason.
- Confirmed a real household chat send and a real payment history that displayed the declarer
  separately from the actor of a later transition on the installed internal public-debug build. No
  personal data from that verification is recorded here.
- Updated the authoritative design-system, Android implementation and task-master documents without
  advancing incomplete release, cross-platform or device-validation work.

## Main files in the iteration

- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Color.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Type.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Shape.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Theme.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/atmosphere/AtmosphereComponents.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/icons/SharedHouseIcons.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/home/HouseholdDashboardScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/calendar/CalendarScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/calendar/CalendarViews.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/money/MoneyScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/tasks/TasksScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/home/HouseholdHubScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/chat/HouseholdChatScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/chat/ChatIcons.kt`
- `apps/android/app/src/main/res/values/strings.xml`
- `apps/android/app/src/main/res/values-ro/strings.xml`
- `docs/01-ux-ui/design-system.md`
- `docs/02-mobile/android.md`
- `docs/09-delivery/task-master.md`
- `infra/production/README.md`

## Schema and API changes

- OpenAPI remains at 1.13.0 and its response contracts now expose server-derived actor display names:
  task requests include `createdByDisplayName` and `resolvedByDisplayName`; payment summaries include
  `declaredByDisplayName`, `confirmedByDisplayName`, `disputedByDisplayName` and
  `reversedByDisplayName`.
- No endpoint was added for this attribution work. Existing chat list/send/SSE and task/payment
  transition routes remain authoritative; the UI does not infer extra capabilities.
- No database migration was introduced. The deployed schema has 16 recorded migrations and remains
  at `0015_expense_supplier_revisions.sql` as the latest migration.

## Decisions

- The exact authored palette and Foundation primitives are the Android source of truth. Dynamic
  device colour is ignored so status and brand meaning remain stable.
- The current glass-like appearance is intentionally simulated with alpha, gradients, borders and
  shadows. It is not true backdrop blur and must not be described as such.
- Sender and payment actor information is displayed only from server-derived fields. Colour never
  replaces the visible status label or accessible description.
- Expense editing remains a linked revision and removal remains a reasoned reversal. Payment
  correction remains a payment reversal. No history is deleted or silently rewritten.
- Backend publication, Android compilation and Android release signing are separate gates.

## Validation evidence

- `npm run check` passed for formatting, lint, type checking, tests, production package builds and
  contract validation.
- `:apps:android:app:compilePublicDebugKotlin` passed after the premium chat integration.
- `:apps:android:app:lintPublicDebug` passed with zero errors; warnings were non-blocking repository
  resource findings and none targeted the new chat files.
- The internal public-debug build was installed and visually verified. A real household chat send
  completed, and a real payment history displayed its declarer separately from the actor of a later
  transition; no personal data from those checks is retained in this record.
- API liveness and readiness passed after publication, the API restart count was `0`, and the actor
  attribution mismatch check returned `0`.
- The installed output remains an internal debug artifact. These checks do not constitute an
  optimized owner-signed release or the complete physical-device acceptance gate.

## Deployment state

- The API/worker image containing the 1.13.0 actor response contract was published as
  `0.1.0-premium-v2-actor-20260811T2223Z`.
- The validated guarded backup for this rollout is
  `/home/sharedhouse-backups/sharedhouse-20260811T222033Z.dump`.
- The production runbook backup example now uses `/home/sharedhouse-backups`, matching the safety
  boundary enforced by `backup.sh`.
- The rollout required no database migration: the schema contains 16 recorded migrations and its
  latest migration remains `0015_expense_supplier_revisions.sql`.
- API liveness/readiness passed after publication, the API restart count remained `0`, and the actor
  attribution mismatch check returned `0`. This is backend health evidence, not Android release
  evidence.

## Security and privacy review

- Household chat still requires an active authenticated household membership. Read-only members do
  not receive a functional send control.
- Failed chat send preserves the draft and uses the existing idempotent retry path; no optimistic
  “delivered” state is fabricated.
- Message bodies remain absent from public lock-screen content and ordinary audit metadata.
- Money values remain signed 64-bit minor units. Actor attribution and reason display improve audit
  clarity without changing permissions or transaction semantics.
- No secret, signing material, user export, production database content or personal screenshot was
  added.

## Limitations

- No true backdrop blur is implemented; renderer/device-safe layered transparency is the fallback.
- The current build is still internal public-debug. It is not production-ready and must not be
  distributed as a release.
- Installation and visual checks passed, but full device checks remain for safe areas, keyboard/IME,
  large text, TalkBack, high contrast, reduced motion, navigation, notification permission/actions
  and long conversations.
- A real chat send was confirmed, but two authenticated devices are still required to validate
  receive/reconnect behaviour. FCM or APNs background delivery, read receipts, reactions,
  attachments and iOS parity remain absent.
- Light/dark EN/RO screenshot evidence and foldable/tablet visual review remain release evidence
  tasks.

## Next task

Use the owner's established signing identity to build an optimized APK/AAB, then complete the
owner-observed accessibility, notification/quick-action and two-device chat gates. Record artifact
identity, certificate, screenshots and device results separately before any distribution or
production-ready claim.
