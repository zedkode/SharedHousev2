# SharedHouse Android application

This directory owns the Jetpack Compose application and future Android-only adapters for secure
storage, notifications, deep links, billing, photo picking, and accessibility.

The application uses application ID `com.sharedhouse.android`, minimum SDK 26 and target SDK 36.
Its current functional vertical supports registration, development email verification, sign-in,
access/refresh rotation, sign-out, household discovery, creation and version-checked editing. It also
supports secure invitation preview/acceptance, owner/admin invitation management and switching among
multiple joined households. The UI uses Navigation Compose and SharedHouse's custom atmospheric
dark-glass Foundation components with a phone dock or large-screen rail, high contrast, reduced
motion, text scaling, a skippable first-run tutorial and matching English/Romanian resources. The
server-backed calendar offers interactive week, month, quarter and
year periods with day sheets and role-aware one-off event create/edit/delete actions. Advanced
settings persist appearance, language, accessibility, notification categories, quiet hours, lead
time, sound and vibration. The atmospheric privacy centre exposes opt-in Analytics, Crashlytics and
AdMob controls. Collection starts disabled; ads require UMP permission and are limited to a labelled
adaptive banner in Guides. Debug variants use Google's test inventory. Seven Android notification
channels, scheduled reminders, task quick actions and privacy-minimised foreground chat alerts exist;
remote push scheduling does not. Money now provides a server-backed append-only expense ledger with
exact equal splitting, personal/household summaries, member proposals, owner/admin approval and
reasoned reversal. Owners and administrators can also manage reusable rent, bill and custom-cost
templates with finite or open-ended weekly/fortnightly/monthly/quarterly/yearly schedules. The production worker now creates each due
ledger occurrence exactly once, and Android labels it as generated without implying a payment.
Members can declare their own approved share paid with method, time, reference and note; another
active writer confirms or disputes the declaration, and corrections preserve the full audit trail.
SharedHouse never claims to move money. Custom split methods are not yet exposed. Tasks is now a
real server-backed atmospheric board: owners/admins assign responsibilities with zone, priority,
deadline and estimate; the responsible member can start, complete, report an issue or request help,
swap or postponement; manager decisions are committed before the assignment/deadline changes. Home
shows only authoritative task counts and the next assignment. Fixed-assignee task schedules support
weekly, fortnightly and monthly generation with an optional final date. Named reusable templates,
photo evidence and fairness scoring are not yet exposed and are never simulated. Household chat is
append-only, tenant-scoped and interactive through authenticated SSE while Android is active;
provider-backed background push remains absent.

Account privacy actions are real rather than placeholders. **Settings > Trust and transparency** can
create a password-confirmed JSON export through Android's system document picker or permanently
delete the account. Deletion revokes sessions, removes password credentials, anonymises the profile,
closes a sole-member household and transfers a shared household to the longest-standing eligible
admin/member. The API also serves the public deletion route at `/account-deletion`.

Access and rotating refresh credentials are encrypted with AES-256-GCM using a non-exportable
Android Keystore key. The atomic ciphertext lives in `noBackupFilesDir`, is bound to the application
and storage format with authenticated additional data, and is removed on local sign-out or terminal
session failure. After process death, the saved refresh token is exchanged and replaced before any
household content is shown; network failures offer an explicit retry without deleting the valid
credential. Public builds use HTTPS and are pinned to `https://houseapi.dohotstudio.com`; owner
signing still requires operator-owned release and provider credentials.
Device-management UI is not yet implemented.

Portable rules belong in `shared/`; Android UI and platform integrations stay here. Run internal
compiler, lint and unit-test gates without producing a distributable debug APK:

```powershell
.\gradlew.bat :shared:domain:jvmTest :shared:network:jvmTest `
  :apps:android:app:lintPublicDebug :apps:android:app:testPublicDebugUnitTest
```

Debug variants are internal verification inputs only. The project no longer defines tasks that copy
or rename debug APKs for installation/distribution. Do not upload a debug binary to users, the VPS
or Google Play.

## First production account

1. Install the owner-signed optimized public release.
2. Complete or skip the tutorial.
3. Select **Create account**, enter the requested adult profile details, an inbox you control and a
   unique password of at least 15 characters, accept the required terms and submit.
4. Enter the eight-digit code delivered by the production Resend integration.
5. Create a household and select its country, timezone, settlement currency, week start and billing
   cycle. The dashboard, calendar, Money, guides and settings are then available.

To join an existing household, choose **Join with invitation** after verification, paste the private
invitation code, check the household and role preview, then confirm. A signed-in user can also join a
second household from the Household tab and switch the active household there. Codes are currently
shared manually; email delivery and Android App Links remain a future integration.

## Production release

The production profile uses the production application ID, requires HTTPS, disables cleartext
traffic and targets `https://houseapi.dohotstudio.com`.

For direct installation before Google services are configured, initialize the dedicated signing
identity once and build the optimized production APK:

```powershell
.\scripts\initialize-direct-android-signing.ps1
.\scripts\build-direct-production-android.ps1 -VersionCode 2 -VersionName 0.2.0
```

The signing material is stored below `%USERPROFILE%\.sharedhouse\release`, outside the repository;
back it up securely because every future direct update must use the same certificate. This channel
sets `SHAREDHOUSE_ENABLE_GOOGLE_SERVICES=false`: Firebase, UMP and AdMob remain visibly unconfigured
and cannot initialize. It is connected only to the production HTTPS API and does not expose a local
verification code.

For the later Google-enabled/Play release, provide all provider and signing values through the
current process environment. Do not put them in `gradle.properties`, source control or shell
history:

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

Set `SHAREDHOUSE_ENABLE_GOOGLE_SERVICES=true` and add the production Firebase configuration at
`app/src/public/google-services.json`. Google-enabled release builds reject missing Firebase
configuration, Google demo AdMob IDs, incomplete signing or a non-production API. Output names use
`SHAREDHOUSE_VERSION_NAME`. Follow
`docs/09-delivery/google-play-firebase-admob-runbook.md` for the full console and release sequence.
