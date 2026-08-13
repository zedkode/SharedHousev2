# UI-003 — SharedHouse launcher identity and animated startup

**Date:** 2026-08-13  
**Area:** Android branding / startup UI / accessibility  
**Status:** implementation and internal public-debug device validation completed; owner-signed release remains

## Outcome

- Reworked the supplied house-and-people visual into a SharedHouse-owned transparent launcher mark
  with a violet-purple-pink premium treatment and clear adaptive-icon safe-zone proportions.
- Replaced every Android launcher density asset and aligned the Android 12+ system splash with the
  dark brand background and new mark.
- Added a Compose startup experience with a centered mark, restrained glow/pulse, an accessible
  three-dot progress indicator and localized English/Romanian copy.
- A successfully restored account receives a personalized welcome using the confirmed account
  display name. A guest receives neutral SharedHouse copy, and the restoration state never exposes
  a stale local user name.
- Reduced-motion preference disables the pulse and orbit while retaining a stable visual progress
  state and TalkBack semantics.

## Main files

- `apps/android/app/src/main/kotlin/com/sharedhouse/android/MainActivity.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/app/SharedHouseApp.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/startup/SharedHouseStartupScreen.kt`
- `apps/android/app/src/main/res/drawable-nodpi/sharedhouse_logo_master.png`
- `apps/android/app/src/main/res/drawable-*/ic_launcher_foreground_art.png`
- `apps/android/app/src/main/res/values*/themes.xml`
- `apps/android/app/src/main/res/values*/strings.xml`
- `apps/android/app/src/test/kotlin/com/sharedhouse/android/ui/startup/SharedHouseStartupScreenTest.kt`
- `docs/02-mobile/android.md`

## Schema and API changes

- No database migration, HTTP endpoint, event or response-contract change was introduced.
- Personalization uses only `AppUiState.account.displayName` after secure session restoration has
  completed successfully.

## Decisions

- The launcher uses density-specific raster outputs derived from one transparent source because the
  new appearance depends on soft 3D highlights that would not be preserved by a flat vector trace.
- The guest path deliberately avoids guessed identity. The authenticated name is shown only after
  the existing server-backed session restoration succeeds.
- The branded Compose transition is intentionally brief after session resolution and does not
  delay an unresolved network/session operation.

## Validation evidence

- Startup state selection has JVM unit coverage for restoring, authenticated and guest states.
- `compilePublicDebugKotlin`, `testPublicDebugUnitTest`, `lintPublicDebug` and
  `assemblePublicDebug` passed for the internal public-debug variant.
- The internal public-debug APK was installed without clearing application data and cold-started on
  the connected physical Android device; the system splash and authenticated personalized startup
  were visually checked. Screenshots remain outside the repository because the authenticated view
  contains a personal display name.

## Security and privacy review

- No token, household data, secret, signing material or personal screenshot is included in the
  source tree.
- The startup page does not persist or log the display name and does not render it until the secure
  session result is available.
- No new permission, tracking SDK, network request or background operation was added.

## Limitations and next task

- This is an internal public-debug build, not an owner-signed production/store artifact.
- The guest state is logic-tested without signing out or clearing the owner's physical-device data.
- Before distribution, build with the established owner signing identity and complete the remaining
  large-text, TalkBack, high-contrast and reduced-motion release gates.
