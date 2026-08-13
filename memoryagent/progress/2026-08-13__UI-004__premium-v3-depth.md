# UI-004 — Premium v3 surface depth and shape system

**Date:** 2026-08-13  
**Area:** Android UI/UX / design system / physical-device validation  
**Status:** implemented and visually validated as internal public-debug; owner-signed release remains

## Outcome

- Preserved the premium-v2 palette and rebuilt its surface treatment around the v3 depth brief.
- Updated shape hierarchy to 24 dp secondary/list cards, explicit 28 dp metric cards, 36 dp heroes,
  20 dp minimum buttons, pill filters and 32–34 dp modal-sheet top corners.
- Standard cards now combine two shadows, a subtle vertical body gradient, a top-left radial
  highlight, a short top-edge reflection and a low-opacity border instead of one flat fill.
- Hero cards retain the exact violet-purple-pink three-stop gradient and add stronger coloured
  elevation, contact shadow, clipped decorative circles and a translucent highlight layer.
- Added reusable radial `DepthIconBadge` surfaces with independent shadow/highlight and applied them
  to principal Home, Money, Tasks, Calendar and House content and empty states.
- Added visible dual ambient blobs to the global scaffold and Home/House containers.
- Primary buttons, clickable surfaces and icon buttons now compress and reduce elevation on press;
  reduced-motion preference changes those transitions to immediate state updates.

## Main files

- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/Shape.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/atmosphere/AtmosphereComponents.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/home/HouseholdDashboardScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/home/HouseholdHubScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/money/MoneyScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/tasks/TasksScreen.kt`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/calendar/CalendarScreen.kt`
- `docs/01-ux-ui/design-system.md`
- `docs/02-mobile/android.md`

## Schema and API changes

- No endpoint, event, response contract, database migration or VPS change was introduced.
- Money, membership, task, calendar, chat and permission actions retain their existing capability
  and audit rules; this task changes only Android presentation and touch feedback.

## Decisions

- Depth is rendered with Compose-native gradients, drawing layers and coloured shadows. It remains
  a simulated glass-like highlight, not true backdrop blur.
- The global primitive owns the three-layer card invariant so list rows and secondary screens do not
  drift into local one-off treatments.
- Empty states use the same depth badge rather than introducing illustrative assets or unsupported
  actions.
- Visual screenshots remain temporary and outside the repository because authenticated household
  screens contain personal data.

## Validation evidence

- `compilePublicDebugKotlin` and `assemblePublicDebug` passed after integration.
- The internal public-debug APK installed over the existing debug build without clearing data.
- Fresh physical-device captures were inspected for Home, Money, Tasks, Calendar and House. All
  five showed rounded layered cards, ambient decoration and depth badges; Home/Money/House heroes
  showed the three-stop gradient, highlight, coloured shadow and clipped decoration.
- Calendar retained a centered seven-column/six-row month grid, proportionate selected day and a
  fully visible selected-day preview after the shape/depth change.
- Pressed feedback is implemented through real Compose interaction sources and animated scale/shadow
  state, with immediate reduced-motion fallback.

## Security and privacy review

- No secret, token, signing material, database content, user export or screenshot was committed.
- No new permission, network request, analytics/tracking SDK or background process was added.
- Status remains icon plus visible text; colour and depth never replace financial or workflow state.

## Limitations and next task

- Android shadows are renderer/device dependent and are intentionally tuned against the connected
  Samsung; tablet/foldable and light/high-contrast screenshot gates remain.
- True backdrop blur is not implemented or claimed.
- The installed artifact is internal public-debug, not an owner-signed store/distribution build.
- Next, complete owner-observed large-text, TalkBack, reduced-motion, light/high-contrast and
  tablet/foldable checks before an owner-signed release.
