# Functional Specification

## 1. Identity, account and invitation

### Registration paths

A user may:

- open a signed invitation link and create an account;
- sign in to an existing account and accept the invitation;
- create a new household without an invitation;
- accept additional household invitations if their commercial plan permits multiple households.

An invitation is a single-use, high-entropy token stored only as a hash, expires by default after seven days, can be revoked, can optionally be restricted to an email address, and cannot grant a role above the inviter’s delegation authority.

### Account security

- Verified email is required before the user can access household content.
- Passwords, if supported, use a modern memory-hard hash and breach/common-password checks.
- Passwordless email magic links and passkeys are preferred upgrade paths.
- Refresh tokens rotate, are device-bound where practical, and are revocable.
- Local biometric unlock protects an existing mobile session; it is not the sole server authentication factor.
- Users can view and revoke active devices.

### Profile and personalisation

Each user can set display name, pronouns optionally, avatar, accent preference, app language, appearance, notification settings, quiet hours, accessibility preferences and default household. Avatar choices include initials, generated illustration and system photo picker upload.

## 2. Household setup

The owner configures:

- household name and optional non-precise label/address;
- country, region, timezone, currency and first day of week;
- cycle type: 14-day, calendar month, weekly or custom;
- rent due rule and grace display;
- categories and default split rules;
- chore zones: kitchen, bathrooms, common areas, bedrooms if shared, outside/garden, bins and custom;
- notification policy and allowed postponement windows;
- subscription plan and member entitlement.

Precise street address is optional and should not be required for core operation.

## 3. Billing cycles and totals

A billing cycle has a start local date, end local date, due date, timezone, currency and state. A 14-day cycle repeats from an anchor date. A monthly cycle supports day-of-month, last-day, or relative rules such as “two days before month end”. Invalid dates resolve through a documented policy, normally the last valid day of that month.

The dashboard presents:

- personal charges for the active cycle;
- personal amount recorded as paid;
- personal outstanding amount;
- pending confirmations or disputes;
- household planned total;
- household recorded-paid total;
- household outstanding total;
- forecast of the next cycle where recurring rules exist.

Every total links to an explanation view listing source expenses, split method, adjustments, payments and rounding.

## 4. Expense management

Expense types include rent, electricity, gas, internet, water, council/house services, cleaning supplies, household supplies, repairs, garden/outdoor, shared food, and custom categories.

An expense contains title, category, supplier/payee label, amount, currency, occurrence/service period, due date, responsible payer, split method, member splits, evidence attachment, notes, recurrence link, status and revision number.

States:

- `draft` — visible only to authorised editors;
- `proposed` — submitted by a member for approval;
- `scheduled` — future recurring occurrence;
- `due` — active obligation;
- `partially_paid` — payment declarations below total;
- `paid` — reconciled declarations equal total;
- `overdue` — due date passed with outstanding amount;
- `disputed` — one or more splits contested;
- `waived` — authorised adjustment removes obligation;
- `reversed` — superseded by corrective record.

## 5. Split methods

- **Equal:** divide among included members.
- **Fixed:** explicit minor-unit amount per member.
- **Percentage:** exact percentages, totalling 100%.
- **Weighted:** relative weights such as room size or occupancy.
- **Usage-based:** meter or usage units multiplied by a rate plus shared standing charge.
- **Custom:** authorised explicit allocations.

The system rejects unreconciled splits. Rounding uses deterministic largest-remainder allocation and records who receives residual minor units.

## 6. Payments and checklist behaviour

A checkbox is a UI affordance for creating a payment declaration, not a destructive toggle. When a member checks an amount as paid, the app asks for:

- paid date/time, default now;
- amount, default outstanding split;
- method label such as bank transfer, cash, card to supplier, or other;
- optional reference/evidence;
- whether confirmation is requested.

The new declaration immediately reduces the member’s displayed outstanding total locally as `syncing`. The server validates it, then returns `recorded`, `confirmed`, `rejected`, or `needs_review`.

Unchecking does not delete history. It creates a reversal request with a reason and, where policy requires, administrator approval.

## 7. Energy and gas

The app supports optional meter readings with date, unit, meter label, reading, photo and submitter. A utility expense can be estimated, invoice-based, or usage-calculated. Estimated values must be visibly labelled. Meter readings never imply connection to a utility provider unless an explicit integration exists.

## 8. Shopping and reimbursements

Household shopping includes:

- shared shopping lists and categories;
- item quantity, expected price, priority and preferred shop;
- assignment or volunteer state;
- bought/not available/substituted states;
- purchase record with actual price and optional receipt;
- selected members to split reimbursement;
- approval policy for high-value or unplanned purchases;
- duplicate-item detection and list history.

Physical purchase values are household ledger entries, not app-store purchases.

## 9. Chores and household operations

Chore templates include cleaning indoors, cleaning outdoors/garden, moving bins to the street, bringing bins back, communal inspections, restocking and custom tasks.

A template defines zone, instructions, recurrence, expected duration, difficulty/weight, required members, proof requirement, assignment method, due window, postpone policy and escalation policy.

Assignment methods:

- round-robin;
- balanced by completed weight;
- fixed member;
- volunteer first, then assign;
- manual;
- team assignment.

## 10. Help, swap and postpone

An assigned member may:

- request a full swap with a selected or any eligible member;
- request help while retaining responsibility;
- ask an administrator to reassign;
- postpone within an allowed window;
- report inability, missing supplies or safety issue;
- decline an incorrectly assigned task with a reason.

Requests have states `pending`, `accepted`, `declined`, `expired`, `cancelled`, and `admin_resolved`. Calendar avatars and reminders update only after the server commits the change.

## 11. Interactive calendar

Calendar views: agenda, week and month. Filters include all, money, chores, shopping, household events, own items and unresolved items.

Calendar events display:

- category icon and status;
- amount for financial due items;
- assigned member avatar(s) for chores;
- due window and recurrence indicator;
- conflict, swap, help, postpone or overdue badge;
- action sheet appropriate to the user’s role.

Editing a recurring series requires a clear choice: this occurrence, this and following, or entire series.

## 12. Notifications and alerts

Notification categories:

- payment due and overdue;
- payment recorded/confirmed/disputed;
- new expense or material amount change;
- chore assigned, due, overdue or completed;
- help/swap/postpone request and result;
- shopping assignment and purchase approval;
- invitation, member and role changes;
- household announcement;
- security and account activity;
- subscription and entitlement changes.

Members control categories, lead times, quiet hours, sound/vibration and high-priority household alerts. Security alerts and legally required service messages may remain enabled but should be narrowly defined.

High-priority alerts use prominent in-app banners and normal high-importance/time-sensitive notifications where the platform and user settings allow. They must not use government-alert branding, alarm sounds, false emergency language or non-dismissible behaviour.

## 13. Tutorial and help

On first launch, show a short value-oriented introduction. After invitation acceptance, guide the user through profile/avatar, household summary, dashboard totals, calendar, payment declaration, task actions and notification choices. Use progressive disclosure rather than a long mandatory tour. Every complex screen includes contextual help linked to role-specific documentation.

## 14. Settings

Settings include profile, household switcher, language, appearance, dynamic colour, accessibility, notifications, quiet hours, privacy, devices, data export, account deletion, subscription management, help and legal information.

## 15. Data export and deletion

A user can request a machine-readable export and a readable summary. Account deletion begins in the app and is also available through a public web route. The flow explains subscription cancellation separately, household ownership transfer requirements, records retained for legitimate obligations, and irreversible effects.
