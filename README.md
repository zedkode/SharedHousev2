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
- a versioned `GET /v1/health` API boundary with an OpenAPI 3.1 contract;
- a Kotlin Multiplatform domain module targeting Android, iOS and JVM tests;
- synthetic-only local PostgreSQL, Redis and S3-compatible infrastructure definitions;
- strict TypeScript checks, unit/API tests, dependency auditing and GitHub Actions CI.

This is an engineering foundation, not a functional MVP. Identity, households, ledger, chores,
shopping, notifications, store commerce and privacy workflows remain implementation work.

## Local development

Requirements: Node.js 22 or newer, npm 10 or newer and JDK 17. Android builds additionally require
an Android SDK. iOS compilation requires macOS and Xcode.

```powershell
npm ci
npm run check
.\gradlew.bat :shared:domain:jvmTest
```

Run individual development processes with:

```powershell
npm run dev:api
npm run dev:workers
npm run dev:admin
```

The API listens on `http://localhost:3000` by default and exposes
`GET http://localhost:3000/v1/health`.

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
