# init.md — Repository Initialization and Agent Boot Sequence

This file defines the mandatory startup procedure for every human or coding agent working in the SharedHouse repository.

## 1. Mission

Build a production-grade Android and iOS application and its supporting platform for shared households. The product must be understandable to non-technical tenants, reliable for financial and chore records, privacy-first, accessible, localised in English and Romanian, and prepared for Google Play and Apple App Store distribution.

## 2. Mandatory reading order

Before making changes, read the following in order:

1. `README.md`
2. `AGENTS.md`
3. `docs/00-product/product-vision.md`
4. `docs/00-product/roles-and-permissions.md`
5. `docs/00-product/functional-specification.md`
6. The architecture file for the affected application or service
7. `docs/05-compliance/security-threat-model.md`
8. The relevant store/privacy document when handling identity, data, notifications, purchases, exports, deletion, analytics, or attachments
9. `memoryagent/INDEX.md`
10. Recent `memoryagent` entries matching the current task area

Do not begin implementation until the relevant rules are understood.

## 3. Required pre-task output

Before editing code, record in the task notes:

- task identifier and exact scope;
- files and modules expected to change;
- business rules that must remain unchanged;
- relevant permissions and data boundaries;
- tests that will prove completion;
- migrations or compatibility risks;
- unresolved questions that can be handled with documented assumptions.

## 4. Engineering constraints

- Android UI: Jetpack Compose.
- iOS UI: SwiftUI.
- Shared domain and data logic: Kotlin Multiplatform where technically appropriate.
- Backend: TypeScript/NestJS, PostgreSQL, Redis, object storage, event workers.
- Platform admin web: React/TypeScript, Vite, Tailwind CSS, TanStack Query/Router, accessible component primitives.
- API: versioned REST for commands and queries, WebSocket/SSE only where real-time value exists.
- Money: integer minor units plus ISO 4217 currency code. Never use binary floating point for money.
- Time: UTC instants for events, IANA time zones for household interpretation, local dates for billing-cycle semantics.
- Identifiers: UUIDv7 or an equivalent sortable, non-sequential public identifier.
- Tenancy: every household-domain query must be scoped by `household_id` and authorised by membership.
- Logs: no access tokens, passwords, invite secrets, full payment references, private notes, or attachment contents.
- No undocumented endpoints, fields, states, roles, or entitlement rules.

## 5. Task completion procedure

A task is not complete until all applicable steps pass:

1. Implementation compiles.
2. Static analysis and formatting pass.
3. Unit tests pass.
4. Integration/contract tests pass where relevant.
5. UI tests or screenshots validate critical flows where relevant.
6. Database migrations are forward-tested and rollback/mitigation is documented.
7. Security and privacy implications are reviewed.
8. English and Romanian strings are present for user-visible text.
9. Accessibility labels and dynamic text behaviour are checked.
10. Related documentation is updated without creating redundant documents.
11. A `memoryagent` completion entry is created.
12. `memoryagent/INDEX.md` is updated.

## 6. Memory entry requirement

Create one file per completed task:

`memoryagent/progress/YYYY-MM-DD__TASK-ID__short-title.md`

Use `memoryagent/TEMPLATE.md`. The entry must state exactly what changed, why, how it was tested, what remains, and which decisions affect future work. Never store credentials, private user data, production tokens, recovery codes, or confidential customer content.

## 7. Stop conditions

Stop and surface the issue instead of guessing when:

- the requested change conflicts with the permission matrix;
- a financial calculation is ambiguous;
- a migration can corrupt or reinterpret historical records;
- store billing behaviour conflicts with current Apple or Google rules;
- a change introduces payment custody, lending, credit scoring, debt collection, or regulated money transmission;
- a notification design could be mistaken for a government emergency alert;
- a data collection purpose lacks a documented lawful basis or disclosure;
- the codebase contains two incompatible sources of truth.
