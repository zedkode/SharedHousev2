# Mobile Architecture

## Chosen approach

Use Kotlin Multiplatform for shared domain logic, validation, API contracts, synchronisation and local data orchestration. Use Jetpack Compose for Android presentation and SwiftUI for iOS presentation.

This approach gives one source of truth for money, cycles, permissions and offline conflict rules while preserving native navigation, accessibility, notifications, widgets, secure storage and store billing integrations.

## Proposed repository structure

```text
apps/
  android/
    app/
    feature-auth/
    feature-home/
    feature-calendar/
    feature-money/
    feature-tasks/
    feature-house/
    platform-notifications/
    platform-billing/
  ios/
    SharedHouseApp/
    Features/
    Platform/
shared/
  domain/
  application/
  data/
  database/
  network/
  sync/
  localization/
  testing/
services/
  api/
  workers/
apps/admin-web/
packages/contracts/
infra/
```

## Shared layers

- **Domain:** immutable entities/value objects, money, cycle rules, split algorithms, permission decisions and state machines.
- **Application:** use cases and ports; no platform UI dependencies.
- **Data:** repositories coordinating local database and remote API.
- **Database:** SQLDelight or equivalent multiplatform schema with migrations.
- **Network:** Ktor client, serialised versioned contracts, idempotency headers and error mapping.
- **Sync:** outbox, cursor-based pull, conflict policies and retry backoff.

## State management

Each feature exposes observable immutable UI state and explicit intents. Domain writes go through use cases. UI components never calculate authoritative totals themselves.

## Local storage

Cache household and user data required for normal offline use. Encrypt platform tokens in Keychain/Keystore. Avoid custom encryption for the entire local database unless threat modelling and performance testing justify it; minimise cached sensitive data and rely on OS file protection plus secure token storage.

## Compatibility

All API requests send client version and supported contract version. The server supports a documented compatibility window. Remote feature flags cannot weaken security, enable unreviewed data collection, or alter settled financial history.
