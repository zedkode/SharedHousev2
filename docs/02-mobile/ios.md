# iOS Implementation Specification

## Technology

- SwiftUI application lifecycle.
- Native NavigationStack and platform presentation patterns.
- Kotlin Multiplatform framework exposed through Swift-friendly adapters.
- Apple Push Notification service for push transport.
- StoreKit 2 for digital subscriptions and transaction observation.
- Keychain for credential storage.
- PhotosPicker for avatar and receipt selection.
- BackgroundTasks only for legitimate refresh; core correctness must not depend on background execution.

## Platform behaviour

- Support light, dark and system appearance.
- Use Dynamic Type, VoiceOver labels, Reduce Motion and Increase Contrast.
- Request notification permission after onboarding explains categories.
- Use standard or Time Sensitive notifications only when the user opts into a suitable category and platform policy permits.
- Do not request Critical Alerts entitlement for the MVP.
- Use app settings to direct users to system notification settings where needed.

## Store baseline

As of 28 April 2026, App Store uploads must be built with Xcode 26 or later using the iOS/iPadOS 26 SDK or later. The deployment target can be lower if security, library and testing coverage support it.

## Privacy preparation

Maintain accurate App Privacy details, required privacy policy URL and a valid privacy manifest, including required-reason APIs used by the app or third-party SDKs. Account deletion must be initiable from within the app.

## Review preparation

Provide a stable review account or review mode with sample household data, complete backend availability, clear review notes for subscriptions and invitation flows, and working in-app purchases visible to reviewers.

## Testing

- Swift unit tests for adapters and platform logic.
- XCUITest for invitation, onboarding, totals, payment declaration, chore actions, notification settings, subscription and deletion.
- StoreKit test configuration plus sandbox testing.
- TestFlight internal and external groups with staged feature gates.
