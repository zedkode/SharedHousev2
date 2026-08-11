# EPIC-01-02E — Android optimized-startup crash repair

## Outcome

The reproducible public-release startup crash was repaired and SharedHouse `0.5.1` was validated on
the connected Samsung SM-S938B before distribution. The release process now fails closed unless the
signed optimized APK also passes a physical-device startup gate.

## Root cause and repair

- The process crashed in `androidx.startup.InitializationProvider`, before `MainActivity`.
- The nested cause was failure to instantiate `androidx.work.impl.WorkDatabase` during
  `WorkManagerInitializer` startup.
- Google Mobile Ads 1.3.0 requested the obsolete WorkManager 2.7.0 runtime. The optimized AGP 9/R8
  artifact failed during reflective Room database creation even though JVM and lint gates passed.
- The Android catalog and app now pin the supported stable WorkManager 2.11.2 runtime, overriding the
  ads SDK's transitive 2.7.0 request.
- `build-direct-production-android.ps1 -RequireConnectedDevice` now retains app data, installs the
  exact signed artifact, performs five cold starts, checks process survival and the resumed activity,
  and rejects any new crash-buffer entry.

## Validation

- Dependency resolution: `androidx.work:work-runtime:2.7.0 -> 2.11.2`.
- Repository `npm run check`: formatting, lint, typecheck, production builds and OpenAPI 1.10.0 all
  passed; API 43/43, workers 9/9 and contracts 9/9 tests passed.
- KMP JVM tests: domain 1/1 and network 13/13 passed.
- Android public unit tests: 48/48 passed.
- Android `lintPublicRelease`: zero errors; six existing non-blocking resource warnings.
- Optimized public release packaging and APK signature verification passed.
- Physical device: version code 6/version 0.5.1 installed over the prior build, five cold starts
  passed, the process remained alive, `MainActivity` was resumed and no crash occurred. All retained
  crash-buffer records predate the repaired installation; the newest was at 12:30:46 and the repaired
  app was visibly running at 12:43.
- A captured device screenshot confirmed that the Material UI rendered the first tutorial screen.
- Public API liveness and database readiness both returned HTTP 200 after publication.

## Artifact

- Local: `apps/android/app/build/outputs/apk/release/SharedHouse-v0.5.1-public-release-signed.apk`
- VPS archive: `/home/sharedhouse-releases/SharedHouse-v0.5.1-public-release-signed.apk`
- Size: 4,846,671 bytes.
- SHA-256: `C6E40796DBC505AB955BAF1AFDEC33498151785A81EAD5D43DEB855CFA0218DC`.
- Certificate: `CN=SharedHouse Direct Release`, RSA-4096, APK signature schemes v2 and v3.

## Remaining release boundaries

- No emulator was configured, so the debug instrumentation suite was not installed over the user's
  release-signed package; doing so would require uninstalling it and clearing real app data. The new
  optimized-release physical gate covers the failure mode that instrumentation/debug builds missed.
- The six lint warnings are plural-resource recommendations and one unused string, not errors. They
  remain a separate localization-cleanup task and did not block this crash-only release.
- Broader Play launch requirements such as provider credentials, store review and the full device and
  accessibility matrix remain governed by the delivery runbook.
