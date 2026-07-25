# API and Event Contracts

## 1. General conventions

- Base path: `/v1`.
- JSON uses `camelCase`; database names use `snake_case`.
- IDs are opaque UUIDv7 strings.
- Money is `{ "minorUnits": 12345, "currency": "GBP" }`.
- Instants are RFC 3339 UTC timestamps.
- Household-local dates use ISO `YYYY-MM-DD` plus the household IANA timezone.
- Mutating requests accept `Idempotency-Key` and `If-Match` where relevant.
- Errors follow a stable problem-details schema with a safe code, message, correlation ID and field violations.

## 2. Identity and account endpoints

```text
POST   /v1/auth/register
POST   /v1/auth/verify-email
POST   /v1/auth/sign-in
POST   /v1/auth/refresh
POST   /v1/auth/sign-out
GET    /v1/account
PATCH  /v1/account/profile
GET    /v1/account/devices
DELETE /v1/account/devices/{deviceId}
POST   /v1/account/export-requests
POST   /v1/account/deletion-requests
GET    /v1/account/deletion-requests/{requestId}
```

Authentication implementation may use a managed identity provider or an internal service, but account linkage, export and deletion remain owned by SharedHouse.

## 3. Household and invitation endpoints

```text
POST   /v1/households
GET    /v1/households
GET    /v1/households/{householdId}
PATCH  /v1/households/{householdId}
GET    /v1/households/{householdId}/members
PATCH  /v1/households/{householdId}/members/{membershipId}
DELETE /v1/households/{householdId}/members/{membershipId}
POST   /v1/households/{householdId}/invitations
GET    /v1/invitations/{inviteToken}/preview
POST   /v1/invitations/{inviteToken}/accept
POST   /v1/households/{householdId}/invitations/{invitationId}/revoke
```

Invitation tokens are single-purpose, high-entropy, time-limited and stored as hashes. Preview returns only household display name, inviter name/avatar, intended role, expiry and safe join requirements.

## 4. Ledger endpoints

```text
GET    /v1/households/{householdId}/cycles
POST   /v1/households/{householdId}/cycles
GET    /v1/households/{householdId}/expenses
POST   /v1/households/{householdId}/expenses
GET    /v1/households/{householdId}/expenses/{expenseId}
PATCH  /v1/households/{householdId}/expenses/{expenseId}
POST   /v1/households/{householdId}/expenses/{expenseId}/approve
POST   /v1/households/{householdId}/expenses/{expenseId}/reverse
POST   /v1/households/{householdId}/allocations/{allocationId}/payment-declarations
POST   /v1/households/{householdId}/payment-declarations/{paymentId}/confirm
POST   /v1/households/{householdId}/payment-declarations/{paymentId}/dispute
POST   /v1/households/{householdId}/payment-declarations/{paymentId}/reverse
GET    /v1/households/{householdId}/ledger-summary
```

A client cannot submit a final total. It submits source values and the server returns the calculated allocation and summary.

## 5. Chores and calendar endpoints

```text
GET    /v1/households/{householdId}/chore-templates
POST   /v1/households/{householdId}/chore-templates
PATCH  /v1/households/{householdId}/chore-templates/{templateId}
GET    /v1/households/{householdId}/chore-assignments
POST   /v1/households/{householdId}/chore-assignments/{assignmentId}/complete
POST   /v1/households/{householdId}/chore-assignments/{assignmentId}/request-help
POST   /v1/households/{householdId}/chore-assignments/{assignmentId}/request-swap
POST   /v1/households/{householdId}/chore-assignments/{assignmentId}/request-postpone
POST   /v1/households/{householdId}/task-requests/{requestId}/respond
GET    /v1/households/{householdId}/calendar?from=&to=&filters=
```

## 6. Shopping endpoints

```text
GET    /v1/households/{householdId}/shopping-lists
POST   /v1/households/{householdId}/shopping-lists
POST   /v1/households/{householdId}/shopping-lists/{listId}/items
PATCH  /v1/households/{householdId}/shopping-items/{itemId}
POST   /v1/households/{householdId}/shopping-items/{itemId}/claim
POST   /v1/households/{householdId}/purchases
POST   /v1/households/{householdId}/purchases/{purchaseId}/approve
```

## 7. Notification and entitlement endpoints

```text
GET    /v1/notification-preferences
PATCH  /v1/notification-preferences
POST   /v1/devices/push-tokens
DELETE /v1/devices/push-tokens/{tokenId}
GET    /v1/inbox
POST   /v1/inbox/{messageId}/read
POST   /v1/billing/apple/transactions/verify
POST   /v1/billing/google/purchases/verify
GET    /v1/entitlements
POST   /v1/billing/restore
```

Store server-notification endpoints are not authenticated as users; they use provider-specific signature verification and strict replay controls.

## 8. Sync endpoints

```text
GET  /v1/sync?householdId=&cursor=&limit=
POST /v1/sync/commands
```

The pull response contains ordered changes, deletions represented as tombstones, the next cursor and a `hasMore` flag. Batched commands carry client-generated operation IDs. The server returns committed, rejected or conflict results per command.

## 9. Domain event envelope

```json
{
  "eventId": "uuidv7",
  "eventType": "ledger.payment_declared.v1",
  "occurredAt": "2026-07-25T10:30:00Z",
  "aggregateType": "paymentDeclaration",
  "aggregateId": "uuidv7",
  "aggregateVersion": 3,
  "householdId": "uuidv7",
  "actor": { "type": "user", "id": "uuidv7" },
  "correlationId": "uuidv7",
  "payload": {}
}
```

## 10. Core event catalogue

- `household.created.v1`
- `membership.joined.v1`, `membership.role_changed.v1`, `membership.left.v1`
- `invitation.created.v1`, `invitation.accepted.v1`, `invitation.revoked.v1`
- `ledger.expense_created.v1`, `ledger.expense_revised.v1`, `ledger.expense_reversed.v1`
- `ledger.payment_declared.v1`, `ledger.payment_confirmed.v1`, `ledger.payment_disputed.v1`
- `chore.assignment_created.v1`, `chore.completed.v1`, `chore.request_created.v1`, `chore.request_resolved.v1`
- `shopping.purchase_recorded.v1`, `shopping.purchase_approved.v1`
- `notification.delivery_requested.v1`, `notification.delivery_result.v1`
- `billing.provider_event_received.v1`, `billing.entitlement_changed.v1`
- `privacy.export_requested.v1`, `privacy.deletion_requested.v1`, `privacy.deletion_completed.v1`

Event payloads are versioned. Consumers must ignore unknown additive fields and reject incompatible event versions safely.
