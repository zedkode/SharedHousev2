# EPIC-03-01B - Android secure session recovery

**Date:** 2026-08-08
**Area:** Android / identity / security / documentation
**Status:** completed slice

## Objective

Persist the authenticated Android session with a non-exportable Android Keystore key, rotate the
stored refresh token whenever the API rotates it, restore the account safely after process death,
and remove local credentials on sign-out or terminal session failure.

## What changed

- Added a small session-store boundary consumed by `SharedHouseViewModel`.
- Added an Android implementation using AES-256-GCM and the `AndroidKeyStore` provider.
- Bound ciphertext to the application and storage format, kept it under `noBackupFilesDir`, and used
  an atomic replacement strategy.
- Added a blocking startup state and plain EN/RO recovery/retry feedback without exposing token
  material.
- Extended unit tests for successful restore, rotation, network retry, corrupt storage, failed secure
  writes and local clearing.

## Implementation notes

- Direct platform cryptography is used instead of deprecated `EncryptedSharedPreferences` and
  `MasterKey` APIs. The generated 256-bit key permits only AES/GCM/NoPadding encrypt/decrypt
  operations and requires randomized encryption.
- The file envelope is versioned and size-bounded. A 128-bit GCM tag authenticates ciphertext and
  application-specific additional data; invalid payloads are rejected and made unrecoverable.
- A refresh response is not installed in memory until its newly rotated refresh token has replaced
  the previous encrypted session. If secure persistence fails, the new server session is revoked on
  a best-effort basis and the UI remains signed out.
- Sign-out always attempts both server revocation and local destruction. Local success is reported
  separately when the server cannot be reached.

## Invariants and boundaries

- Raw access and refresh tokens never enter DataStore, logs, backups, UI state or test diagnostics.
- A restored refresh token must be exchanged with the server before household content is shown.
- Every successful refresh rotation must replace the stored session before the retried command is
  accepted locally.
- Terminal server rejection clears local credentials; a network failure retains them for retry.
- Sign-out clears local credentials even when server revocation cannot be confirmed.
- No biometric claim, device-management UI, new backend endpoint, schema migration or notification
  permission is included in this slice.

## Validation

- Android unit suite: 41/41 tests passed across nine suites, including encrypted round-trip,
  ciphertext tampering, atomic-write preservation, restore/rotate ordering, retry and sign-out.
- KMP JVM suites remained green: 1 domain test and 7 network tests.
- Android compile, lint (`No issues found`), debug APK assembly and release manifest/build-config
  processing passed against SDK 36.
- `app-debug.apk`: 24,317,218 bytes; SHA-256
  `DD4B0D301BC9F7014D2C2598AB007901D11BA07922DA6100D93791F08EB6B866`.
- EN/RO parity passed with 474 resource keys in each locale. The root TypeScript/API/contract,
  runtime smoke and dependency audit gates passed after integration; npm reported zero known
  vulnerabilities.

## Migrations and compatibility

- No API, OpenAPI or database migration changed. Existing refresh-token rotation semantics remain
  authoritative.
- Storage format v1 is Android-only and additive. Corrupt, missing or incompatible local data falls
  back to a safe signed-out state; there is no plaintext migration because prior builds persisted no
  token.
- Minimum SDK 26 and target/compile SDK 36 remain unchanged. Release cleartext traffic stays
  disabled.

## Security and privacy review

- Raw token values do not enter DataStore, logs, backups, Compose state or diagnostic output.
- Household content is not loaded from a restored session until the server accepts and rotates its
  refresh token.
- No new permission, analytics SDK, device fingerprint, biometric claim or user-data category was
  added.

## Remaining work

- Add device/session listing and revocation APIs/UI, recent-authentication policy and optional local
  biometric unlock after a dedicated product/security decision.
- Run instrumented process-death and real-hardware Keystore tests on API 26 and current Android,
  including invalidated keys and OEM failure behaviour.
- Continue EPIC-12 with WorkManager/local calendar reminder scheduling and later server-owned FCM
  delivery/deduplication.

## Documentation updated

- `README.md`
- `apps/android/README.md`
- `docs/02-mobile/android.md`
- `docs/09-delivery/task-master.md`
- `memoryagent/INDEX.md`
