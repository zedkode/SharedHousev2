# SharedHouse project instructions

This document explains how this repository is structured, how to install and run the admin web portal, and how to create users in the current development state.

## 1. Repository overview

This repository is a monorepo for:

- `apps/admin-web/` — React admin portal shell
- `services/api/` — NestJS API with identity, households, and account endpoints
- `services/workers/` — background jobs and async processors
- `packages/contracts/` — shared TypeScript/OpenAPI contracts
- `shared/` — cross-platform domain/network code

## 2. Required prerequisites

Use the following versions:

- Node.js 22+
- npm 10+
- JDK 17+

Optional for full mobile builds:

- Android SDK for Android builds
- macOS + Xcode for iOS builds

## 3. Clean install

From the repository root:

```powershell
npm ci
```

This installs all workspaces defined in the root package file.

## 4. How to start the backend API

The authoritative API is in the workspace package `@sharedhouse/api`.

Run:

```powershell
npm run dev:api
```

Expected result:

- API starts on `http://localhost:3000`
- If `DATABASE_URL` is not set, the API falls back to a persistent PGlite database under `tmp/sharedhouse-pglite`
- In development, the verification code is exposed for testing purposes

## 5. How to start the admin web portal

The admin portal package is `@sharedhouse/admin-web`.

Run:

```powershell
npm run dev:admin
```

Expected result:

- Vite dev server starts on `http://localhost:4173/`
- The current version is a static shell and not a full authenticated admin console yet

Important note:

- Do not pass `--host` after `npm run dev:admin` from the root script. The correct root command is the one above.
- If you want to run the Vite command directly, use:

```powershell
npm run dev -w @sharedhouse/admin-web
```

## 6. How to build the project

Use the root validation command:

```powershell
npm run check
```

This runs formatting, lint, typecheck, tests, build, and OpenAPI validation.

## 7. How to verify the API with a smoke test

A real runtime proof already exists in the repository.

Run:

```powershell
npm run smoke:api
```

This verifies that:

1. a user can register
2. the email verification code is returned in development
3. the session is created
4. a household can be created
5. the data survives a restart using the local persistence layer

## 8. How users are created in the current project state

The user-creation path is not implemented as a real admin-web form. The current working flow is API-driven.

### 8.1 Register a new user

Endpoint:

```http
POST /v1/auth/register
```

Example body:

```json
{
  "email": "new.user@example.test",
  "password": "A synthetic runtime smoke passphrase 2026",
  "displayName": "Runtime Smoke User",
  "preferredLocale": "ro",
  "ageConfirmed": true,
  "termsAccepted": true,
  "marketingConsent": false
}
```

Expected result:

- HTTP 202 Accepted
- `verificationRequired: true`
- In development, a `developmentVerificationCode` is returned

### 8.2 Verify the email address

Endpoint:

```http
POST /v1/auth/verify-email
```

Example body:

```json
{
  "email": "new.user@example.test",
  "code": "12345678",
  "deviceName": "Local dev workstation"
}
```

Expected result:

- HTTP 200
- access token and refresh token returned in the body

### 8.3 Sign in

Endpoint:

```http
POST /v1/auth/sign-in
```

Example body:

```json
{
  "email": "new.user@example.test",
  "password": "A synthetic runtime smoke passphrase 2026",
  "deviceName": "Local dev workstation"
}
```

Expected result:

- HTTP 200
- session details returned

## 9. Admin web current state

The admin portal currently provides only a shell page. It does not yet implement:

- authenticated admin login
- role-based access control
- user provisioning UI
- household administration UI
- invitation management UI

So, the admin web portal is only a scaffold for future platform administration.

## 10. Operational recommendations

For a clean local developer setup, use this sequence:

```powershell
npm ci
npm run dev:api
npm run dev:admin
```

Then create a user through the API registration flow and verify it through `/v1/auth/verify-email`.

## 11. Verified facts from this workspace

The following checks were executed successfully in the current repository:

- `npm run build -w @sharedhouse/admin-web` — success
- `npm run smoke:api` — success
- `npm run check` — success

The current critical gap is not a broken build pipeline; it is the lack of implemented authenticated admin-web features and a real user-management UI.
