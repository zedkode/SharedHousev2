# Support and Operations

## 1. Support case lifecycle

States: `new`, `triaged`, `waiting_for_user`, `in_progress`, `escalated`, `resolved`, `closed`. Cases have category, severity, account/household references, user-provided attachments, assigned team, communication history and resolution summary.

## 2. Identity verification

Support never asks for passwords, complete payment credentials, recovery codes or invite tokens. Account recovery uses verified channels and risk-based controls. High-risk changes require recent authentication or a documented recovery workflow.

## 3. Content-access escalation

When resolving a case genuinely requires private content:

1. Record the user request and precise data scope.
2. Obtain a permitted support purpose and approval.
3. Grant time-limited access to named staff.
4. Mask unrelated members and fields where possible.
5. Log each content view and action.
6. Revoke access automatically and include it in the case resolution.

## 4. Common playbooks

### Purchase not recognised

Verify account binding, provider environment, transaction state and notification processing. Trigger provider reconciliation. Never grant permanent access from a screenshot alone.

### Wrong household total

Inspect ledger integrity summaries and safe record metadata. Reproduce calculations using record IDs and versions. Do not manually edit materialised totals; correct the source record through a documented domain action.

### Lost access to household

Confirm account identity and membership history. Household owners control normal membership. Platform staff intervene only for account/security faults or documented policy enforcement, not ordinary roommate disputes.

### Notification complaint

Inspect preference version, device token state, job decision and delivery result. Do not reveal another member’s private notification content.

### Account deletion blocked

Identify active household ownership, privacy job stage or legal/retention exception. Provide a precise next action and do not keep the request indefinitely unresolved.

## 5. Service operations

Operational dashboards cover API health, database capacity, queue depth/age, provider notification failures, push/email delivery, error rates, latency, backup status and privacy workflow deadlines.

## 6. Incident mode

An incident record has severity, commander, timeline, affected systems/users, current mitigation, communication decision and follow-up actions. Feature flags or maintenance mode must preserve account safety and access to required privacy/subscription information where possible.

## 7. Abuse and moderation

The product is household collaboration, not a public social network. Abuse controls focus on invitation spam, harassment through announcements/notifications, malicious uploads, account compromise and platform misuse. Household interpersonal disputes are not adjudicated by automated scoring.
