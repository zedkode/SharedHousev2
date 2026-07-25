# Purchases, Plans and Entitlements

## 1. Commercial model

Mobile premium access is sold as Apple App Store and Google Play digital subscriptions or non-consumable digital products where the commercial design supports them. The backend website may present plan information and, where legally and store-policy appropriate, support web purchases through a separate payment provider. Mobile UI, links and messaging must be reviewed against the current regional store rules before each release.

Household rent, utility reimbursements and physical shopping are not app-store products and are never mixed into the platform subscription ledger.

## 2. Proposed plans

The exact prices require market validation. A safe feature model is:

| Plan | Intended scope |
|---|---|
| Free | One household, core bills/chores/calendar, limited history/media |
| Household Plus | More members, recurring rules, longer history, exports, advanced reminders |
| Household Pro | Advanced reports, expanded storage, priority support, future integrations |

Do not artificially block access to data a user has already created. When a plan downgrades, premium creation features may stop while prior records remain readable/exportable according to retention rules.

## 3. Product catalogue

The portal stores a platform product and maps it to Apple product IDs, Google product IDs and optional web-price IDs per region/environment. Fields include display key, feature bundle, billing period, trial/offer policy, availability, tax-display notes, grace behaviour and effective dates.

Store consoles remain authoritative for store-specific price and availability. The portal must flag, not silently overwrite, mismatches.

## 4. Purchase verification flow

1. App starts a store purchase using the native billing framework.
2. App sends provider transaction evidence plus an account-binding identifier to the backend.
3. Backend verifies with provider/trusted signed data.
4. Backend records the provider transaction idempotently.
5. Entitlement service calculates current status.
6. App receives the server entitlement and refreshes UI.
7. Server notifications and periodic reconciliation keep state current.

Client purchase success is a pending UI state until server verification succeeds.

## 5. Apple operations

Support App Store Server Notifications V2 and App Store Server API workflows. Validate signed payloads, separate sandbox/production, bind App Account Token when used and reconcile original transaction chains. Handle renewals, grace/billing retry, expiration, refund, revocation and offer changes according to verified provider state.

## 6. Google operations

Support Google Play Billing Library and Real-time Developer Notifications. Validate purchase tokens through the Google Play Developer API, map obfuscated account/profile IDs, acknowledge purchases as required and reconcile subscription lifecycle events. Store purchase tokens securely and prevent reuse across accounts.

## 7. Webhook/provider-event console

The portal shows provider, event ID/type, received time, signature result, processing result, retries, linked transaction/account and correlation ID. Operators may replay only idempotent processing after correcting a transient issue; they cannot alter a signed provider payload.

## 8. Entitlement console

Show the calculated entitlement timeline and its evidence. Allowed manual action is a time-limited support grant with reason, ticket and expiry. Manual edits must not masquerade as store transactions.

## 9. Refunds and cancellations

Users manage store subscriptions through the relevant store experience. The application provides clear management links. Refund decisions for store purchases normally remain with the provider unless an applicable programme provides another mechanism. The backend processes verified refund/revocation events promptly.

## 10. Reporting

Commerce reports distinguish gross store proceeds, provider commissions/taxes where reported, refunds, active entitlements and recognised internal metrics. Do not treat entitlement count as accounting revenue. Financial accounting should use provider statements and qualified accounting processes.

## 11. Fraud and abuse controls

Detect repeated token reuse, account switching, impossible provider-state transitions, excessive restores, webhook replay and admin-grant abuse. Controls must avoid automated decisions with unjustified user harm; suspicious states can enter review while retaining basic data access.
