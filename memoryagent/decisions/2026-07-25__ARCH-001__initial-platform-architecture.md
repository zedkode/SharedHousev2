# ARCH-001 — Initial platform architecture

**Date:** 2026-07-25  
**Status:** approved baseline for implementation planning

## Context

SharedHouse requires native-quality Android/iOS UX, identical financial/recurrence rules, offline operation, store billing, a platform-admin website and privacy/security controls for UK/EU/US launch planning.

## Decision

- Kotlin Multiplatform shares domain, data, network, sync and validation logic.
- Android uses Jetpack Compose; iOS uses SwiftUI.
- NestJS/TypeScript modular monolith with PostgreSQL, Redis, object storage and workers.
- React/TypeScript/Vite/Tailwind platform administration portal.
- REST/OpenAPI public contract with event/outbox internal integration.
- App-store purchases are server-verified and separated from household physical/financial obligations.
- MVP records money but does not hold or transmit it.

## Consequences

The team must maintain native UI expertise plus Kotlin Multiplatform. Domain rules have one shared implementation. Native billing, notifications, deep links, accessibility and secure storage remain platform adapters. Microservices are deferred until measured need.

## Review triggers

Major KMP tooling limitation, payment initiation/custody, public web member client, more than one independent backend scaling boundary, or regulatory expansion.
