# Information Architecture and Navigation

## Mobile navigation

Use a five-destination bottom navigation structure:

1. **Home** — personal status, next due amount, next task, alerts, requests and quick actions.
2. **Calendar** — month/week/agenda with financial, chore, shopping and household events.
3. **Money** — cycles, expenses, personal balance, payments, utilities, reports.
4. **Tasks** — chores, requests, shopping lists, completion and rotations.
5. **House** — members, household settings, announcements, documents/help and subscription status.

The navigation is role-aware but stable: restricted functions are hidden or read-only inside the relevant destination rather than constantly moving tabs.

## Home dashboard hierarchy

1. Current household and cycle.
2. Personal outstanding total with explanation.
3. Next due item.
4. Next assigned household task with avatar and action.
5. Pending requests requiring the user.
6. Household notices.
7. Quick actions: record payment, add purchase, complete task, open shopping list.
8. Household summary for administrators.

## Money hierarchy

- cycle selector;
- personal summary;
- household summary (permission controlled);
- expenses grouped by due date/category;
- payment declarations and confirmations;
- utility readings;
- recurring rules;
- reports/export.

## Task hierarchy

- My tasks;
- household task board;
- requests (help, swap, postpone);
- shopping lists;
- templates and rotations for authorised users;
- history and fairness summary.

## Deep links

Deep links support invitation acceptance, expense detail, task detail, request detail, shopping list, calendar date, subscription management, security alert and account deletion. Every deep link validates authentication and household membership after navigation; URL possession never bypasses authorisation.
