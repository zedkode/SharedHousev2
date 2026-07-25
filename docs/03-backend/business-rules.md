# Business Rules

## 1. Money and currency

1. A household has one default currency, but imported or exceptional records may retain another currency only when explicitly supported.
2. Totals never add different currencies together. The UI groups totals by currency or uses a separately disclosed exchange-rate feature in a later release.
3. Money arithmetic uses integer minor units. Currency metadata defines decimal exponent.
4. Historical amounts are never recalculated due to a later currency, split-default or membership change.
5. Every total must be explainable from immutable sources and corrections.

## 2. Cycle generation

### Fourteen-day cycle

A cycle begins on the configured anchor local date and repeats every 14 calendar days in the household timezone. Daylight-saving changes do not alter the local-date boundaries.

### Monthly cycle

Supported rules: fixed day, last day, or offset from month end. When the requested day does not exist, use the last valid local date and mark the occurrence as adjusted.

### Closing

Closing a cycle freezes normal edits. Later corrections create adjustments or superseding records. Administrators may reopen only under a permissioned, audited workflow.

## 3. Expense lifecycle

- Members may propose an expense when permitted.
- Administrators approve recurring rules and material changes.
- Editing amount, currency, due date, included members or split method creates a new revision and notifies affected members.
- A paid expense cannot be silently reduced or deleted.
- Reversal requires reason, actor and link to the original record.

## 4. Allocation validation

- Equal splits include only active, explicitly selected memberships.
- Fixed allocations must sum exactly to total.
- Percentages use basis points and sum to 10,000.
- Weighted splits require positive weights.
- Usage splits record standing charge and usage components separately.
- Largest-remainder rounding is deterministic using stable membership ordering.

## 5. Payment declaration semantics

`recorded as paid` means a user has declared a payment; it is not proof of bank settlement. A household may configure confirmation as none, payer confirmation, recipient confirmation or administrator confirmation. A declaration can be `pending_sync`, `recorded`, `confirmed`, `disputed`, `rejected` or `reversed`.

Outstanding for a member equals committed allocations plus positive adjustments minus confirmed or policy-accepted payment declarations and negative adjustments. Pending offline operations appear separately and never alter another member’s authoritative view until committed.

## 6. Shopping reimbursement

A purchase may require approval when:

- actual total exceeds a household threshold;
- the item was not on an approved list;
- substitution exceeds configured tolerance;
- evidence is required but missing;
- the purchaser includes members not previously selected.

After approval, the system creates a linked expense and allocations. Editing the purchase later requires reconciliation against that ledger record.

## 7. Chore fairness

Balanced assignment uses completed and currently assigned weight over a configurable rolling period. It excludes inactive members and respects capability/exemption settings without exposing private reasons to other members. The system shows a transparent fairness summary, but administrators can override with a recorded reason.

## 8. Swap, help and postponement

- A swap changes the responsible member only after acceptance or admin resolution.
- Help adds one or more helpers but does not remove responsibility.
- Postponement must remain inside the template’s allowed window unless an administrator overrides it.
- Requests expire at the earlier of configured expiry or task due end.
- If a task is safety-related, postponement can require immediate admin review.

## 9. Invitations and membership

- Invite links expire and can be revoked.
- Accepting an invitation requires a verified account and explicit household join confirmation.
- Removing a member revokes future access immediately but preserves their display identity on historical records.
- A household must always have at least one owner. Ownership transfer is required before the last owner leaves or deletes their account.

## 10. Notifications

The notification scheduler respects category preference, quiet hours, timezone and platform permission. Security notifications may bypass household quiet hours only when narrowly necessary. Household high-priority mode may increase prominence but never bypass OS/user controls or imitate public warning systems.

## 11. Entitlements

- Mobile purchases are trusted only after server verification.
- Entitlement state follows provider status including active, grace, billing retry, paused, expired, refunded or revoked where applicable.
- A household plan may define member and feature limits.
- Loss of entitlement does not delete household data. Paid features become read-only or gracefully limited according to the product matrix.
- Platform support grants are time-limited, reasoned and audited.

## 12. Account deletion

Deletion requires identity re-authentication. The system checks active subscriptions, household ownership and unresolved export requests. It explains that cancelling an app account does not necessarily cancel a store subscription and links to provider management. Data is deleted, anonymised or retained according to purpose, law and the retention schedule; historical household accounting references use a neutral former-member label where identity is no longer necessary.
