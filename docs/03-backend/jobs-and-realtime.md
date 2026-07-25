# Jobs, Scheduling and Realtime Behaviour

## 1. Scheduled jobs

- Generate recurring billing cycles and expense occurrences.
- Generate chore assignments from templates.
- Schedule due-soon and overdue notifications.
- Expire invitations and task requests.
- Reconcile Apple and Google subscription state.
- Build privacy exports and execute deletion stages.
- Recalculate materialised ledger summaries and integrity checks.
- Delete expired media upload intents and export packages.
- Enforce retention policy and legal-hold exclusions.

Every scheduled job uses a distributed lock or deterministic work key, records progress and is safely retryable.

## 2. Notification scheduling

Persist a notification intent before delivery. Resolve the user’s latest preferences and timezone near send time. Collapse superseded reminders. Never send an overdue alert when a payment or task completion has already committed.

Push content should be privacy-minimised by default, for example “A household bill is due tomorrow” rather than exposing amount or supplier on a locked screen. Users may opt into detailed previews.

## 3. Realtime updates

Use WebSocket or Server-Sent Events for foreground household updates only after authentication and membership authorisation. Events contain resource IDs, versions and minimal changed fields; the client fetches authoritative details. Push notification remains the background wake/awareness channel.

## 4. Event ordering

Per aggregate, event versions are monotonic. Clients discard older versions and request a resync when a version gap cannot be filled. Cross-aggregate ordering is not guaranteed, so calendar and summary endpoints expose an `asOf` cursor.

## 5. Worker failure handling

- Exponential backoff with bounded retries.
- Dead-letter queue for non-transient failures.
- Operator dashboard with reason, attempts and safe replay.
- No blind replay of financial commands; replay domain events only through idempotent handlers.
- Alert on queue age, repeated provider failures and job-generation gaps.

## 6. Timezone and daylight saving

Store recurrence semantics as local date/time and IANA timezone, then resolve each occurrence independently. Never add a fixed number of UTC hours to create future local recurrences. Ambiguous or skipped local times follow a documented resolver and appear in test fixtures.
