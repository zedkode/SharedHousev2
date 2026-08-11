# EPIC-01/03-02D — direct production Android authentication repair

Status: completed; direct production APK ready for installation.

## Outcome

- Verified the public API and authentication endpoint through Cloudflare; live PostgreSQL contains
  one verified account and one household, so no account recreation or data migration was needed.
- Identified the unreachable-service report as a client transport/configuration failure: the old
  local package targeted `http://10.0.2.2:3000`, while no sign-in request reached the live API.
- Added an explicit Google-services switch. Direct releases keep Firebase, UMP and AdMob disabled
  and reported as unconfigured instead of using test inventory; the production API remains fixed to
  `https://houseapi.dohotstudio.com` with cleartext disabled.
- Added one-time direct signing initialization and repeatable production build scripts. The RSA-4096
  key and DPAPI-protected credential are stored outside the repository below the Windows user
  profile; passwords never enter source control or build logs.
- Produced `SharedHouse-v0.2.0-public-release-signed.apk`, application ID
  `com.sharedhouse.android`, version code 2, optimized by R8 and signed with APK Signature Schemes
  v2/v3.

## Evidence

- Public health: HTTP 200; shaped sign-in probe reached the API and returned the expected HTTP 401.
- KMP network tests, Android public unit tests and public release lint passed.
- APK inspection found the production HTTPS endpoint and did not find `10.0.2.2`; generated release
  flags confirm Firebase and AdMob are disabled.
- APK SHA-256: `BFFB6F50CB903EA71BC4BB1CA25B6864CFD579A2242EC884C8555A746E835693`.
- Signing certificate SHA-256:
  `A6F07FCE988E59BB1894A817ABFD0F17AFE834ACB8EF9AF48FF7970592748DCA`.

## Installation

- Remove `SharedHouse Local` (`com.sharedhouse.android.local`). This does not alter server data.
- If a previous `com.sharedhouse.android` build used a different certificate, uninstall it first.
- Install this APK, then sign in with the existing verified production credentials.

## Remaining store scope

- Back up the direct signing directory on encrypted offline media.
- Create the separate Google Play upload identity and configure Firebase/AdMob before Play release.
- Complete device installation and real-account authentication evidence on the user's phone.
