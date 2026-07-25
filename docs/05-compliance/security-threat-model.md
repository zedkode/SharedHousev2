# Security Threat Model

## 1. Assets

- Account credentials and sessions.
- Household membership and invitation capability.
- Bills, allocation/payment history and totals.
- Chore/calendar/private household information.
- Receipts, meter images and avatars.
- Store transaction evidence and entitlements.
- Privacy exports and deletion state.
- Platform-administrator privileges and audit evidence.

## 2. Trust boundaries

Mobile device; public network; CDN/WAF; API; database/cache/storage; worker queue; Apple/Google/email/push providers; platform admin browser; support operators; CI/CD and cloud control plane.

## 3. Primary threats and controls

| Threat | Example | Required controls |
|---|---|---|
| Broken household access | User changes ID and reads another house | Server membership scope, policy tests, scoped repositories |
| Account takeover | Credential stuffing/token theft | Rate limits, secure sessions, MFA for admins, device/session controls |
| Malicious invite | Guess/replay/share expired link | High entropy, hash storage, expiry, rate limits, explicit acceptance |
| Ledger tampering | Delete payment or alter total | Append corrections, versions, audit, server calculation |
| Offline replay | Duplicate queued payment | Operation IDs, idempotency, conflict handling |
| Forged store purchase | Fake receipt/client flag | Provider verification, signed notifications, reconciliation |
| Malicious upload | Script/polyglot/oversized file | Type/size validation, scanning, safe derivatives, authorised URLs |
| Notification abuse | Spam/harassment/emergency imitation | Preferences, rate limits, templates, reporting, no critical-alert mimicry |
| Admin misuse | Support browses private content | Least privilege, reason-bound access, MFA, immutable audit |
| Data leakage in logs | Token/reference/private note recorded | Structured allowlist logging, redaction tests |
| Supply-chain compromise | Malicious SDK/dependency | Pinning, SBOM, scans, provenance, signed releases |
| Privacy export exposure | Link forwarded or bucket public | Re-auth, short-lived links, private storage, expiry |

## 4. Abuse cases

- A former housemate attempts to retain access after removal.
- An owner repeatedly assigns punitive chores or alerts to harass a member.
- A member falsely marks all charges paid.
- A purchaser inflates or duplicates receipts.
- A user creates repeated invite links for spam.
- A support agent attempts unrelated household access.

The product records responsibility and provides dispute/leave/report controls, but it does not claim to resolve domestic or tenancy conflicts legally.

## 5. Mobile controls

Follow OWASP MASVS as a verification baseline: secure storage, authentication/session handling, network security, platform interaction, code quality, resilience appropriate to risk and privacy. Do not rely on certificate pinning as a universal control; if used, design rotation and failure recovery.

## 6. Security test gates

- SAST, dependency and secret scan on pull requests.
- API authorisation matrix integration tests.
- Mobile secure-storage and deep-link tests.
- Store webhook signature/replay tests.
- Upload fuzz/type tests.
- External penetration test before material public launch and after high-risk architectural changes.
- Remediation SLA and documented risk acceptance.
