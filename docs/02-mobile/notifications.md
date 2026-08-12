# Notification Architecture

## Delivery model

The target architecture keeps reminder evaluation and push intent on the backend, with APNs and FCM
as transport layers. The mobile app owns category preferences, local presentation and deep-link
routing.

The current Android release also provides a bounded local fallback: whenever an authenticated
household projection refreshes, WorkManager replaces future reminders for the known approved
expenses and the current member's tasks. This makes already-synchronised reminders survive process
death, but it is not remote push delivery and cannot discover server changes until the app next
refreshes.

## Priority levels

- **Informational:** digest, shopping updates, routine completion.
- **Reminder:** upcoming bill/task.
- **Important:** overdue obligation, unresolved request, material expense change.
- **High-priority household:** safety/urgent operational message selected by an authorised household user and allowed by recipient settings.
- **Security:** new sign-in, email/password change, suspicious session, export/deletion event.

“High-priority” is an internal household classification, not a public emergency classification.

## Preference model

Preference scope can be global, per household and per category. Store lead time, digest mode, quiet hours, sound, vibration and priority permission. Security messages have a separate policy.

## Scheduling

- Generate reminders using household timezone and local due rules.
- Deduplicate by user, event, category and reminder offset.
- Recalculate after expense/task changes.
- Respect membership state and mute periods.
- Record provider response, invalid tokens and final delivery state where available.

## Android quick actions

Assigned task reminders expose **Start** or **Complete** only when the last authoritative task model
allowed that action. The immutable pending intent targets a non-exported receiver, validates the
household/task UUID, action allow-list and optimistic version, then runs the API command through
WorkManager. The worker loads the encrypted session, rotates an expired access token when possible
and lets the server re-check membership, capability and version. Stale or unauthorised actions fail
closed and do not make the notification claim success.

Notification bodies are privacy-minimised. The private version may show the task/cost title after
unlock; the public lock-screen version shows the app name and a generic due reminder.

## Household chat notifications

Android has a separate user-controlled Chat category and notification channel. While the app is in
the foreground, an authorised SSE/incremental refresh message from another household member creates
a local notification when the chat surface is not already open. The notification intentionally
does not copy the message body onto the lock screen and messages sent by the current user are not
notified back to that user.

This is not background remote push: when Android is stopped or cannot maintain the foreground
stream, only a configured FCM provider and backend device-token fan-out can wake the device. Those
provider credentials, token registration, delivery telemetry and background chat push are not yet
implemented and must not be represented as working.

## Abuse prevention

Limit household broadcasts by role and rate. Provide reporting and mute controls. Do not allow arbitrary custom alarm sounds or language designed to impersonate official warnings. Platform administrators can suspend abuse without reading unrelated household content.
