# Mobile Permission Plan

## Required or conditional permissions

| Capability | Android | iOS | Timing | Alternative |
|---|---|---|---|---|
| Push notifications | Runtime notification permission where required | Notification authorisation | After value explanation | In-app inbox and badges |
| Avatar/receipt image | System Photo Picker; no broad library permission | PhotosPicker | User taps choose image | Initials/generated avatar |
| Camera (future optional) | Camera runtime permission | Camera usage description | User taps scan/take photo | Photo picker/manual entry |
| Biometrics | Biometric prompt, no sensitive permission | LocalAuthentication | User enables app lock | Device passcode/session login |

## Permissions not permitted in MVP

No location, background location, contacts, SMS, call logs, phone state, microphone, Bluetooth, nearby devices, accessibility service, notification listener, broad file storage, calendar read/write or exact alarm permission.

A future feature requiring one of these permissions needs a new product justification, data-flow update, privacy/store review, threat model and user-facing fallback.
