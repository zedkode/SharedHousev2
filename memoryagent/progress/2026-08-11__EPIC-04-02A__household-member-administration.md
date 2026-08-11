# EPIC-04-02A — Household member administration

## Outcome

The Android **Household / Locuință** destination now contains a production-backed **People and
access / Persoane și acces** panel. Owners can manage member roles and access, and transfer
ownership without leaving the app. Administrators receive only their narrower server-confirmed
actions. The API preserves membership history and prevents stale or cross-household mutations.

## Product and UI

- Material 3 member cards show display name, current role, status, join date and the signed-in user.
- Loading, isolated failure, retry and in-progress states do not block the rest of Household.
- Server-provided capability flags and assignable-role lists drive every visible action.
- Role selection explains administrator, member and read-only access in English and Romanian.
- Suspend, remove and ownership transfer use explicit consequence dialogs; reactivation is a
  separate action.
- A version conflict reloads the authoritative board before showing the conflict explanation.
- Removed/suspended history is visible only to owner/admin; ordinary members see active people.

## Schema and API

- Migration `0011_household_member_administration.sql` adds membership versions/timestamps, a
  partial unique active-owner index and append-only `household_membership_history`.
- OpenAPI advanced from 1.9.0 to 1.10.0.
- `GET /v1/households/{householdId}/members` returns a privacy-safe board and caller-specific
  capabilities.
- `POST /v1/households/{householdId}/members/{membershipId}/actions` supports `change_role`,
  `suspend`, `reactivate`, `remove` and `transfer_ownership` with authentication, active
  membership, delegation rules, `If-Match` and idempotency.
- Owner transfer demotes the current owner and promotes one active admin/member in one transaction.
  Account deletion was updated to respect the same single-active-owner invariant.
- Successful mutations write membership history, audit events and outbox events. Cross-household
  and inactive-member access is hidden with `404`.

## Validation

- `npm run check`: passed — API 43/43, workers 9/9, contracts 9/9, formatting, lint, typecheck,
  production builds and OpenAPI 1.10.0 validation.
- New API E2E coverage: privacy-safe board, capability projection, outsider isolation, admin
  delegation denial, owner promotion, stale version, idempotent replay, suspend/reactivate/remove,
  immediate access loss and atomic owner transfer.
- Account-deletion owner-transfer regression coverage passed.
- KMP MockEngine tests passed, including authorization, `If-Match`, idempotency and action JSON.
- Android public unit tests, public release lint, R8 packaging, endpoint/feature-flag checks and APK
  signature checks passed through `build-direct-production-android.ps1`.

## Production evidence

- Pre-deploy backup: `/home/sharedhouse-backups/sharedhouse-20260811T111602Z.dump`.
- Isolated Compose project: `sharedhouse-production`; unrelated-container gate passed.
- Live API/worker image tag: `0.1.0-household-admin-20260811`.
- Migration `0011_household_member_administration.sql` is recorded in PostgreSQL.
- Existing data remained one active user, one active household, one active membership and zero
  invented membership-history records.
- API, workers, PostgreSQL and tunnel were running with zero restarts; post-deploy API/worker log
  error scan was clean.
- Public readiness returned HTTP 200 in 0.207 seconds; unauthenticated member-board access returned
  HTTP 401 in 0.163 seconds.
- Signed APK: `SharedHouse-v0.5.0-public-release-signed.apk`, 4,777,710 bytes, SHA-256
  `26145D5569BC27FA3FBA01F410B7DCC86B4E16B7200713E38F1ACE30380E57D9`, version code 5, signed by
  `CN=SharedHouse Direct Release` with RSA-4096 and APK signature schemes v2/v3. A matching copy is
  archived at `/home/sharedhouse-releases/`.

## Main files

- `services/api/migrations/0011_household_member_administration.sql`
- `services/api/src/households/household-members.*`
- `services/api/src/identity/identity.repository.ts`
- `packages/contracts/src/index.ts`
- `packages/contracts/openapi/sharedhouse-v1.yaml`
- `shared/network/src/commonMain/kotlin/com/sharedhouse/network/*`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/home/*`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/app/*`
- `docs/00-product/roles-and-permissions.md`
- `docs/08-admin-docs/household-admin-guide.md`

## Limitations and next task

- No USB device/emulator was available, so physical-device visual, touch-target, TalkBack and
  rotation validation is still required; it is not claimed as completed.
- Household closure, voluntary member leave, per-capability custom roles, device/session management
  and push notifications for access changes remain.
- Finance Manager and Chore Manager are still documented future capability bundles and are not
  falsely offered as assignable database roles.

