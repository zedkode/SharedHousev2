# Platform Administration Web Portal

## 1. Scope

The platform administration portal is for the company operating SharedHouse. It is not the same as the household-administration screens in the mobile app. It manages application commerce, catalogue configuration, entitlements, support, operational health, privacy requests, releases and audited platform controls.

The portal must never provide casual browsing of private household content. Default views use aggregated or metadata-only information. Any exceptional content access requires a support case, explicit reason, elevated permission, time limit and audit event.

## 2. Recommended web stack

- React and TypeScript with Vite.
- Tailwind CSS with an accessible headless component system.
- TanStack Router and TanStack Query.
- OpenAPI-generated client and runtime schema validation.
- Server-mediated authentication using secure cookies where practical.
- Strong Content Security Policy and no unsafe inline scripts.

## 3. Platform roles

| Role | Main access |
|---|---|
| Support Viewer | Account and household metadata, support tickets, no private content |
| Support Specialist | Approved account recovery/support actions, entitlement refresh |
| Commerce Manager | Products, plans, offers, purchase reconciliation and reporting |
| Privacy Operator | Export/deletion queues and retention evidence |
| Security Operator | Security events, session revocation and incident tools |
| Release Manager | App versions, compatibility, feature rollout and maintenance notices |
| Platform Administrator | Configuration and user administration within assigned scope |
| Super Administrator | Break-glass operations only; strongest MFA and mandatory reason |

Permissions are atomic and assigned through roles. Production access is periodically reviewed. No role should combine unrestricted support content access, billing modification and audit administration by default.

## 4. Navigation

- Overview
- Users
- Households
- Support cases
- Products and plans
- Store transactions
- Entitlements
- Offers and promotions
- App versions and rollout
- Notifications and templates
- Privacy requests
- Security and sessions
- Jobs and webhooks
- Audit log
- System health
- Configuration

## 5. Overview dashboard

Show active users/households, subscription status distribution, failed provider events, privacy request deadlines, critical job failures, app-version adoption, support queue and operational incidents. Avoid displaying revenue or retention metrics without a defined source, timezone and currency context.

## 6. User and household views

User view may show account status, verified contact, locales, registered devices, entitlement summary, household membership identifiers, privacy requests and support history. Household view may show member count, plan, timezone, cycle configuration, storage usage and health signals. Financial line items, notes, receipts, chore evidence and private announcements remain hidden unless an approved escalated support procedure permits limited access.

## 7. Safe administrative actions

- Revoke sessions or devices.
- Resend verification/invitation within rate limits.
- Trigger store-state reconciliation.
- Apply or remove a time-limited support entitlement.
- Suspend an account or household for documented abuse/security reason.
- Correct safe display metadata.
- Re-run failed privacy export/deletion stages.
- Disable a compromised notification template or feature rollout.

Every state-changing action requires confirmation, reason and audit evidence. Destructive or high-impact actions use step-up authentication and, where practical, four-eyes approval.

## 8. Search and data minimisation

Search by exact email, user ID, household ID, transaction ID or support reference. Do not offer broad text search across household content. Results are permission-filtered and sensitive values are masked.

## 9. Audit interface

Audit entries show time, actor, role, action, target, reason, result, correlation ID and safe before/after summaries. Audit logs are searchable but immutable through the portal. Export requires a privileged permission and is itself audited.

## 10. Operational safety

Production portal sessions are short-lived, protected by MFA and restricted by risk controls. Bulk actions require preview and limits. Admin UI must clearly label production versus staging to prevent accidental cross-environment operation.
