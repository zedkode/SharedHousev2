# SharedHouse Android application

This directory owns the Jetpack Compose application and future Android-only adapters for secure
storage, notifications, deep links, billing, photo picking, and accessibility.

The application uses application ID `com.sharedhouse.android`, minimum SDK 26 and target SDK 36.
Its current functional vertical supports registration, development email verification, sign-in,
access/refresh rotation, sign-out, household discovery, creation and version-checked editing. It also
supports secure invitation preview/acceptance, owner/admin invitation management and switching among
multiple joined households. The
UI uses Navigation Compose and Material 3 with a phone navigation bar or large-screen rail,
light/dark/dynamic/high-contrast themes, text scaling, a skippable first-run tutorial and matching
English/Romanian resources. The server-backed calendar offers interactive week, month, quarter and
year periods with day sheets and role-aware one-off event create/edit/delete actions. Advanced
settings persist appearance, language, accessibility, notification categories, quiet hours, lead
time, sound and vibration. A Material 3 privacy centre exposes opt-in Analytics, Crashlytics and
AdMob controls. Collection starts disabled; ads require UMP permission and are limited to a labelled
adaptive banner in Guides. Debug variants use Google's test inventory. Six Android notification
channels and a local test notification exist;
remote push scheduling does not. Unimplemented Money and Tasks areas remain explicit unavailable
states and never fabricate household activity.

Access and rotating refresh credentials are encrypted with AES-256-GCM using a non-exportable
Android Keystore key. The atomic ciphertext lives in `noBackupFilesDir`, is bound to the application
and storage format with authenticated additional data, and is removed on local sign-out or terminal
session failure. After process death, the saved refresh token is exchanged and replaced before any
household content is shown; network failures offer an explicit retry without deleting the valid
credential. Public builds use HTTPS and are pinned to `https://houseapi.dohotstudio.com`; owner
signing and a live deployment still require operator-owned secrets and infrastructure.
Device-management UI is not yet implemented.

Portable rules belong in `shared/`; Android UI and platform integrations stay here. Build and test
the application with:

```powershell
.\gradlew.bat :apps:android:app:lintLocalDebug :apps:android:app:testLocalDebugUnitTest :apps:android:app:packageLocalTestingApk
```

The resulting installable artifact is:
`app/build/outputs/apk/testing/SharedHouse-v0.1.0-local-testing-signed.apk`. It uses application ID
`com.sharedhouse.android.local` and label `SharedHouse Local`, so it can remain installed alongside
the public `com.sharedhouse.android` application. Android's debug keystore signs it for local
installation and testing; do not distribute it as a production or Play Store release.

The emulator debug build uses `http://10.0.2.2:3000`. A different local API can be selected with:

```powershell
.\gradlew.bat :apps:android:app:packageLocalTestingApk -PSHAREDHOUSE_LOCAL_API_BASE_URL=http://192.0.2.10:3000
```

## First local account

1. From the repository root run `npm ci`, then `npm run dev:api` and keep the API running.
2. Install the named testing APK on an emulator. Complete or skip the tutorial.
3. Select **Create account**, enter the requested adult profile details and a unique password of at
   least 15 characters, accept the required terms and submit.
4. Copy the development verification code shown by the app into the eight-digit code field.
5. Create a household and select its country, timezone, settlement currency, week start and billing
   cycle. The dashboard, calendar, guides and settings are then available.

To join an existing household, choose **Join with invitation** after verification, paste the private
invitation code, check the household and role preview, then confirm. A signed-in user can also join a
second household from the Household tab and switch the active household there. Codes are currently
shared manually; email delivery and Android App Links remain a future integration.

The default APK reaches the PC API through the Android Emulator address `10.0.2.2`. For a physical
phone, rebuild with the PC's LAN address through `SHAREDHOUSE_LOCAL_API_BASE_URL`, keep the phone and
PC on the same network, and allow the API port through the local firewall. The local response code
is development-only. The public account flow sends the code through the configured Resend
integration and never includes it in an API response.

## Public profile

The public profile uses the production application ID, requires HTTPS, disables cleartext traffic
and targets `https://houseapi.dohotstudio.com`. To make a debug-signed APK that tests the deployed
public environment:

```powershell
.\gradlew.bat :apps:android:app:packagePublicTestingApk
```

For an owner-signed optimized APK or Play Store AAB, provide the signing material only through the
current process environment. Do not put these values in `gradle.properties`, source control or a
command committed to shell history:

```powershell
$env:SHAREDHOUSE_RELEASE_STORE_FILE = "C:\secure\sharedhouse-upload.jks"
$env:SHAREDHOUSE_RELEASE_STORE_PASSWORD = "<from-secret-manager>"
$env:SHAREDHOUSE_RELEASE_KEY_ALIAS = "<upload-key-alias>"
$env:SHAREDHOUSE_RELEASE_KEY_PASSWORD = "<from-secret-manager>"
$env:SHAREDHOUSE_VERSION_CODE = "2"
$env:SHAREDHOUSE_VERSION_NAME = "0.2.0"
$env:SHAREDHOUSE_ADMOB_APP_ID = "<real-AdMob-app-id>"
$env:SHAREDHOUSE_ADMOB_BANNER_ID = "<real-banner-unit-id>"

.\gradlew.bat :apps:android:app:packagePublicReleaseApk
.\gradlew.bat :apps:android:app:copyPublicReleaseBundle
```

Add the production Firebase configuration at `app/src/public/google-services.json`. Release builds
reject missing Firebase configuration, Google demo AdMob IDs, incomplete signing, or a non-production
API. Output names use `SHAREDHOUSE_VERSION_NAME`. Follow
`docs/09-delivery/google-play-firebase-admob-runbook.md` for the full console and release sequence.
