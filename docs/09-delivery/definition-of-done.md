# Definition of Done

A task is done only when all applicable conditions are true.

## Product and scope

- Acceptance criteria are met without undocumented behaviour.
- Permission, financial, notification and entitlement semantics match the approved docs.
- Empty, loading, offline, error, denied and conflict states are designed.

## Engineering

- Code compiles with strict checks and follows module boundaries.
- Unit/integration/contract/UI tests cover the change and pass.
- No new critical/high dependency or security issue is introduced.
- Database migrations are tested from the prior production version.
- API/events remain compatible or follow approved versioning.

## Security and privacy

- Household scope and least privilege are tested.
- Logs/analytics contain no prohibited sensitive data.
- New data has purpose, disclosure, retention and deletion behaviour.
- Store purchases are server-verified; privileged actions are audited.

## UX, localisation and accessibility

- Android and iOS use appropriate native behaviour.
- English and Romanian strings are complete and reviewed.
- TalkBack/VoiceOver labels, focus, text scaling, contrast and reduced motion are checked.
- Light, dark and system appearance are validated.

## Operations and documentation

- Monitoring and safe error telemetry exist.
- Runbooks/configuration are updated where required.
- Relevant product/technical/user/admin docs are updated, not duplicated.
- A factual `memoryagent` entry is added and indexed.
- No secrets, real user data or unsupported legal/compliance claims are committed.
