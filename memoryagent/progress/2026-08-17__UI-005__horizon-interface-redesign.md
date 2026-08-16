# UI-005 — SharedHouse Horizon interface redesign

**Date:** 17 August 2026  
**Status:** Implemented in source; Android compilation blocked by missing SDK in the execution environment.

## Outcome

The Android application’s shared visual system was redesigned from the violet/pink `premium v3` direction to **SharedHouse Horizon**: a calm evergreen canvas with teal and sky action surfaces, an amber attention accent, matte card layering and a more compact navigation dock. The redesign is global because the edited theme and Foundation primitives are consumed by the authenticated home, money, calendar, task, house, chat, onboarding and form surfaces.

## Files changed

| Area | Files |
|---|---|
| Theme tokens | `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Color.kt`, `Shape.kt`, `Theme.kt`, `Type.kt` |
| Shared UI primitives | `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/atmosphere/AtmosphereComponents.kt` |
| Design authority | `docs/01-ux-ui/design-system.md` |

## Design decisions

- The dark canvas is now `#0D1714`, with teal (`#2DD4BF`) as the principal interactive colour, sky (`#38BDF8`) as orientation accent and amber for attention.
- The one-per-screen hero keeps a three-stop gradient but uses teal-to-sky rather than violet-to-pink.
- Standard cards are more matte and less glossy, with lowered shadow strength and a smaller highlight. The navigation dock is more opaque and compact for legibility over active content.
- Typography reserves dominant display figures for the one decisive number on a screen. Corners use a 10–32 dp scale with 18 dp buttons and 26 dp cards.
- Existing server states, permissions, localisation, accessibility semantics, financial audit rules and the prohibition on money movement were not changed.

## Validation

| Check | Result |
|---|---|
| `git diff --check` | Passed; no whitespace errors. |
| Android Kotlin compilation | Not run to completion: Gradle reported that `ANDROID_HOME`/`sdk.dir` is not configured in this environment. |
| Prior repository state | The repository’s API/contracts/portal checks had passed before this visual-only Android change; the root lint command remained red before this task due to existing TypeScript lint errors. |

## Next task

On a workstation with Android SDK Platform 36 configured, run:

```sh
bash gradlew :apps:android:app:lintPublicDebug :apps:android:app:testPublicDebugUnitTest :apps:android:app:assemblePublicDebug
```

Then inspect Home, Money, Tasks, Calendar, House, Chat, authentication and settings in both dark/light modes, at large text sizes and with reduced motion. No product behaviour should be changed during that review.
