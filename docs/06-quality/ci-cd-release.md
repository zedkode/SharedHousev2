# CI/CD and Release Engineering

## 1. Branch and review model

Use short-lived feature branches and protected main. Every change requires review, passing checks and linked task/memory entry. Security-, billing-, privacy- and migration-sensitive changes require a designated code owner.

## 2. Continuous integration

Run:

- formatting and linting;
- Kotlin/Swift/TypeScript compilation;
- unit and property tests;
- backend integration/contract tests;
- Android/iOS UI smoke tests where runners support them;
- web accessibility/static checks;
- dependency, secret, licence and container scans;
- migration validation against previous production schema;
- generated-contract drift check;
- documentation-link and required-file checks.

## 3. Build security

Use ephemeral runners where practical, least-privilege OIDC cloud access, protected signing credentials and separate store roles. Generate an SBOM and provenance attestation. Release artifacts are immutable and traceable to commit, pipeline and dependency lock files.

## 4. Environments

- Local: synthetic data only.
- Development: shared engineering, disposable.
- Staging: production-like integrations using sandbox provider environments.
- Production: real users, isolated credentials and controlled migrations.

Feature flags and configuration are environment-specific. Production data is never copied to lower environments without approved anonymisation.

## 5. Database deployment

Use expand-migrate-contract:

1. add backward-compatible schema;
2. deploy compatible application;
3. backfill with monitored jobs;
4. verify integrity;
5. switch reads/writes;
6. remove obsolete schema in a later release.

Every migration has runtime estimate, lock analysis, backup/recovery plan and rollback or forward-fix strategy.

## 6. Mobile release channels

- Android internal testing → closed testing → staged production rollout.
- iOS internal TestFlight → external TestFlight → phased App Store release where suitable.

Monitor crashes, API compatibility, purchase verification, sync failures and user support during rollout. Pause or roll back server flags before requiring an emergency mobile binary where possible.

## 7. Versioning

Use semantic product versions and monotonically increasing Android version code/iOS build number. API and event contract versions are independent. Minimum supported client version changes require notice, measured adoption and a safe update screen.

## 8. Release checklist

- Scope and release notes approved.
- Store requirement verification dated.
- Privacy declarations/SDK inventory updated.
- Database migration rehearsed.
- Purchase, account deletion, export and invite flows passed.
- English/Romanian copy reviewed.
- Accessibility and device matrix passed.
- Incident rollback owner assigned.
- Monitoring dashboard and alert thresholds confirmed.
