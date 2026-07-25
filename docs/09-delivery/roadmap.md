# Delivery Roadmap

## Delivery principle

Build one trustworthy vertical slice before expanding breadth. Financial integrity, household isolation, account deletion, store purchase verification and accessibility are release gates, not final polish.

## Phase 0 — Foundation and validation

- Confirm product name, legal entity, markets and age policy.
- User interviews with UK shared households and at least Romanian-speaking users.
- Validate plan/pricing assumptions and free/paid boundaries.
- Establish repository, CI, contracts, environments and design system.
- Complete privacy data map, threat model and store-rule verification.

**Exit:** approved scope, architecture decision records, clickable prototype and test strategy.

## Phase 1 — Identity, household and core shell

- Registration, verification, sign-in/session/device management.
- Household creation, membership and secure invite links.
- Profile/avatar, English/Romanian, light/dark/system appearance.
- Mobile navigation, onboarding and role-aware help skeleton.
- Admin portal authentication/RBAC/audit foundation.

**Exit:** a five-person test household can join securely on Android and iOS.

## Phase 2 — Ledger and cycles

- 14-day/monthly cycles.
- Expenses, recurring rent/utilities, split methods and explanation view.
- Payment declaration, confirmation, dispute, reversal and totals.
- Offline outbox, sync cursor and conflict handling.
- Meter readings and evidence media.

**Exit:** deterministic totals pass property/integration tests and full cycle can be reconciled.

## Phase 3 — Chores, calendar and shopping

- Chore templates, assignment strategies and recurrence.
- Swap/help/postpone/completion flows.
- Calendar agenda/week/month with avatars and money events.
- Shopping list, purchase evidence, approval and reimbursement link.
- Notification categories, quiet hours and in-app inbox.

**Exit:** real household pilot completes two 14-day cycles without manual database correction.

## Phase 4 — Commerce and platform operations

- Native Apple/Google products and purchase UI.
- Backend verification, provider notifications and reconciliation.
- Entitlement gates and downgrade behaviour.
- Platform portal products, transactions, support and privacy workflows.
- Data export/deletion and public deletion page.

**Exit:** sandbox purchase lifecycle, refund/revocation, restore and account deletion pass.

## Phase 5 — Hardening and store launch

- Accessibility/manual localisation review.
- External penetration test and remediation.
- Performance, backup restoration and incident exercise.
- Store assets, privacy declarations, review accounts and legal approvals.
- Internal/closed/TestFlight pilots, staged production rollout.

**Exit:** production launch gates approved by product, engineering, security/privacy and operations.

## Phase 6 — Post-launch

- Monitor crash-free sessions, sync/ledger integrity, support and purchase reconciliation.
- Improve onboarding and notification relevance from measured evidence.
- Consider widgets, web member access, utility imports or regulated payment-provider integration only through new architecture/legal review.
