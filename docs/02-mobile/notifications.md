# Notification Architecture

## Delivery model

The backend owns reminder evaluation and push intent. APNs and FCM are transport layers. The mobile app owns category preferences, local presentation and deep-link routing.

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

## Abuse prevention

Limit household broadcasts by role and rate. Provide reporting and mute controls. Do not allow arbitrary custom alarm sounds or language designed to impersonate official warnings. Platform administrators can suspend abuse without reading unrelated household content.
