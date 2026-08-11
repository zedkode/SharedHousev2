# Roles and Permissions

## Role namespaces

SharedHouse uses two independent role systems.

### Household roles

- **Household Owner** — created the household or received ownership transfer. Controls household settings, ownership transfer, and household deletion.
- **Household Admin** — manages members, invitations, categories, recurring expenses, cycles, chores, reminders, and household rules.
- **Finance Manager** — manages expenses, splits, meter readings, purchase records, corrections, and financial exports, but cannot remove the owner or delete the household.
- **Chore Manager** — manages templates, schedules, assignments, approvals, swaps and postponement policy.
- **Member** — views relevant household records, records own payments and purchases, completes tasks, requests changes and manages own profile/preferences.
- **Read-only Member** — can view permitted household information but cannot create or modify financial/task records.

The owner can grant multiple capabilities to a member. The implementation should use capabilities rather than hard-coded role checks so future roles can be introduced safely.

The current production membership store exposes `owner`, `admin`, `member` and `read_only` roles.
Finance Manager and Chore Manager remain planned capability bundles; the app must not display them
as assignable until the database and API support them.

### Platform roles

- **Super Administrator** — restricted break-glass role for platform configuration and role management.
- **Operations Administrator** — service status, feature flags, release controls and operational dashboards.
- **Billing Administrator** — products, regional price mapping, promotions, entitlements, store transactions and reconciliation.
- **Support Agent** — tickets and account-level support metadata; private household content is inaccessible by default.
- **Trust and Safety Analyst** — abuse, fraud and security cases under documented access controls.
- **Privacy Administrator** — export/deletion requests, retention, legal holds and consent records.
- **Auditor** — read-only access to approved audit views.

## Capability matrix

| Capability | Owner | Admin | Finance | Chore | Member | Read-only |
|---|---:|---:|---:|---:|---:|---:|
| View household dashboard | Yes | Yes | Yes | Yes | Yes | Yes |
| Invite members | Yes | Yes | No | No | No | No |
| Remove/suspend member | Yes | Yes* | No | No | No | No |
| Transfer ownership | Yes | No | No | No | No | No |
| Configure billing cycle | Yes | Yes | Yes | No | No | No |
| Create/edit expenses | Yes | Yes | Yes | No | Limited** | No |
| Confirm another member payment | Yes | Yes | Yes | No | No | No |
| Create chores and rotations | Yes | Yes | No | Yes | No | No |
| Approve swaps/postponements | Yes | Yes | No | Yes | No | No |
| Complete own chore | Yes | Yes | Yes | Yes | Yes | No |
| Record own payment/purchase | Yes | Yes | Yes | Yes | Yes | No |
| View another member private note | No | No | No | No | No | No |
| Delete household | Yes | No | No | No | No | No |

\* An admin cannot remove the owner or another administrator with protected status unless policy permits.  
\** Members may create proposed expenses or purchases; publishing to the ledger can require approval.

## Permission enforcement

Every backend operation must verify:

1. authenticated user and active account;
2. verified household membership;
3. member status is active for the relevant effective date;
4. capability required by the command;
5. target resource belongs to the same household;
6. no ownership or protected-role rule is violated;
7. optimistic version matches for conflict-sensitive updates;
8. subscription entitlement allows the feature, without overriding safety-critical access such as data export or account deletion.

## Membership lifecycle

Member states are `invited`, `active`, `suspended`, `left`, and `removed`. Historical expense splits and completed chore assignments remain linked to a stable member record after departure. A removed user loses future access immediately; their historical display name may be retained where needed for ledger integrity, with privacy-minimised representation after account deletion.

The owner may change any non-owner role and may suspend, reactivate or remove a non-owner. An admin
may manage only members and read-only members, cannot create another admin and cannot act on an
owner or admin. Nobody can suspend/remove themselves through member administration. Ownership can
move only from the current owner to another active admin/member; the transaction first demotes the
old owner to admin and then promotes exactly one new owner. Every mutation requires `If-Match`, an
idempotency key and an append-only history/audit/outbox record. Ordinary members see active people;
owner/admin views may also show suspended and removed history.

## Platform access rules

Platform staff do not inherit household access. Support tooling exposes account status, device/session state, subscription state, delivery diagnostics and consented diagnostic bundles. A content-access request requires a case ID, reason, approval, expiry and audit event. Highly sensitive operations require step-up authentication and two-person approval.
