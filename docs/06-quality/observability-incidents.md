# Observability and Incident Management

## 1. Observability principles

Telemetry must answer what failed, for whom, where and since when without exposing household content. Use correlation IDs across API, workers and provider callbacks. Metrics use bounded labels; never use email, user ID or household ID as unbounded metric labels.

## 2. Core metrics

- Request rate, latency and error rate by endpoint class.
- Authentication failures and token-reuse detections.
- Tenancy/authorisation denials by policy code.
- Sync cursor lag, conflict and rejected command rates.
- Ledger integrity-check failures.
- Recurrence-generation gaps/duplicates.
- Queue depth and oldest job age.
- Push/email provider success categories.
- Apple/Google event age, verification and processing failures.
- Entitlement reconciliation mismatch.
- Privacy request age and workflow failures.
- Mobile crash-free sessions and app-version adoption.

## 3. Logging

Use structured events with timestamp, severity, service, environment, correlation ID, safe actor/target pseudonymous references and outcome. Apply central redaction. Restrict access and retention by operational purpose.

## 4. Tracing

Trace critical flows: invite acceptance, expense creation, payment declaration, occurrence generation, push scheduling, purchase verification, provider notification, export and deletion. Sample routine success; retain higher sampling for errors while respecting data minimisation.

## 5. Incident severity

- SEV-1: broad outage, confirmed material breach, widespread incorrect financial state or store entitlement failure.
- SEV-2: major feature unavailable, significant subset affected or serious security weakness without confirmed exploitation.
- SEV-3: degraded performance, limited feature fault or recoverable job backlog.
- SEV-4: minor defect or operational request.

## 6. Response lifecycle

Detect → declare → assign commander → contain → communicate → recover → verify integrity → close → conduct blameless review. Security/privacy incidents add evidence preservation and legal notification assessment.

## 7. User communication

Status messages state affected capability, start time, current mitigation and next confirmed update, without speculation. Do not disclose sensitive attack details during active response. Financial integrity incidents explain whether displayed totals can be trusted and which actions users should avoid.

## 8. Post-incident work

Record timeline, root causes, contributing conditions, detection gaps, corrective actions, owners and deadlines. Update tests, runbooks, threat model and `memoryagent/incidents/` with non-sensitive engineering conclusions.
