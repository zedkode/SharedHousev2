# Product Vision and Scope

## Product statement

SharedHouse gives people living together one trusted place to understand what is due, what has been paid, what needs buying, who is responsible for each household task, and what is happening next. It replaces fragmented group chats, spreadsheets, notes, and verbal agreements with a transparent household workspace.

## Reference scenario

The initial scenario is a shared house in the United Kingdom with five adult tenants. Rent and most household costs are reviewed either every two weeks or every calendar month. The home shares electricity, gas, internet, cleaning, garden/outdoor cleaning, bin movement, and routine purchases.

The product must also support households with fewer or more members, without encoding “five” as a system limit. Commercial plans may impose entitlement limits, but the domain model must remain unbounded.

## Outcomes

The application succeeds when:

- each member can see an accurate personal amount due and the household total;
- marking an item paid reduces the outstanding amount without deleting history;
- recurring bills and chores appear automatically in the interactive calendar;
- chore events display the assigned member’s avatar;
- members can request a swap, ask for help, postpone within policy, or report a problem;
- the household administrator can invite members with a secure link;
- members receive useful, controllable reminders;
- a new user can understand the product through onboarding and contextual guidance;
- the platform can sell and manage premium subscriptions through Apple, Google, and an approved web flow;
- platform operators can support users without casually accessing private household data.

## Product principles

1. **Clarity before cleverness.** Totals, responsibility, due dates, and states must be immediately understandable.
2. **Auditability without hostility.** Preserve a factual trail but avoid public shaming and aggressive debt language.
3. **Privacy by default.** Collect only the data required for household coordination.
4. **Local-first resilience.** Users can read cached data and queue reasonable changes during poor connectivity.
5. **Platform-native quality.** Android and iOS use native interaction patterns while sharing domain rules.
6. **International foundations.** Locale, currency, timezone, tax, policy, and data-residency assumptions are explicit.
7. **No hidden financial magic.** Every total can be explained from visible entries and split rules.

## In scope for version 1

- account creation through a household invitation or direct onboarding;
- email verification, secure sessions, device management;
- profile and avatar customisation;
- household creation and invitation management;
- household roles and permissions;
- 14-day, monthly, weekly, and custom billing cycles;
- rent, energy, gas, internet, council/household services, household supplies, and custom expenses;
- recurring and one-off expenses;
- equal, fixed, percentage, weighted, usage, and custom splits;
- personal due total, household total, paid total, outstanding total;
- payment declaration/checklist, confirmation, dispute, correction and history;
- shopping lists, assignments, purchase recording and reimbursements;
- cleaning, outdoor chores, bins, inspections and custom task schedules;
- assignment avatars in calendar;
- help, swap, postpone, reassign, skip-with-reason and completion actions;
- interactive calendar with filters and event details;
- configurable reminders, high-priority household alerts, quiet hours and notification categories;
- English and Romanian localisation with device-language detection and manual override;
- light, dark and system appearance; Android dynamic colour where supported;
- first-run tutorial and contextual help;
- app subscriptions and entitlement management;
- platform administration web portal;
- export, account deletion and household deletion processes;
- security, audit, monitoring and store submission readiness.

## Explicitly out of scope for version 1

- holding, pooling or transferring tenant money;
- automated direct debit or bank payment initiation;
- credit scoring, lending, debt collection or eviction workflows;
- landlord legal notices or tenancy-law advice;
- utility provider account switching;
- government-alert integration;
- precise geolocation or member tracking;
- advertising and cross-app tracking;
- users under 18;
- public social feeds or anonymous chat;
- AI-based financial decisions.

## Product surfaces

1. Android application.
2. iOS application.
3. Public marketing and legal website.
4. Platform administration portal for commercial and operational management.
5. Backend API, background workers, notification service, store-entitlement service, export/deletion service, and audit pipeline.
6. Separate documentation experiences for members, household administrators, and platform administrators.
