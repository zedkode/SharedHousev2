# SharedHouse Documentation Pack

**Status:** Product and engineering specification v1.0  
**Working name:** SharedHouse (internal codename, not a cleared commercial brand)  
**Prepared:** 25 July 2026  
**Primary markets:** United Kingdom, European Union/EEA, United States  
**Platforms:** Android, iOS, platform administration web portal, backend services

SharedHouse is a privacy-first household coordination application for shared homes. It combines rent and bill tracking, household shopping, chores, bin schedules, interactive calendar events, member invitations, task swaps, reminders, and household totals. The first reference household is a UK home with five tenants, but the domain model supports configurable household size and 14-day, monthly, weekly, and custom cycles.

This repository contains the product definition, mobile architecture, backend specification, data model, security controls, store-release plan, compliance baseline, administrator documentation, member documentation, testing strategy, phased task plan, and agent operating rules.

## Implementation status

Development started with `EPIC-01` on 25 July 2026. The first foundation slice includes:

- npm workspaces for the NestJS API, workers, React administration portal and shared contracts;
- a versioned OpenAPI 3.1 boundary for health, registration, email verification, rotating sessions,
  account access and tenant-scoped household configuration;
- Kotlin Multiplatform domain and Ktor network modules targeting Android, iOS and JVM tests;
- an installable Android Compose application with a Material 3 light/dark/dynamic theme,
  English/Romanian resources, a skippable first-run tutorial, responsive navigation, persistent
  accessibility/notification settings and real authentication and household setup/editing flows;
- Android session recovery backed by a non-exportable AES-256-GCM Android Keystore key, atomic
  no-backup storage and server-side refresh rotation before household data is shown;
- an append-only Money ledger with recurring household costs and payment
  declaration/confirmation/dispute/correction flows that never claim SharedHouse moved money;
- a tenant-scoped one-off calendar vertical with an interactive Android week/month/quarter/year UI,
  idempotent creation and optimistic edit/delete protection;
- a tenant-scoped household task board with role-aware creation/assignment, start and completion,
  append-only history, issue reporting, and committed help/swap/postpone request decisions;
- secure, expiring household invitation codes with email restriction, role-aware creation and
  revocation, one-time acceptance, and an Android household switcher for multi-home accounts;
- consent-gated Firebase Analytics/Crashlytics and GMA Next-Gen/UMP foundations, with optional ads
  kept outside authentication and core household actions and disabled by default;
- PostgreSQL-compatible migrations plus persistent embedded PGlite for Docker-free development;
- synthetic-only local PostgreSQL, Redis and S3-compatible infrastructure definitions;
- strict TypeScript checks, unit/API tests, dependency auditing and GitHub Actions CI.

The identity, household-configuration and one-off calendar verticals are functional. A hardened
single-VPS deployment profile now provides PostgreSQL, an outbound Cloudflare Tunnel and
transactional verification email through Resend, with the Android public profile pinned to
`https://houseapi.dohotstudio.com`. A guarded interactive Linux wizard prepares secrets, validates
Docker Compose and can deploy, back up and verify the public endpoint. This is not yet a complete
production MVP: independent backup restoration exercises, session-device management, recent-authentication,
distributed rate limiting, emailed invitation links/App Links, recurring/generated calendar and
chore occurrences, advanced fairness/exemptions, shopping, remote notification delivery, store
billing and final privacy/store evidence remain. The implemented identity, ledger, task and privacy
records use the production VPS/API path and account exports include created or assigned tasks.

## Engineering validation

Requirements: Node.js 22 or newer, npm 10 or newer and JDK 17. Android builds additionally require
an Android SDK. iOS compilation requires macOS and Xcode.

```powershell
npm ci
npm run check
npm run smoke:api
.\gradlew.bat :shared:domain:jvmTest
.\gradlew.bat :shared:network:jvmTest
.\gradlew.bat :apps:android:app:lintPublicDebug :apps:android:app:testPublicDebugUnitTest
```

Android commands require `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to point to an SDK containing
platform 36. Debug variants remain internal compiler/test inputs and are never packaged or supplied
to users. Distribution accepts only the optimized `publicRelease` APK/AAB signed by the owner key.

Run individual development processes with:

```powershell
npm run dev:api
npm run dev:workers
npm run dev:admin
```

The API listens on `http://localhost:3000` by default. With no `DATABASE_URL`, development uses a
persistent PGlite database under `tmp/sharedhouse-pglite`; registration responses include a local
verification code only outside production. The Android debug build connects to
`http://10.0.2.2:3000` from the emulator. Override it with the
`SHAREDHOUSE_LOCAL_API_BASE_URL` Gradle property when testing against another local host.

The public profile keeps the production application ID `com.sharedhouse.android`, prohibits
cleartext traffic and is pinned to `https://houseapi.dohotstudio.com`. Direct distribution can use
the repository's secret-safe direct-signing scripts with Firebase and AdMob explicitly disabled.
Google-enabled/Play releases additionally require the provider files, IDs and Play upload key.
Every release fails closed when its endpoint or required signing material is absent. Follow the VPS,
Cloudflare, Resend, backup and release procedure in `infra/production/README.md`.

On a prepared Ubuntu/Debian VPS, the production setup can be driven interactively without placing
secrets in shell history:

```sh
chmod +x infra/production/scripts/install-interactive.sh
./infra/production/scripts/install-interactive.sh
```

To create the first production account, install only an owner-signed public release. Complete or
skip the tutorial, choose **Create account**, enter an adult display name, an inbox you control and a
unique password of at least 15 characters, accept the required terms, then enter the code delivered
by Resend. Create the household by choosing its name, country, timezone, currency, week start and
billing cycle.

To join an existing home instead, choose **Join with invitation**, paste the private code received
from its owner/admin, review the safe household preview and confirm. Owners and admins manage codes
from the Household tab; optional email restriction prevents another signed-in address from using a
forwarded code. Invitation email delivery and clickable App Links are not implemented yet, so codes
must currently be shared privately and pasted into the app.

## Start here

1. Read `init.md`.
2. Read `AGENTS.md` in full.
3. Read `docs/00-product/product-vision.md` and `docs/00-product/functional-specification.md`.
4. Read the architecture document for the area being changed.
5. Read the relevant compliance and security documents.
6. Check `memoryagent/INDEX.md` and the most recent entries related to the task.
7. Implement only the assigned scope.
8. Run the required tests and checks.
9. Add a factual completion entry to `memoryagent/` and update its index.

## Important product boundaries

- SharedHouse is not a bank, electronic-money institution, escrow provider, debt collector, landlord management system, legal advisor, utility supplier, or government-alert service.
- The MVP records obligations, payments, reimbursements, and evidence. It does not hold or transmit tenant money.
- App subscriptions are digital products and use Apple/Google store billing as the safe default inside mobile apps.
- Rent, utility reimbursements, and physical-household purchases are separate real-world obligations. Future payment initiation must use a regulated payment provider and separate legal review.
- High-priority household alerts must never imitate UK Emergency Alerts, RO-ALERT, Wireless Emergency Alerts, or another government warning system.
- The initial service is intended for adults. Accounts for users under 18 are not supported in the MVP.

## Repository map

- `apps/android/` — Android application and platform adapters
- `apps/ios/` — iOS application and platform adapters
- `apps/admin-web/` — React platform administration portal
- `shared/` — portable Kotlin Multiplatform domain and application code
- `services/api/` — authoritative NestJS HTTP API
- `services/workers/` — background and scheduled processes
- `packages/contracts/` — versioned TypeScript and OpenAPI contracts
- `infra/` — synthetic local infrastructure and future reproducible environments
- `docs/00-product/` — vision, roles, functional scope, release boundaries
- `docs/01-ux-ui/` — navigation, screens, design system, accessibility, onboarding
- `docs/02-mobile/` — Kotlin Multiplatform, Android, iOS, offline sync, notifications, permissions
- `docs/03-backend/` — services, APIs, data, jobs, security and tenancy
- `docs/04-admin-portal/` — platform administration and commercial operations
- `docs/05-compliance/` — UK/EU/US privacy baseline, store rules, retention, threat model
- `docs/06-quality/` — tests, CI/CD, observability, acceptance criteria
- `docs/07-user-docs/` — member-facing product guide
- `docs/08-admin-docs/` — household and platform administrator guides
- `docs/09-delivery/` — roadmap, task master and definition of done
- `memoryagent/` — controlled implementation memory and handoffs

## Documentation authority

The approved product specification and current code are the sources of truth. When code and documentation diverge, stop and resolve the discrepancy. Do not silently modify business rules, permissions, billing calculations, retention periods, subscription entitlements, or security requirements.
