# Android Implementation Specification

## Technology

- Kotlin and Jetpack Compose.
- Material 3 components with product design tokens.
- AndroidX Navigation or a typed navigation abstraction.
- WorkManager for deferrable sync and scheduled local work.
- Firebase Cloud Messaging for push transport, with server-owned notification logic.
- Google Play Billing Library for digital subscriptions.
- Android Keystore-backed encrypted credential storage.
- System Photo Picker for avatars and receipt images.

## Platform behaviour

- Follow system light/dark theme and support dynamic colour on compatible Android versions.
- Request `POST_NOTIFICATIONS` contextually on supported versions.
- Define notification channels by category: money reminders, tasks, requests, household announcements, security and subscription.
- Allow users to open Android channel settings from the app.
- Use high-importance channels only for user-selected high-priority household alerts and security events.
- Do not request exact alarms for routine reminders; server push and WorkManager/local scheduling are sufficient.
- Do not request background location, contacts, SMS, phone, broad storage or accessibility-service permissions.

## Implemented secure-session behaviour

- Android encrypts the complete rotating session envelope with AES-256-GCM and a non-exportable
  `AndroidKeyStore` key; token material is never written to DataStore, logs, backup storage or UI
  state.
- Ciphertext is application/format-bound with authenticated additional data and atomically replaced
  in `noBackupFilesDir` after sign-in, verification and every successful refresh rotation.
- Process startup exchanges the stored refresh token before presenting household content. A
  network failure retains the credential for explicit retry; a terminal rejection, corrupt payload
  or sign-out makes the local session unrecoverable.
- The flow does not claim biometric unlock or device management. Those require dedicated policy,
  backend endpoints and instrumented device validation.

## Store baseline

The release branch must target the current Google Play requirement. As of 31 August 2026, new apps and updates must target Android 16/API 36 or higher. Use Android App Bundles, Play App Signing, internal testing, closed testing and staged production rollout.

## Data safety preparation

Maintain a machine-readable data inventory mapped to Google Play Data safety answers. Any SDK change must trigger a privacy/data-safety review. The app must expose an in-app account deletion path and a public web deletion request path.

## Testing

- JVM unit tests for shared and Android-specific logic.
- Compose UI tests for critical flows.
- Screenshot tests across light/dark, English/Romanian and font scales.
- Instrumented tests for notification channels, deep links, process death and secure storage.
- Play Billing test products and licence testers for all subscription lifecycle states.
