# DOC-001 — Admin web setup and user-flow documentation

**Date:** 2026-08-02
**Area:** admin-web / backend / documentation
**Status:** completed

## Objective

Document the real local installation and start sequence for the admin web portal, confirm the current backend identity flow, and capture the current limitation that the portal is still a static shell without real user-management screens.

## What changed

- Added [instructions.md](../../instructions.md) with the verified installation and runtime workflow.
- Captured the authoritative API user-creation path through `/v1/auth/register`, `/v1/auth/verify-email`, and `/v1/auth/sign-in`.
- Confirmed the workspace exposes a working admin web build and a passing API smoke test.

## Business and security rules preserved

- The API remains the authoritative implementation path for identity and user creation.
- Development verification codes are only exposed in non-production environments.
- The current product boundary does not yet include a real authenticated admin console.

## Implementation notes

- The admin web app is currently a static React shell with no login or role-based portal features.
- The backend identity flow is implemented and verified through the repository smoke test.
- The documented install path is `npm ci`, `npm run dev:api`, and `npm run dev:admin`.

## Validation

Commands executed and verified:

- `npm run build -w @sharedhouse/admin-web` — success
- `npm run smoke:api` — success
- `npm run check` — success

## Migrations and compatibility

- No schema migrations were required for this documentation task.
- No API contract changes were introduced.

## Remaining work

- Implement actual admin authentication and role-based platform administration UI.
- Implement real user-management screens in the admin web portal.
- Wire the portal to the API for registration, session management, and household administration.

## Documentation updated

- [instructions.md](../../instructions.md)
