# EPIC-08-01A — Household task workflow

## Outcome

Implemented and deployed the first authoritative household-task vertical. The Android Tasks
destination is no longer a placeholder: it reads and mutates production task state through the KMP
client and the tenant-scoped API. Home now presents the next personal assignment and live active /
pending-request counts.

## Product and UI

- Material 3 task board with My tasks, Active, Requests, Completed and All filters.
- Summary cards for personal active work, overdue work and pending requests.
- Owner/admin creation with title, instructions, zone, priority, local due date/time, estimate and
  active writable assignee.
- Role-aware start, complete, cancel and reopen actions.
- Completion note and request history remain visible.
- Help, swap, postpone and issue requests are explicit pending records. Only an approved swap or
  postponement changes the assignee/deadline.
- English and Romanian UI/documentation parity and accessible text-based status in addition to
  colour/icon cues.

## Schema and API

- Migration `0010_household_tasks.sql` adds `household_tasks`, `household_task_requests` and
  append-only `household_task_history` with tenant, assignee, due/status and pending-request indexes.
- OpenAPI 3.1 contract advanced from 1.8.0 to 1.9.0.
- `GET/POST /v1/households/{householdId}/tasks` and
  `POST /v1/households/{householdId}/tasks/{taskId}/actions`.
- Mutations require authentication, active membership, capability checks, idempotency keys and
  optimistic `If-Match` versions. Cross-household resources are hidden.
- Task transitions emit audit and outbox evidence. Account JSON export includes tasks created by or
  assigned to the user and their request history.

## Main files

- `services/api/migrations/0010_household_tasks.sql`
- `services/api/src/tasks/*`
- `services/api/src/http/request-validation.ts`
- `services/api/src/identity/identity.repository.ts`
- `packages/contracts/src/index.ts`
- `packages/contracts/openapi/sharedhouse-v1.yaml`
- `shared/network/src/commonMain/kotlin/com/sharedhouse/network/*`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/tasks/*`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/app/*`
- `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/home/*`

## Validation

- `npm run check`: passed; API 40/40, workers 9/9, contracts 9/9, plus formatting, lint,
  typecheck, production builds and OpenAPI validation.
- `:shared:network:jvmTest`: passed, including task-board request/header coverage.
- `:apps:android:app:testPublicDebugUnitTest`: passed.
- Direct production build gate passed: Kotlin tests, Android tests, release lint, R8 packaging,
  production HTTPS flags and APK signature validation.
- API E2E covers task create/assign/start/complete/reopen, stale version, member create denial,
  tenant isolation, help approval, swap approval, postpone approval and account export.

## Production evidence

- Backup before deployment: `/home/sharedhouse-backups/sharedhouse-20260811T104742Z.dump`.
- Isolated Compose project: `sharedhouse-production`; unrelated-container gate passed.
- API/worker image tag: `0.1.0-tasks-workflow-20260811`.
- Migration `0010_household_tasks.sql` is recorded on PostgreSQL; existing production counts remained
  one user and one household, with zero invented task records.
- Public health and readiness returned HTTP 200 in about 0.12–0.14 seconds; an unauthenticated task
  query returned 401; API/worker/PostgreSQL/tunnel restart counts were zero.
- API and worker startup logs showed task routes registered and no runtime error.
- Signed APK: `SharedHouse-v0.4.0-public-release-signed.apk`, 4,749,227 bytes, SHA-256
  `546F3ED8304F92B6E2DF48E97DFA4FF49995C6C7815EFED7E94DC9B228CF48E5`, signed by the existing
  `CN=SharedHouse Direct Release` RSA-4096 certificate using APK signature schemes v2/v3. A matching
  archive is stored in `/home/sharedhouse-releases/`.

## Security and privacy review

- Read-only members cannot mutate or be assigned work; regular members act only on their own
  assignment; owner/admin decisions are explicit.
- Task content is not logged in normal API telemetry; audit/outbox payloads are scoped and do not
  contain credentials.
- History uses append evidence and state transitions rather than destructive deletion.
- No synthetic production task was inserted during deployment validation.

## Limitations and next task

- Physical-device visual/accessibility validation is still required; no USB/emulator device was
  available in this run.
- Reusable chore templates, recurrence/series scope, balanced or round-robin strategies, private
  exemptions, photo evidence, generated calendar entries and remote notification delivery remain.
- Next: EPIC-08-01B chore templates, recurrence and deterministic fair assignment, including DST,
  join/leave and exemption tests.
