# EPIC-01-02A — Android application foundation

**Date:** 2026-08-01  
**Area:** mobile / documentation  
**Status:** completed

## Objective

Create and verify the first installable Android application shell. Scope is limited to the native
Compose Home foundation, product theme, English/Romanian resources, shared contract linkage and
build configuration. Identity, household creation, persistence, networking and financial domain
behaviour remain out of scope.

## What changed

- Added the `:apps:android:app` module with application ID `com.sharedhouse.android`, minimum SDK 26
  and target/compile SDK 36.
- Added a Material 3 Compose Home shell, system light/dark themes, five top-level destinations and
  accessible English/Romanian labels.
- Added `HomeFoundationState.empty()` backed by shared `ApiContract.VERSION`, with zero household,
  payment, task and request activity.
- Added a launcher icon, disabled application backup/device transfer and configured Android build
  memory for reproducible local compilation.
- Corrected Android JVM test wiring to use the JUnit-backed Kotlin test artifact.

## Business and security rules preserved

- The UI creates no account, household, payment declaration, ledger record or synthetic balance.
- Unavailable actions provide explicit feedback and never claim that setup or synchronisation
  succeeded.
- No dangerous Android permission, network endpoint, analytics SDK, secret or release signing key
  was added.
- Application backup and device-transfer extraction are disabled for this foundation.

## Implementation notes

- The current application identifier and Android debug signing are development-only boundaries;
  release identity and signing still require explicit approval.
- Android UI remains under `apps/android`; the only shared dependency is portable domain contract
  metadata from `shared/domain`.
- Gradle uses a 2 GiB heap because the previous 512 MiB default caused garbage-collector thrashing
  while compiling Android resources and dex files.

## Validation

- `npm run check` — passed formatting, lint, TypeScript checks, 9 tests, all workspace builds and
  OpenAPI validation.
- `.\gradlew.bat :apps:android:app:testDebugUnitTest :apps:android:app:assembleDebug` — passed; 2
  Android unit tests passed with 0 failures.
- `.\gradlew.bat :apps:android:app:lintDebug :shared:domain:jvmTest` — passed; lint retains one
  `ObsoleteSdkInt` warning for the required adaptive-icon `v26` resource folder.
- Debug APK generated at `apps/android/app/build/outputs/apk/debug/app-debug.apk`; it is a debug
  development artefact, not a signed release candidate.
- No emulator/device UI test or screenshot comparison was run in this Windows environment.

## Migrations and compatibility

No API or database schema changed. The application requires Android 8/API 26 or newer and uses API
contract version `v1`. No release upgrade path exists yet because this is version code 1.

## Remaining work

- Create the installable iOS application project on macOS/Xcode and confirm identifiers/signing.
- Add Android navigation and the EPIC-02 progressive onboarding flow.
- Add Compose instrumentation/accessibility tests and screenshot coverage on an emulator/device.
- Recheck the adaptive-icon folder warning after the Android build toolchain is upgraded; moving the
  file to the generic folder currently makes AAPT fail to resolve the launcher icon.
- Connect identity and household setup only in their task-master epics and through versioned APIs.

## Documentation updated

- `README.md`
- `apps/android/README.md`
- `docs/09-delivery/task-master.md`
- `memoryagent/INDEX.md`
