# Backend Security Specification

## 1. Security objectives

Protect account identity, household confidentiality, financial-record integrity, invitation secrecy, media access, subscription entitlements and administrative operations. The dominant risk is broken access control across households, followed by account takeover, insecure invitations, webhook forgery, malicious media and privileged support misuse.

## 2. Authentication

- Require verified email before joining a household.
- Passwords, when used, are hashed with a current memory-hard algorithm and per-user salt.
- Prefer passkeys and platform-supported sign-in methods as a later enhancement.
- Refresh tokens are rotating, revocable and stored as hashed identifiers server-side.
- Detect reuse of rotated refresh tokens and revoke the token family.
- Sensitive actions require recent authentication.
- Platform administrators require MFA; higher roles should require phishing-resistant MFA.

## 3. Authorisation

Use a policy layer combining authenticated user, household membership, role, resource ownership, record state and entitlement. Controllers must not implement ad hoc role checks. Repository access requires validated scope. Denial is the default.

Automated tests must include horizontal access attempts using valid IDs from another household and vertical access attempts by lower roles.

## 4. Invitation protection

Generate at least 128 bits of cryptographic randomness. Store only a token hash, use HTTPS universal/app links, impose expiry and use limits, rate-limit previews/acceptance and never include sensitive household details in an unauthenticated preview.

## 5. API controls

- TLS 1.2+ with modern configuration; prefer TLS 1.3.
- Strict request size and content-type limits.
- Schema validation with unknown-field policy.
- Per-account, per-IP and per-action rate limits.
- Idempotency and replay protection for writes.
- CSRF protection for cookie-authenticated web endpoints.
- Secure CORS allowlists.
- No stack traces or internal identifiers in production errors.

## 6. Media security

Use pre-signed upload intents with size, type and ownership constraints. Verify actual file signature, strip dangerous metadata where appropriate, malware-scan, generate safe derivatives and serve through short-lived authorised URLs. Never render user-controlled SVG/HTML directly in privileged portals.

## 7. Store purchase security

- Validate Apple signed transactions and server notifications using official libraries/trust material.
- Validate Google purchase tokens against the publisher API and process Real-time Developer Notifications.
- Verify package/bundle ID, product ID, environment, account binding and transaction uniqueness.
- Acknowledge/consume Google purchases only according to product type and current policy.
- Deduplicate notifications and reconcile provider state periodically.
- Never grant an entitlement from a screenshot, client boolean or unverified receipt.

## 8. Secrets and keys

Use a managed secret store. Rotate signing, provider and database credentials. Limit runtime identity by service. Production operators must not retrieve raw long-lived secrets through the admin UI. Keep an emergency rotation runbook.

## 9. Database and storage

Encrypt storage and backups using managed provider controls. Use least-privilege database roles and separate migration credentials. Protect backups from routine deletion and test restoration. Object keys must be non-guessable, but authorisation cannot depend on obscurity.

## 10. Logging and audit

Security logs contain correlation ID, actor, action, target type, outcome and risk signals. Redact tokens, full invite URLs, email verification codes, payment references, private notes and media URLs. Privileged actions create append-only audit events with reason and before/after safe summaries.

## 11. Dependency and supply-chain security

Pin lock files, verify provenance where supported, run dependency and container scans, enable secret scanning, generate an SBOM for releases and sign build artifacts. High/critical findings require triage before release; exceptions need owner, rationale and expiry.

## 12. Incident response

Maintain severity levels, contacts, containment procedures, evidence preservation, user/regulator notification assessment and post-incident review. Security controls and notification deadlines depend on jurisdiction and facts; legal/privacy leads decide external notifications.
