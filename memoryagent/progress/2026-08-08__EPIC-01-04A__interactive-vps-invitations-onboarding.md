# EPIC-01-04A — Interactive VPS installation and household invitations

**Date:** 2026-08-08  
**Area:** backend / Android / infrastructure / documentation  
**Status:** completed implementation slice; live VPS and device validation pending

## Objective

Make the production VPS setup safer and easier for a non-specialist operator, then remove a major
first-use blocker by allowing verified users to join and switch households through secure invites.

## What changed

- Added a POSIX interactive production installer with platform, capacity, Docker, configuration,
  secret-permission, DNS and public readiness checks; it can deploy and create the first backup.
- Added migration `0004_household_invitations.sql`, invitation contracts/OpenAPI 1.3.0 and Nest API
  endpoints for list, create, safe preview, accept and revoke.
- Added Kotlin network DTOs/client calls and Android Material 3 join/manage flows in English and
  Romanian, including an active-household switcher.
- Made tutorial replay in Settings functional instead of leaving a no-op action.
- Extended the end-to-end smoke flow to prove an invite persists across an API restart.

## Business and security rules preserved

- Only a token hash is persisted; the high-entropy secret is returned once at creation and is not
  exposed by invitation listings or the public preview.
- Codes expire after seven days, are single-use, may be restricted to an exact normalized account
  email, and can be revoked. Acceptance replay by the same user is idempotent.
- Owners may invite admin/member/read-only roles. Admins may invite only member/read-only roles.
  Members and read-only participants cannot manage invitations.
- Cross-household management requests are hidden with `404`; an existing stronger active membership
  is never downgraded by accepting another invite.
- The installer does not install Docker from an unreviewed remote script, print secrets, embed them
  in `production.env`, or publish PostgreSQL/API host ports.

## Implementation notes

- Manual copy/paste is deliberately honest: no email-sent or deep-link state is shown while those
  integrations do not exist.
- The installer preserves existing secrets unless replacement is explicitly confirmed and writes
  new secret/configuration files atomically with restrictive permissions.
- Invitation mutations use audit/outbox records, row locking and the existing tenant membership
  authority model.

## Validation

- `npm run check`: passed; API 27/27, workers 1/1, contracts 5/5, TypeScript builds, lint,
  formatting, admin production bundle and OpenAPI 1.3.0 validation passed.
- `npm run smoke:api`: passed, including account verification, restart persistence, calendar mutation
  and invitation persistence/acceptance after restart.
- `npm audit --omit=dev --audit-level=high`: zero reported vulnerabilities.
- `:shared:network:jvmTest`, `testPublicDebugUnitTest`, `lintPublicDebug` and
  `packagePublicTestingApk`: passed; combined KMP/Android test XML reports contain 52 tests with zero
  failures/errors.
- `lintPublicRelease` and R8 `assemblePublicRelease`: passed with the Android debug keystore used
  only to validate the release toolchain; this is not an owner-signed production release.
- Production Compose YAML parsed, all three POSIX scripts passed `bash -n`, installer help ran and
  `git diff --check` passed.

## Migrations and compatibility

- Apply migrations through `0004_household_invitations.sql` before deploying API code that exposes
  invitation endpoints. The migration is additive and existing accounts/households remain valid.
- Older Android clients continue to use identity, household and calendar endpoints unchanged.

## Remaining work

- Run the wizard on the operator-owned VPS with real Cloudflare/Resend credentials and rehearse a
  restore into a disposable PostgreSQL instance.
- DNS for `houseapi.dohotstudio.com` still returned NXDOMAIN during this validation and Docker is not
  installed on the Windows workstation, so no live production deployment was claimed.
- Add provider-delivered invitation email, verified Android App Links/iOS Universal Links and
  notification delivery.
- Implement password reset, active-device/session management, recent-authentication, ownership
  transfer and member-role management.
- Complete privacy export/deletion, monitoring/alerting and external security/legal release gates.

## Documentation updated

- `README.md`, `apps/android/README.md`, `infra/production/README.md`
- `docs/07-user-docs/member-guide.md`, `docs/08-admin-docs/household-admin-guide.md`
- `docs/09-delivery/task-master.md`, `memoryagent/INDEX.md`
