# EPIC-01-02B - Android 3D brand and named testing APK

**Date:** 2026-08-08
**Area:** Android / branding / delivery / documentation
**Status:** completed slice

## Outcome

- Replaced the launcher foreground with an original soft-3D SharedHouse mark combining a house,
  calendar and completion check.
- Preserved a transparent high-resolution master and generated density-specific adaptive Android
  foreground assets, and displayed the new logo on the welcome screen.
- Added configuration-cache-compatible local/public product profiles. The local profile packages
  as `SharedHouse-v0.1.0-local-testing-signed.apk`; public testing/release tasks require an HTTPS
  endpoint, and production release tasks additionally require external signing credentials.
- Documented the first local account and household setup flow, including emulator and physical
  device API connectivity.

## Files

- `apps/android/design/app-icon/sharedhouse-icon-generated-source.png`
- `apps/android/design/app-icon/sharedhouse-icon-master.png`
- `apps/android/app/src/main/res/drawable-*/ic_launcher_foreground_art.png`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/auth/AuthScreens.kt`
- `apps/android/app/build.gradle.kts`
- `apps/android/app/proguard-rules.pro`
- `apps/android/README.md`
- `README.md`

## Schema and API changes

No database schema, OpenAPI contract, endpoint, permission or domain rule changed.

## Decisions

- The product mark uses a strong, text-free silhouette and remains inside the Android adaptive-icon
  safe zone so launchers can apply different masks.
- The local named APK is intentionally a debug/test artifact. It uses the separate
  `com.sharedhouse.android.local` application ID, is signed with the local Android debug
  certificate and is not represented as a production, upload-key or Play Store build.
- The public profile retains `com.sharedhouse.android`, enforces HTTPS and supports an optimized APK
  and AAB only when owner-controlled signing values are supplied through process environment.
- No private keystore, alias or password was created, requested in chat, or committed. Production
  signing remains a release-operations task using secrets supplied through an approved secure path.

## Validation

- `testLocalDebugUnitTest`, `lintLocalDebug` and `packageLocalTestingApk` completed successfully
  against SDK 36; the public debug source and manifest compiled with a synthetic HTTPS endpoint.
- Gradle Configuration Cache was reused successfully after making the copy task serializable.
- `apksigner verify --verbose --print-certs` passed: APK Signature Scheme v2, one RSA-2048 signer,
  certificate subject `C=US, O=Android, CN=Android Debug`.
- APK identity: `com.sharedhouse.android`, version `0.1.0` (1), min SDK 26, target/compile SDK 36.
- Local artifact size: 23,546,482 bytes. SHA-256:
  `917DBB0A253131FA2739E5E08CE4EA9EDB643FD6854D3203E3C8E2260CAB6952`.
- Negative release gates were exercised: a public build without an HTTPS endpoint failed before
  compilation, and a public release without all four signing variables failed before packaging.
- Packaged permissions remained limited to Internet, notification permission and the internal
  non-exported AndroidX receiver permission.
- The adaptive launcher resource is present for all reported Android densities.

## Security and privacy review

- No signing secret, account credential, personal data, analytics SDK or new runtime permission was
  added.
- The testing certificate is explicitly identified as non-production to avoid misleading release
  claims.
- Local account creation still depends on the development API and development-only verification
  code; real email and a hosted production environment remain unavailable.

## Limitations and next task

- Play Store delivery requires an Android App Bundle, a production application ID/brand decision,
  an owner-controlled upload key, Play App Signing and a deployed HTTPS API.
- The logo has been visually inspected as a master and a 432 px Android density asset, but launcher
  mask appearance should still be checked on real devices and OEM launchers.
- Continue with production environment configuration and device/session management only after
  signing ownership and hosted-service decisions are approved.
