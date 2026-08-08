# EPIC-01-03A — Production VPS, verification email and Android stability

**Date:** 2026-08-08  
**Status:** completed implementation/configuration slice; live rollout pending operator infrastructure

## Outcome

- Added a single-VPS production profile with PostgreSQL, a read-only non-root API container and an
  outbound Cloudflare Tunnel for `houseapi.dohotstudio.com`; neither PostgreSQL nor the API publishes
  a VPS host port.
- Added production verification email through the Resend HTTPS API. Registration and resend write an
  encrypted message to a transactional PostgreSQL outbox, use provider idempotency, bounded retry and
  dead-letter states, and never expose production codes in API responses.
- Added the versioned resend-verification API and Android action, including cooldown, generic
  account-enumeration-safe responses and English/Romanian UI feedback.
- Fixed the Android night startup theme to use an AppCompat parent, removing a concrete dark-mode
  startup crash condition for `AppCompatActivity`.
- Changed the launcher background resource to transparent. The 3D foreground PNG has alpha zero at
  its corners and the adaptive/legacy launcher definitions use the transparent background.
- Pinned public Android release builds to `https://houseapi.dohotstudio.com`, disabled cleartext and
  retained fail-closed owner signing. CI now validates the public flavor and named testing artifact
  rather than an obsolete `app-debug.apk` path.
- Added operator instructions for VPS hardening, Cloudflare, Resend DNS, secrets, deployment health,
  first account, backup/restore rehearsal and owner-signed Android builds.

## Main files

- `.github/workflows/ci.yml`, `.dockerignore`, `.gitignore`
- `infra/production/Dockerfile.api`, `infra/production/compose.yaml`,
  `infra/production/production.env.example`, `infra/production/scripts/*`,
  `infra/production/README.md`
- `services/api/migrations/0003_verification_email_outbox.sql`
- `services/api/src/config/*`, `services/api/src/email/*`, `services/api/src/identity/*`,
  `services/api/src/operations/health.controller.ts`
- `packages/contracts/openapi/sharedhouse-v1.yaml`, `packages/contracts/src/index.ts`
- `shared/network/src/commonMain/kotlin/com/sharedhouse/network/*`
- Android app state/gateway/view-model/auth resources plus `values-night/themes.xml`,
  `values/colors.xml` and `apps/android/app/build.gradle.kts`
- Root, Android and infrastructure README files and `docs/09-delivery/task-master.md`

## Schema and API

- Migration `0003_verification_email_outbox.sql` adds a tenant-independent verification outbox with
  unique challenge binding, availability/lock timestamps, eight-attempt limit, provider message ID
  and safe error code. The AES-GCM code payload is present only while pending/sending and is erased
  on sent/dead terminal states.
- `POST /v1/auth/resend-verification` accepts an email and always returns a generic `202` response.
  A pending account receives a replacement single-use eight-digit challenge after a 60-second
  cooldown; the previous challenge is consumed transactionally.
- `GET /v1/health/ready` verifies database access while the existing liveness route remains cheap.

## Decisions

- Use Resend rather than operating SMTP on the VPS. Sender DNS is isolated on
  `mail.dohotstudio.com`; exact SPF/DKIM values come from the operator's Resend account.
- Use a remotely managed Cloudflare Tunnel so the origin needs no public API/database port.
- Store database/provider/tunnel/encryption material only in Docker secret files outside source
  control. The database URL contains no password.
- Do not publish or rename the local release-validation APK as production: its certificate is the
  Android Debug certificate. A public artifact is blocked until the owner supplies and backs up a
  stable upload key.

## Verification

- `npm run check`: passed; API 23/23, workers 1/1, contracts 5/5, TypeScript builds, lint, formatting
  and OpenAPI 1.2.0 validation all passed.
- `npm audit --omit=dev --audit-level=high`: zero reported vulnerabilities.
- `:shared:network:jvmTest`, `testPublicDebugUnitTest`, `lintPublicDebug`,
  `assemblePublicDebug`: passed; Android unit tests 42/42.
- `lintPublicRelease` and R8 `assemblePublicRelease`: passed with a temporary debug certificate used
  only to validate the release toolchain. `apksigner` confirmed v2/v3 signatures and explicitly
  identified the signer as Android Debug.
- Packaged public debug/release BuildConfig values both contain
  `https://houseapi.dohotstudio.com`; release manifest has `usesCleartextTraffic=false`, target SDK
  36 and application ID `com.sharedhouse.android`.
- Compose YAML parsed successfully; both POSIX scripts passed `bash -n`; `git diff --check` passed.

## Security and privacy review

- Production startup fails without PostgreSQL, Resend sender/key and a canonical 32-byte base64
  outbox key, or when development verification exposure is enabled.
- AES-256-GCM uses random 96-bit IVs and binds challenge, recipient and expiry as authenticated
  context. Codes and addresses are excluded from application logs; ciphertext is erased after
  terminal delivery state.
- Resend calls have a ten-second timeout, safe response handling, retry only for timeout/rate/server
  failures and a stable provider idempotency key.
- Registration, verification and resend are rate limited. Resend responses are generic for unknown
  accounts. HTTP problem logging includes only safe metadata and a correlation ID.
- Containers drop Linux capabilities, forbid privilege escalation and bound logs. The API filesystem
  is read-only and PostgreSQL is isolated on an internal network.

## Limitations

- No Docker runtime is installed on this workstation, so the production image/Compose stack was not
  executed here. PostgreSQL-container startup, migrations, tunnel and Resend delivery require VPS
  validation.
- DNS currently returns NXDOMAIN for `houseapi.dohotstudio.com`; the public readiness endpoint is not
  live until the operator creates the Cloudflare Tunnel route.
- No Android device/emulator or system image is available, so the reported crash could not be
  reproduced with Logcat and the fix still requires physical-device dark/light cold-start testing.
- No owner upload keystore exists in the workspace. The generated release-validation APK is not a
  distributable production artifact.
- Privacy policy, public deletion/export, support operations, distributed throttling, monitoring and
  external security/legal review remain launch gates; this record does not claim a completed public
  production launch.

## Next task

Provision the operator-owned VPS, Cloudflare Tunnel and Resend domain/key, execute the documented
deploy/backup/health/real-email gates, capture device Logcat if any crash remains, and build the final
named APK/AAB using the owner's backed-up upload keystore.
