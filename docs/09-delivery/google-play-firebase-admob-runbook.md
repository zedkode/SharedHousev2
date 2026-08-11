# Google Play, Firebase and AdMob production runbook

**Verified against official Google documentation:** 8 August 2026  
**Android package:** `com.sharedhouse.android`  
**Current target SDK:** 36  
**Production API:** `https://houseapi.dohotstudio.com`

**Live preflight on 8 August 2026:** the hostname did not resolve in public DNS, so public sign-in is
blocked until the Cloudflare Tunnel route and VPS deployment are active. Complete the
[interactive VPS deployment guide](../../infra/production/README.md), then require HTTP 200 from
both `/v1/health` and `/v1/health/ready` before installing a public release candidate.

This is an operator checklist, not a claim of legal or store approval. Complete every evidence item
against the exact release binary. Never copy passwords, service credentials, signing stores or real
user data into the repository, tickets or screenshots.

## 1. What is already implemented

- Firebase Analytics and Crashlytics dependencies use Firebase BoM `34.16.0`.
- Google Mobile Ads uses GMA Next-Gen SDK `1.3.0`; UMP `4.0.0` manages required advertising privacy
  messages.
- Analytics, Crashlytics and ads are disabled by default. The user controls them independently in
  **Settings > Privacy and optional services**.
- No SharedHouse account ID, email, household ID/name, invitation, calendar text or financial value
  is assigned to Firebase or ad requests by application code.
- Ads appear only as a labelled adaptive banner in Guides, after both the SharedHouse switch and UMP
  permit requests. Authentication, dashboard, household management and calendar remain ad-free.
- Local/public debug builds use Google's demo app and banner IDs. A public release fails if demo IDs,
  Firebase configuration, HTTPS API configuration or owner signing material are missing.
- The repository ignores `google-services.json`; each environment receives it through the protected
  release workspace.

Firebase Cloud Messaging, App Check/Play Integrity, Remote Config and Play Billing are deliberately
not enabled yet: each needs a backend lifecycle, retention/deletion rules and tests. Merely adding an
SDK would create data collection without delivering a complete feature.

## 2. Accounts and ownership — complete first

1. Decide whether the publisher is a person or a legal organisation. Use an organisation account if
   the app is owned by a company and prepare matching legal name, address, phone, payment profile and
   D-U-N-S details where requested.
2. Create role-based accounts instead of sharing one Google password: Play owner/release manager,
   Firebase owner/Android maintainer, AdMob administrator/payments contact and privacy/support owner.
3. Enable MFA on all accounts. Store recovery codes in an offline or organisational secret manager.
4. Record who may publish, rotate keys, change Data safety, access crashes or alter ad payments and
   consent configuration.
5. Publish public pages, accessible without sign-in: privacy EN/RO, terms, support and account
   deletion, plus a developer website with root `/app-ads.txt`.

Suggested routes are `https://dohotstudio.com/sharedhouse/privacy`, `/terms`, `/support` and
`/delete-account`. They are placeholders until actually published and tested; do not enter dead URLs
in Play Console.

## 3. Create the Google Play app

1. In Play Console choose **Create app** and enter only factual declarations.
2. Fix the package permanently as `com.sharedhouse.android`; never upload
   `com.sharedhouse.android.local`.
3. Enable Play App Signing. Google protects the app-signing key; retain a separately backed-up upload
   key for future submissions.
4. Begin with **Internal testing**. A new personal account created after 13 November 2023 must plan a
   closed test with at least 12 continuously opted-in testers for 14 days, then request production
   access. Confirm what the specific Play account displays.
5. Create a reviewer account backed only by synthetic data. Document verification, household creation
   and invitation-code paths, and keep the public API available during review.

The app already targets API 36, matching Google's stated Android 16 requirement for new apps and
updates from 31 August 2026. Recheck immediately before every submission.

## 4. Create and configure Firebase

1. Create a production Firebase project, for example `sharedhouse-production`. Choose the correct
   Analytics region and optional data-sharing settings after privacy review.
2. Add Android package `com.sharedhouse.android` and download `google-services.json` to:

   ```text
   apps/android/app/src/public/google-services.json
   ```

   It is environment-specific and intentionally excluded from Git. Restrict its API key in Google
   Cloud Console to the Android package and production signing certificate where supported.
3. Add SHA-256 and SHA-1 fingerprints for the Play app-signing certificate and upload certificate.
   Inspect a local certificate with:

   ```powershell
   keytool -list -v -keystore C:\secure\sharedhouse-upload.jks -alias sharedhouse-upload
   ```

4. Enable Crashlytics, but keep automatic collection off. SharedHouse enables it only after the local
   choice. Do not call `setUserId` or attach household content in custom logs/keys.
5. Enable Analytics only after privacy notice, retention, access permissions and Data safety are
   ready. Collection remains off per device until opt-in. Do not enable remarketing from household
   activity.
6. Link Firebase to Play only after package and signing are final; grant minimum roles.
7. On an internal-test device prove: no pre-opt-in events; approved anonymous events after Analytics
   opt-in; a debug-only controlled crash after Crashlytics opt-in; and no new collection after
   withdrawal/restart. Never ship a permanent crash-test control.

## 5. Create and configure AdMob

1. Verify AdMob publisher identity, payment profile and tax details.
2. Add the unpublished Android app with package `com.sharedhouse.android`; link its Play listing later.
3. Create one banner unit named `Guides adaptive banner`. Interstitial, app-open and rewarded ads are
   outside this release and should not be created merely to increase impressions.
4. Keep the App ID (`ca-app-pub-...~...`) and banner ID (`ca-app-pub-.../...`) in the protected build
   environment, never source files.
5. In **Privacy & messaging**, configure the EEA/UK consent message through Google's certified UMP
   flow and any applicable US-state messages, with equally prominent choices.
6. Use demo IDs in debug and register physical test devices when testing production-looking ads. Do
   not tap live ads during development.
7. Publish AdMob's exact personalised seller line at `https://dohotstudio.com/app-ads.txt`, add the
   developer domain to Play contact details, verify HTTP 200/robots access, and wait for AdMob
   ownership verification. Never copy the example publisher ID from documentation.
8. Complete AdMob app-readiness review and verify ad content is compatible with the app rating.

## 6. Create the production upload key

Run once on a trusted administrative workstation. Enter strong unique passwords interactively so
they never appear in shell history:

```powershell
New-Item -ItemType Directory -Force -Path C:\secure | Out-Null
keytool -genkeypair -v `
  -keystore C:\secure\sharedhouse-upload.jks `
  -alias sharedhouse-upload `
  -keyalg RSA -keysize 4096 -validity 10000
```

Create two encrypted backups in separate controlled locations and store alias/passwords in a secret
manager. A leak requires immediate Play key-reset handling.

## 7. Build the release AAB and APK

Use a clean reviewed checkout. `VERSION_CODE` must increase for every Play upload.

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:SHAREDHOUSE_VERSION_CODE = "2"
$env:SHAREDHOUSE_VERSION_NAME = "0.2.0"
$env:SHAREDHOUSE_RELEASE_STORE_FILE = "C:\secure\sharedhouse-upload.jks"
$env:SHAREDHOUSE_RELEASE_STORE_PASSWORD = Read-Host "Store password"
$env:SHAREDHOUSE_RELEASE_KEY_ALIAS = "sharedhouse-upload"
$env:SHAREDHOUSE_RELEASE_KEY_PASSWORD = Read-Host "Key password"
$env:SHAREDHOUSE_ADMOB_APP_ID = "ca-app-pub-REPLACE_WITH_REAL_APP_ID"
$env:SHAREDHOUSE_ADMOB_BANNER_ID = "ca-app-pub-REPLACE_WITH_REAL_BANNER_ID"

.\gradlew.bat clean `
  :apps:android:app:testPublicDebugUnitTest `
  :apps:android:app:lintPublicRelease `
  :apps:android:app:copyPublicReleaseBundle `
  :apps:android:app:packagePublicReleaseApk
```

Expected outputs:

```text
apps/android/app/build/outputs/bundle/release/SharedHouse-v0.2.0-public-release-signed.aab
apps/android/app/build/outputs/apk/release/SharedHouse-v0.2.0-public-release-signed.apk
```

Clear release variables afterwards. Inspect the AAB with Bundle Explorer, verify package, version and
permissions, then retain hashes, commit SHA, dependencies, lint and tests. Debug-signed binaries are
internal build by-products and must never be distributed.

## 8. Complete Play declarations from evidence

- **App access:** reviewer credentials, verification instructions, synthetic household and invite.
- **Ads:** answer yes because an optional Guides banner exists, even when users may disable it.
- **Target audience:** adults only for the current product; creative and copy must match.
- **Content rating:** answer from actual app and ad behaviour.
- **Data safety:** include SharedHouse processing and every shipped SDK. Review at least IP/general
  location, app interactions, diagnostics, crash/device metadata, installation IDs, advertising or
  app-set IDs and Google's documented purposes. UMP/off-by-default does not remove disclosure duties
  for behaviour present in the binary.
- **Account deletion:** use the implemented in-app Settings flow and the same-origin public route
  `https://houseapi.dohotstudio.com/account-deletion`. Verify both against production. A sole-member
  home closes; a shared home transfers to the longest-standing active admin/member; no eligible
  successor returns an honest blocker.
- **Privacy policy:** disclose VPS/API, Resend, Firebase and AdMob purposes, retention, transfers,
  choices and contacts in clear EN/RO.
- **Financial features:** SharedHouse records/coordinates; it does not hold or transmit household money.
- **Permissions:** reconcile the merged manifest. Internet, notifications and any advertising-ID
  permission merged by the SDK must match declarations and actual behaviour.

Have privacy/legal counsel approve the real notices and lawful bases. Code cannot certify GDPR, UK
GDPR, consumer-law or Play compliance.

## 9. Quality gates before closed testing

1. Public API readiness and real verification email pass.
2. Registration/session/create/join/switch/calendar pass on minimum and current Android.
3. Light/dark, EN/RO, large text, TalkBack, display zoom and reduced motion pass.
4. UMP passes EEA and non-EEA test-geography scenarios on registered test devices.
5. No ad appears before opt-in/UMP, on core flows, or after withdrawal; only test ads are used before
   the live release candidate.
6. Firebase receives no email, token, invitation secret, household/calendar content or money value.
7. Crash-free cold/warm/offline startup and ANR alerting have named responders.
8. Account deletion/export and the external deletion page work end-to-end against production, not
   only the automated local coverage.
9. Backup restore, rollback, support escalation and rollout pause criteria are rehearsed.

## 10. Rollout and remaining gates

Use internal, closed, then staged production tracks. Pause for startup crashes, broken authentication
or deletion, data leakage, incorrect consent, ads on prohibited surfaces, corrupted household state
or store-policy warnings.

Still required before a full public launch:

- production evidence for the implemented export/deletion API, Android UI and public web page;
- password reset and device/session management;
- FCM device binding, backend delivery, opt-out/deletion and safe payloads;
- App Check/Play Integrity with backend verification and emergency runbook;
- Play Billing plus backend purchase verification, RTDN, restore/refund/chargeback tests;
- final privacy notice, terms, Data safety evidence and external security/legal review.
