# EPIC-13-16A Google Play, Firebase, AdMob and privacy foundation

## Outcome

Implemented the first production-oriented Google services slice for Android without pretending that
provider accounts or policy approval already exist. Firebase Analytics, Firebase Crashlytics, GMA
Next-Gen and UMP are integrated behind explicit local choices that default to off. Guides may show a
labelled adaptive banner only after both the app choice and Google's consent state allow it; account,
household and calendar flows stay ad-free.

## Main changes

- Added pinned Google/Firebase dependencies and conditionally applied Firebase build plugins.
- Added fail-closed public-release checks for HTTPS API, real-format non-demo ad IDs, Firebase
  configuration and a complete release signing configuration.
- Added semantic version inputs and named APK/AAB packaging tasks.
- Disabled Analytics, Crashlytics, advertising-ID collection and default ad-personalisation signals
  in the manifest until runtime choices permit the applicable optional service.
- Added persisted Analytics, crash-reporting and advertising choices to Android DataStore.
- Added a Material 3 privacy/services settings surface with honest configured/test/unavailable,
  consent and privacy-options states in English and Romanian.
- Added a Google service coordinator, UMP consent flow and adaptive banner confined to Guides.
- Added an owner/operator runbook covering Play, Firebase, AdMob, signing, declarations, tests,
  rollout and known blockers.

## Decisions

- No account, email, household/calendar content, invitation secret or financial value is attached to
  Firebase or advertising calls by application code.
- Firebase configuration remains ignored and is supplied only to a protected release workspace.
- Debug uses Google's documented sample IDs. Public release refuses sample IDs or incomplete owner
  configuration.
- FCM, App Check, Remote Config and Play Billing remain unshipped until their backend, deletion,
  retention and test lifecycles are implemented.
- Registration means account export/deletion and a working external deletion page remain public
  release blockers.

## Validation on 8 August 2026

- Public release validation with a disposable Firebase fixture, non-demo-format placeholder ad IDs
  and the local debug certificate: lint, R8, signed APK and signed AAB completed. Those artifacts are
  validation-only and must not be uploaded.
- Public/local Android debug unit tests, public lint and named testing APK packaging completed.
- Workspace formatting, ESLint, TypeScript, API/worker/contract tests, production builds and OpenAPI
  validation completed; npm audit reported zero vulnerabilities.
- Firebase fixture was removed after validation. A release without the owner's real protected inputs
  is designed to fail.

## Remaining production gates

- Owner-created Play, Firebase and AdMob accounts/configuration, real upload key and live-device UMP,
  Analytics and Crashlytics validation.
- Public privacy, terms, support, account-deletion and app-ads.txt resources.
- Account deletion/export, password recovery/session management, FCM lifecycle, App Check backend
  verification and Play Billing lifecycle.
- Closed testing, accessibility/device matrix, Data safety evidence and external security/privacy
  review.
