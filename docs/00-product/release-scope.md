# Release Scope and Non-Functional Requirements

## MVP launch gate

The MVP is launchable only when the complete path works for both English and Romanian:

1. Household owner creates a household with a UK timezone and GBP currency.
2. Owner invites five or more users.
3. Users accept invitations, create accounts and set avatars.
4. Owner creates a 14-day or monthly cycle.
5. Rent, electricity, gas and internet are generated and split.
6. Each member sees a correct personal total.
7. A member records payment and the outstanding total is reduced with audit history.
8. Owner creates cleaning, outdoor and bin chores.
9. Chores appear with avatars on the calendar.
10. A member requests help, swap and postponement; authorised users resolve each flow.
11. Notifications are delivered and can be configured or disabled by category.
12. Subscription purchase, renewal, cancellation and refund states reconcile through store notifications.
13. Export and deletion flows work.
14. Platform admin operations are audited and least-privileged.
15. Store metadata, privacy disclosures and review account are prepared.

## Reliability targets

- API monthly availability target: 99.9% after general availability.
- Financial command durability: no acknowledged write is lost.
- Duplicate webhook and offline-command processing: idempotent.
- P95 read API latency target: under 400 ms within primary region under normal load.
- Notification scheduling accuracy: within five minutes for normal reminders, subject to platform delivery limits.
- Recovery point objective: 15 minutes for primary transactional data; recovery time objective: four hours for regional service restoration.

## Performance targets

- Cold start: under 2.5 seconds on supported mid-range devices for cached dashboard.
- Dashboard interactive after warm start: under 700 ms from local cache.
- Calendar month rendering: 60 fps for normal household sizes.
- Offline queue: at least 500 pending operations with bounded storage.
- Attachment upload: resumable and compressed with clear progress/error state.

## Accessibility targets

- WCAG 2.2 AA for web surfaces.
- Android accessibility checks and TalkBack test coverage.
- iOS VoiceOver, Dynamic Type, Reduce Motion and Increase Contrast validation.
- Minimum touch targets per platform guidance.
- No colour-only status communication.

## Supported versions

The release pipeline must use current store-required build SDKs at submission time. As of 25 July 2026, the planning baseline is Android 16/API 36 target for Google Play submissions from 31 August 2026 and Xcode 26 with iOS/iPadOS 26 SDK or later for App Store uploads. Minimum supported runtime versions are product decisions and may be lower, subject to security and library support.

## Scale assumptions

Initial design target:

- 100,000 registered users;
- 25,000 active households;
- 50 members per normal household, with higher limits tested for premium organisations;
- 10 million ledger/task/calendar records;
- notification bursts at local morning/evening boundaries;
- multi-region expansion without cross-region household writes.
