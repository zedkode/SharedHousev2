# Apple App Store and Google Play Compliance

## 1. Release baseline dated 8 August 2026

Store requirements change. Before every release, the release manager verifies the current official Apple and Google documentation and records the result. At this document date, plan builds around the currently required Apple toolchain/SDK and Google target-API deadlines rather than relying on historical requirements.

## 2. Apple requirements

- Use native StoreKit for eligible digital subscriptions and features sold in the iOS app unless a reviewed regional entitlement/rule explicitly permits another flow.
- Implement restore/purchase-state recovery and a clear subscription management path.
- Process verified transactions and App Store Server Notifications V2 on the backend.
- Provide App Privacy details matching actual collection, including third-party SDK behaviour.
- Maintain required privacy manifests and required-reason API declarations for included SDKs/APIs.
- Offer in-app account deletion when account creation exists.
- Provide complete review notes, test account and backend availability.
- Request only entitlements that the app genuinely uses. Do not request Critical Alerts for routine household reminders.

## 3. Google Play requirements

- Use Google Play Billing for in-app digital products/subscriptions unless a current programme/rule and region-specific implementation has been reviewed.
- Verify purchase tokens server-side and process Real-time Developer Notifications.
- Complete the Data safety form accurately for app and SDK behaviour.
- Publish a privacy policy and provide an account-deletion path in the app plus the required web resource where applicable.
- Request only necessary runtime permissions and provide prominent disclosure when collection is not reasonably expected.
- Meet current target API, billing library and app-bundle/signing requirements.
- Keep store listing, screenshots, declarations and in-app behaviour consistent.

The Android build contains optional Firebase Analytics, Firebase Crashlytics, Google Mobile Ads and
UMP integrations. Analytics, crash reporting and ads default to off and are independently controlled
from the in-app privacy settings; advertising is confined to a labelled Guides banner after UMP
permits requests. These technical defaults do not replace Data safety disclosure, a lawful-basis
assessment, a real privacy notice or testing against the exact production binary. Follow the
[Google Play, Firebase and AdMob production runbook](../09-delivery/google-play-firebase-admob-runbook.md)
for the evidence and release gates.

## 4. Store listing

Listings describe a household organiser and ledger, not a banking/payment service. Avoid claims such as “guarantees payment”, “legally proves debt”, “official emergency alert” or “fully compliant worldwide”. Clearly distinguish free and paid capabilities.

Required assets include app icon, phone screenshots, optional tablet screenshots, feature artwork where required, localized description, support URL, privacy URL, deletion URL and terms URL.

## 5. Review account and demo data

Provide a stable review household with synthetic members, bills, chores, shopping and calendar events. Never provide real tenant data. Review instructions explain invitations without requiring reviewers to control multiple external email accounts; use test-mode tools approved for review only.

## 6. Permissions and declarations

The core app requires internet access. Notification permission is requested contextually. Photos use system pickers where possible. Camera is optional for receipt/meter capture. Biometrics use platform APIs for local app protection. No contacts, precise location, microphone, SMS, call log, background location, accessibility service or device-admin permission is justified for MVP.

## 7. Subscription disclosures

Before confirmation, show product name, billing period, price returned by the store, trial/introductory terms, auto-renewal, cancellation/management path and links required by platform rules. Never hardcode a price displayed as authoritative.

## 8. Release evidence

For each release retain:

- store-rule verification date and links;
- SDK/privacy inventory;
- Data safety/App Privacy answers;
- permission list and purpose;
- account-deletion test;
- purchase/restore/refund lifecycle test;
- reviewer credentials/instructions;
- screenshots of final consent and subscription screens;
- signed build provenance and version identifiers.
