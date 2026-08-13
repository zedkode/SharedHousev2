# Android Implementation Specification

## Technology

- Kotlin and Jetpack Compose.
- SharedHouse-owned Compose Foundation components and theme tokens; Material 3 is not the component
  or theme runtime.
- AndroidX lifecycle/navigation abstractions for the authenticated household shell.
- WorkManager for deferrable sync and scheduled local reminders.
- Firebase Cloud Messaging is the intended push transport, with server-owned notification logic;
  provider-backed background delivery is not yet configured.
- Google Play Billing Library for future digital subscriptions.
- Android Keystore-backed encrypted credential storage.
- System Photo Picker for future avatars and receipt images; broad storage access is prohibited.

## Implemented premium v2 UI

- `ui/theme/` owns the exact dark/light/high-contrast palette, Sans type scale and 8/12/16/20/28 dp
  shape scale. Device dynamic colour is currently ignored so it cannot corrupt brand or status
  semantics.
- `ui/atmosphere/` owns cards, buttons, text fields, chips, dialogs, sheets, scaffold, navigation and
  safe-area behaviour. Screens may not silently fall back to default Material component styling.
- Home, Calendar, Money, Tasks and House use compact information-first layouts. Primary creation
  actions are placed where they do not cover list content or the bottom dock.
- The floating phone dock and large-screen rail use SharedHouse navigation vectors and explicit
  selected semantics. All icon-only controls retain at least a 48 dp touch target.
- The SharedHouse vector family uses 24 × 24 monochrome paths with 1.8 dp rounded strokes. Status
  remains icon + visible label + accessible description.
- The current “glass” appearance is translucent layering, gradients, borders and shadows. It is not
  a true backdrop blur; see `docs/01-ux-ui/design-system.md` for the portability/performance limit.
- English and Romanian resources are updated together. Dates, times, amounts and plural forms use
  locale-aware formatters/resources rather than concatenated grammar.

The v3 depth pass preserves this palette and information architecture while changing the physical
treatment: 36 dp heroes, 28 dp metrics, 24 dp list cards, two-stage coloured/contact shadows,
subtle neutral gradients, top-left highlights, independently lit icon badges and 120 ms pressed
feedback. Home, Money, Tasks, Calendar and House share the same ambient background composition;
no financial, permission, sync or API behaviour is changed by this visual layer.

## Launcher identity and startup experience

- The Android launcher and system splash use the SharedHouse-owned house-and-three-people mark on
  the fixed `#0B0C16` brand background. Density-specific launcher art is derived from one
  transparent master so adaptive and legacy icons retain the same safe-zone proportions.
- The Compose startup surface continues the system splash with the same mark, a restrained
  violet-purple-pink glow and a three-dot indeterminate progress indicator. Motion is removed when
  reduced motion is enabled; progress and logo semantics remain available to TalkBack.
- Session restoration is resolved before any account name is presented. A confirmed account sees a
  localized personalized welcome; a missing session sees a neutral guest welcome. No stale local
  display name is exposed while credential restoration is still in progress.

## Household chat

The Android chat consumes only the implemented tenant-scoped contract:

- initial/incremental authenticated history read;
- idempotent append-only send;
- authenticated server-sent event stream while the conversation is open;
- bounded incremental reload and visible `Connecting`, `Live`, `Reconnecting` or `Offline` state;
- role-derived read-only composer state;
- a 2,000-character server/client limit.

The conversation groups consecutive messages from the same displayed sender within a short time
window and renders a date separator, sender name, deterministic initials avatar, localized timestamp
and TalkBack message description. It keeps a user's draft during send failure, shows real sending
progress and offers explicit reconnect/retry. Auto-scroll follows a newly confirmed own message or
a reader already near the latest message; it does not pull a reader away from older history for an
incoming message.

The UI does not invent delivery/read receipts, typing state, reactions, attachments or message
editing because the current model/endpoints do not support them. “Live” means the foreground SSE
stream is connected; it does not mean provider-backed background push is active. Message text stays
out of public lock-screen notification content and normal audit metadata.

## Actor attribution and Money correction presentation

- Task request summaries carry server-derived `createdByDisplayName` and
  `resolvedByDisplayName`. Android identifies the requester and, for a completed decision, the
  separate resolver beside the status icon and label.

- Money displays exact signed 64-bit minor-unit values with the household settlement currency. It
  does not claim SharedHouse moved money.
- A payment history row renders a status icon, localized status label and the responsible server
  actor: declarer, confirmer, disputer or reverser. After a later transition, the original declarer
  remains visible separately. Dispute/reversal reasons remain visible in history.
- `Mark as paid` creates a declaration. Another permitted active writer may confirm or dispute it;
  correction is a reasoned payment reversal with optimistic version and capability checks.
- An expense cannot be revised or reversed while it has active payment declarations. The UI first
  exposes the permitted payment correction action instead of hiding or deleting those records.
- Owner/admin expense editing creates an idempotent linked replacement and reverses/supersedes the
  original with a reason. A reasoned expense reversal changes status and preserves allocations,
  linked revisions and audit evidence; there is no destructive expense delete.
- Optional supplier data is presentation metadata and does not alter settlement or payment state.

## Platform behaviour

- Follow the user's system/light/dark choice while preserving the authored brand fallback,
  high-contrast, text-scale and reduced-motion controls.
- Request `POST_NOTIFICATIONS` contextually after explaining value.
- Define notification channels by category: money reminders, tasks, requests, household
  announcements, household chat, security and subscription.
- Allow users to open Android channel settings from the app.
- Use high importance only for user-selected high-priority household alerts and security events.
- Do not request exact alarms for routine reminders; server push and WorkManager/local scheduling
  are sufficient.
- Do not request background location, contacts, SMS, phone, broad storage or accessibility-service
  permissions.

## Implemented secure-session behaviour

- Android encrypts the complete rotating session envelope with AES-256-GCM and a non-exportable
  `AndroidKeyStore` key; token material is never written to DataStore, logs, backup storage or UI
  state.
- Ciphertext is application/format-bound with authenticated additional data and atomically replaced
  in `noBackupFilesDir` after sign-in, verification and every successful refresh rotation.
- Process startup exchanges the stored refresh token before presenting household content. A
  network failure retains the credential for explicit retry; a terminal rejection, corrupt payload
  or sign-out makes the local session unrecoverable.
- The flow does not claim biometric unlock or device management. Those require dedicated policy,
  backend endpoints and instrumented device validation.

## Current verification and delivery snapshot

As recorded on 2026-08-11:

- `npm run check` passed for the TypeScript/contracts/backend workspace.
- OpenAPI remains version `1.13.0` and now exposes actor display-name fields on task-request and
  payment summaries. This is a response-contract change, not a new endpoint or database migration.
- The current API/worker image tag is `0.1.0-premium-v2-actor-20260811T2223Z`; API liveness and
  readiness passed, the API container restart count was `0`, and the production actor-attribution
  mismatch check returned `0`.
- The guarded production backup is
  `/home/sharedhouse-backups/sharedhouse-20260811T222033Z.dump`.
- No database migration was added. The schema contains 16 recorded migrations and its latest file
  remains `0015_expense_supplier_revisions.sql`.
- Android Kotlin compilation and public-debug lint passed during the UI integration. The Android
  **internal public-debug** artifact was installed and visually verified. A real household chat send
  was confirmed, and real payment history displayed the declarer separately from the later
  transition actor; no member names or other PII are recorded here. This is not a production/store
  release and must not be described as production-ready.
- Distribution remains blocked until the owner provides the established signing identity, an
  optimized owner-signed APK/AAB is built, and the remaining device gates pass for large text,
  TalkBack, reduced motion, notifications, quick actions and multi-device chat/reconnect.

Backend deployment health does not satisfy the Android signing/device gate; these are separate
release claims.

## Store baseline

The release branch must target the applicable Google Play requirement. As of 31 August 2026, new
apps and updates must target Android 16/API 36 or higher. Use Android App Bundles, Play App Signing,
internal testing, closed testing and staged production rollout.

## Data safety preparation

Maintain a machine-readable data inventory mapped to Google Play Data safety answers. Any SDK
change triggers privacy/data-safety review. The app must expose an in-app account deletion path and
a public web deletion request path.

## Testing obligations

- JVM unit tests for shared and Android-specific logic.
- Compose UI tests for critical flows.
- Screenshot/device tests across dark/light, English/Romanian and supported font scales.
- Instrumented tests for notification channels/actions, deep links, process death and secure
  storage.
- Two authenticated household devices for chat send/receive/reconnect and foreground/background
  boundary evidence.
- Play Billing test products and licence testers for every future subscription lifecycle state.
