# EPIC-03-14B account export and deletion

## Outcome

Implemented password-confirmed account export and deletion across the OpenAPI/TypeScript contract,
Nest API, KMP client and Android Settings. Added a same-origin public deletion page for the Google
Play external deletion URL.

## Schema and API

- Migration `0005_privacy_requests.sql` records completed exports and completed/blocked deletions
  without storing export payloads.
- `POST /v1/account/export` returns versioned JSON containing the current implemented profile,
  household memberships, user-created calendar events, consent records, sessions and invitations.
- `DELETE /v1/account` requires the current password and literal `DELETE` confirmation.
- `GET/POST /account-deletion` provides the HTTPS same-origin web deletion path.
- OpenAPI v1 document advanced from 1.3.0 to 1.4.0.

## Deletion decisions

- All sessions are revoked and password credentials/challenges/idempotency records are removed.
- The email is replaced with a non-routable per-user tombstone and the display name becomes
  `Former member`, preserving stable foreign keys for shared historical records.
- A sole-member owned household is closed and its pending invitations/events are revoked/deleted.
- For a shared household, ownership transfers deterministically to the longest-standing active
  admin, otherwise member. Deletion is blocked if only read-only successors exist.
- Store subscriptions are explicitly described as separately managed.

## Android UX

- Material 3 export and destructive confirmation dialogs require the current password.
- Export uses the Android system document picker; the app does not silently write to shared storage.
- Deletion success clears the encrypted local session. Failures and ownership blockers remain honest.
- English and Romanian strings are present.

## Validation

- API suite covers wrong-password export/deletion, export content, anonymisation, session revocation,
  sole-home closure, deterministic ownership transfer, read-only blocker and public form.
- Android unit tests cover local-session clearing after successful deletion.
- Public Android compile, lint, tests, KMP network tests and named APK packaging passed during the
  implementation run.

## Remaining launch work

- Live VPS/Cloudflare/Resend deployment and device evidence.
- Password reset and device/session management.
- Provider-created Firebase/AdMob/Play configuration, legal pages/declarations and store review.
- Future ledger/media records must be added to the export and retention engine when implemented.
