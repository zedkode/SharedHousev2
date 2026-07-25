# Retention, Export and Deletion

## 1. Policy approach

Retention is purpose-based, not “keep everything”. Exact legal/accounting periods require counsel and finance approval for the operating entity and markets. The table below is a product baseline and must be converted into an approved schedule before production.

## 2. Proposed baseline schedule

| Data class | Proposed active retention | End-of-purpose action |
|---|---|---|
| Account/profile | Account lifetime | Delete/anonymise after closure workflow |
| Household content | While household active | Household-controlled deletion plus closure rules |
| Ledger/history | Active service plus approved dispute/accounting window | Minimise/anonymise where legally possible |
| Invitations | Until accepted/revoked/expired plus short abuse window | Delete token data; retain minimal audit |
| Push tokens | Until invalid, signed out or device removed | Delete promptly |
| Support cases | Case lifetime plus approved support window | Delete/redact attachments and content |
| Security logs | Risk-based short period | Aggregate or delete |
| Store transaction evidence | Required subscription/accounting/dispute period | Minimise payload; retain essential transaction proof |
| Privacy exports | Short download window, e.g. 7 days | Secure deletion |
| Backups | Rolling encrypted schedule | Expire automatically; deletion propagates through rotation |

## 3. Export contents

A user export includes profile, household memberships, their created/assigned records, ledger allocations/payment declarations, chores, shopping actions, notification preferences, consent records and subscription/entitlement history. Shared records may contain other members’ display identifiers; minimise third-party personal data while preserving understandable context.

Provide:

- JSON/CSV machine-readable files;
- a human-readable HTML or PDF index;
- media files the user is entitled to access;
- schema/version and generation timestamp.

Export packages are encrypted at rest, accessed through a short-lived authenticated link and deleted after expiry.

## 4. Account deletion flow

1. Re-authenticate user.
2. Explain effects and store-subscription cancellation separately.
3. Resolve last-household-owner transfer or household closure.
4. Allow export request before deletion.
5. Create a deletion request and revoke active sessions as policy defines.
6. Immediately stop optional marketing/analytics association.
7. Delete or anonymise eligible account/profile/device data.
8. Replace identity in necessary shared historical records with a neutral identifier where possible.
9. Delete eligible media and revoke links.
10. Complete provider/vendor deletion propagation.
11. Record minimal completion evidence without retaining deleted content.

## 5. Household deletion

Only an owner can request household closure. Show outstanding invitations, active members, recurring obligations, paid plan and export options. Closing a household ends future occurrences and access but follows retention rules for historical records.

## 6. Exceptions

Retention exceptions require a documented legal obligation, dispute, fraud/security investigation or legal hold. They are narrow, reviewed and inaccessible for unrelated product use. Users receive an explanation when legally permitted.

## 7. Backups

Deletion from live systems does not require unsafe immediate mutation of immutable backups. Document backup rotation, prevent restored deleted data from re-entering active service and reapply deletion tombstones after restoration.
