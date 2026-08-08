# EPIC-01-02C - Android local and public profiles

**Date:** 2026-08-08
**Area:** Android / environments / signing / delivery
**Status:** completed configuration slice

## Outcome

- Added separate `local` and `public` Android product profiles.
- The local profile uses `com.sharedhouse.android.local`, the `SharedHouse Local` label, a
  configurable HTTP/LAN endpoint and debug signing, so it can coexist with the public app.
- The public profile keeps `com.sharedhouse.android`, disables cleartext traffic, requires a real
  HTTPS API and supports debug-signed environment testing plus optimized owner-signed APK/AAB
  output.
- Unsafe public variants are disabled from aggregate builds when the endpoint or release signing
  configuration is absent. Explicit public tasks fail with actionable messages.

## Files

- `apps/android/app/build.gradle.kts`
- `apps/android/app/proguard-rules.pro`
- `apps/android/README.md`
- `README.md`
- `memoryagent/INDEX.md`

## Schema and API changes

No database, OpenAPI, endpoint or domain-model change. Build-time API routing changed from one
debug/release field to environment-specific product flavors.

## Decisions

- Local and public package IDs differ to prevent test data and credentials from overwriting the
  public installation.
- Local release is disabled; local distribution remains explicitly debug/test signed.
- Public release secrets are accepted only from the current process environment. No keystore,
  password, alias or signing property is stored in the repository.
- Public release enables R8 code/resource optimization and can create a named APK or Play Store AAB.

## Validation

- Local: 41/41 unit tests passed, lint reported zero issues, APK assembly and named packaging passed.
- Local APK: 23,546,482 bytes; SHA-256
  `917DBB0A253131FA2739E5E08CE4EA9EDB643FD6854D3203E3C8E2260CAB6952`.
- `apksigner` verified APK Signature Scheme v2 and one Android debug RSA-2048 certificate.
- `aapt` verified ID `com.sharedhouse.android.local`, version `0.1.0-local`, min SDK 26, target SDK
  36 and label `SharedHouse Local`.
- Public source and manifest compiled with a synthetic HTTPS URL; the merged manifest disables
  cleartext traffic.
- Negative gates passed: public packaging without a deployed URL failed before compilation, and
  public release packaging without all signing variables failed before creating an artifact.

## Security and privacy review

- Public cleartext traffic is prohibited and a placeholder endpoint cannot produce a public build.
- No secret or personal data was added. Debug signing is clearly separated from production signing.
- Existing Keystore session isolation follows the distinct application IDs automatically.

## Limitations and next task

- No public artifact was produced because no deployed SharedHouse HTTPS API or owner-controlled
  release/upload key was supplied.
- A public service still needs deployed PostgreSQL/API infrastructure, real email verification,
  DNS/TLS, monitoring, privacy/support workflows and release/store review.
- Next: choose the public API hostname and hosting environment, deploy and health-check the API,
  then build with an owner-controlled upload key and verify the final APK/AAB certificate.

