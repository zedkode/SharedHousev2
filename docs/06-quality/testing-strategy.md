# Testing Strategy

## 1. Quality goals

The application must produce correct household totals, preserve history, enforce household isolation, behave predictably offline, deliver understandable notifications and support accessible English/Romanian user flows on current store-supported devices.

## 2. Test pyramid

### Unit tests

Cover money value objects, currency exponent, 14-day/monthly cycle calculations, daylight-saving boundaries, split algorithms, rounding, ledger summaries, state machines, permission policies, chore fairness, task request expiry and entitlement calculation.

Use property-based tests for split invariants:

- allocations always sum to total;
- no excluded member receives an allocation;
- deterministic input produces deterministic rounding;
- reversal restores the mathematically expected outstanding value;
- cross-currency totals are rejected.

### Integration tests

Run PostgreSQL/Redis/object-storage-compatible test containers. Test transactions, constraints, outbox atomicity, idempotency, tenancy, media upload ownership, invitation expiry, provider-event deduplication and privacy workflows.

### Contract tests

Validate OpenAPI compatibility between backend, Kotlin client, Swift-facing shared layer and admin web. Store event fixtures are versioned and tested against official sandbox payloads where available.

### UI and end-to-end tests

Critical mobile flows:

1. register/sign in;
2. accept invite and create profile/avatar;
3. view active-cycle totals;
4. create/approve bill and verify split explanation;
5. record payment and reverse/dispute it;
6. complete/swap/help/postpone chore;
7. add/buy shopping item;
8. use calendar filters and recurring-event edit scope;
9. configure notifications/quiet hours;
10. purchase/restore/manage subscription;
11. export and request deletion.

Admin portal tests cover RBAC, masked data, entitlement reconciliation, privacy requests and audited support actions.

## 3. Mobile device matrix

Test representative small/large Android phones, at least one tablet layout if supported, current and minimum Android versions, current and minimum iOS versions, dynamic text, display zoom, light/dark, English/Romanian and 12/24-hour time. Store submission builds use real release signing in a protected pipeline.

## 4. Accessibility tests

Automated checks supplement manual screen-reader testing. Verify logical focus order, labels, actions, heading structure, contrast, touch targets, font scaling, keyboard behaviour on web and that information is not colour-only. Test TalkBack and VoiceOver on all critical flows.

## 5. Security tests

- Cross-household identifier attacks.
- Lower-role access to admin actions.
- Invitation brute-force/replay.
- Refresh-token rotation/reuse.
- Upload content/type attacks.
- Provider webhook signature and replay.
- Deep-link/universal-link validation.
- Sensitive logging and screenshot/privacy behaviour where implemented.

## 6. Offline and concurrency tests

Test airplane mode, process termination, duplicated commands, long offline periods, clock skew, two devices editing the same bill, membership removal while offline and recurring-series edits. UI must explain conflicts and never silently overwrite financial history.

## 7. Performance tests

Reference household: 5 members, 5 years of bills, daily chores, 10,000 calendar items and normal media. Larger tests validate 50-member edge households without changing core correctness. Measure startup, dashboard load, calendar scrolling, sync and API p95/p99.

## 8. Release gates

No release with failing required tests, unresolved critical/high security findings, inaccurate store privacy declarations, broken account deletion, failed purchase lifecycle or known ledger-integrity defect. Exceptions require documented owner, impact, mitigation and expiry; financial-integrity and tenancy defects are not waivable for production.
