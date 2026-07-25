# Product Acceptance Criteria

## Account and invitation

- A user can create and verify an account in English or Romanian.
- A valid link opens the installed app or safe web fallback and shows a minimal household preview.
- Expired/revoked links cannot join a household.
- Joining creates exactly one active membership and refreshes household access.

## Household administration

- An owner/admin can invite, change allowed roles, revoke invitations and remove members.
- The last owner cannot leave/delete without transfer or closure.
- Removed members lose future access while historical records remain understandable.

## Money

- Admin can configure 14-day or monthly cycles.
- Rent, electricity, gas, internet and custom expenses support recurrence.
- Every split reconciles exactly to total and has an explanation.
- Checking paid creates a payment declaration and reduces displayed outstanding appropriately.
- Reversing/unchecking creates history, not deletion.
- Overdue status follows household local time and committed payments.
- Two currencies are never silently added.

## Chores and calendar

- Recurring chores generate one assignment per occurrence.
- Calendar displays amount for money items and avatar(s) for chores.
- Member can request swap, help or postponement; state changes only after committed response.
- Recurring edits require occurrence/series scope.
- Bin-to-street and bin-return tasks can have separate due windows.

## Shopping

- Members can create, claim, substitute and complete items.
- Purchase records actual price and optional receipt.
- Approved reimbursable purchase produces a traceable ledger expense.

## Notifications

- User can disable normal household categories and set quiet hours.
- Security/service messages remain narrowly scoped.
- No notification imitates a government emergency alert or bypasses OS controls.
- Notification content respects lock-screen privacy preference.

## Design, localisation and accessibility

- Light, dark and system appearance work; Android dynamic colour is optional and safe.
- App detects device language initially and supports manual English/Romanian override.
- Large text, screen readers, reduced motion and contrast requirements pass critical flows.
- Tutorial is skippable/replayable and progressive.

## Purchases and stores

- Native purchase, restore, renewal, grace, cancellation/expiry and refund/revocation are server-reconciled.
- Entitlement is never granted solely from client state.
- Store privacy declarations match the release binary and SDKs.
- Account deletion is reachable and functional from the app and required web route.

## Security and privacy

- Cross-household API tests fail securely.
- Media is private and served only through authorised short-lived access.
- Export is machine-readable and securely delivered.
- Deletion handles ownership/subscription explanations and follows approved retention.
- Privileged portal actions require role, reason and immutable audit.
