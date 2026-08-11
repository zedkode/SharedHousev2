# Household Administrator Guide

## 1. Household admin role

A household owner/admin configures the shared home, members, billing cycles, recurring costs, chores, shopping rules and announcements. This role does not provide access to the platform’s commercial/admin portal.

## 2. Create the household

Set:

- household display name;
- timezone (for a UK house normally `Europe/London`);
- default currency (normally GBP);
- 14-day or monthly cycle and due rules;
- optional rooms/zones and bin categories;
- notification defaults and quiet-hour guidance;
- approval thresholds for purchases and proposed expenses.

Avoid adding a complete street address unless a later feature genuinely requires it.

## 3. Invite members

Open the Household tab and choose **Manage invitations**. Select the intended role and, when the code
is for one person, restrict it to their email address. The code expires after seven days and becomes
unusable after one successful acceptance. Share it through a private channel and revoke unused codes
when they are no longer needed.

Owners can invite admins, members or read-only participants. Admins can invite members or read-only
participants but cannot delegate admin access. Use **Member** for normal tenants and grant admin
rights only when operationally necessary. The current release shares pasteable codes manually; do
not promise that the app emailed the recipient.

## 3A. Manage people and transfer ownership

Open **Household > People and access**. Every card shows the server-confirmed role, access state and
join date. The available buttons come from server capabilities, so a stale or modified client cannot
grant itself extra authority.

- **Change role**: an owner may choose administrator, member or read-only. An administrator may
  switch only between member and read-only and cannot create another administrator.
- **Suspend access**: immediately blocks household reads and writes while keeping the same member
  record available for a later reactivation.
- **Reactivate**: restores a suspended member after you have confirmed that access is appropriate.
- **Remove**: ends access and keeps the historical identity needed by expenses, payments and tasks.
- **Transfer ownership**: available only to the owner and only for an active admin/member. After
  confirmation, the recipient becomes the only owner and the previous owner becomes an admin.

Review confirmation text carefully. If another device changed the same membership, SharedHouse
rejects the stale version and reloads the list instead of overwriting newer data. Reassign future
tasks and settle access-sensitive matters before removal. Do not suspend someone as a substitute
for emergency services, tenancy advice or an agreed dispute process.

## 4. Configure rent and bills

Open **Money**, then use the settings action in the top bar to open **Money administration**.
Owners and administrators can create reusable household costs for rent, electricity, gas, water,
internet, council tax, groceries, supplies, maintenance, other or a user-defined category. For each
cost set the title, price in the household currency, weekly/monthly/quarterly/yearly frequency, next
due date and optional notes.

An active schedule creates one approved expense automatically on its local household due date. The
worker locks the schedule and uses a unique template/date occurrence key, so retries, restarts and
multiple instances cannot create a second occurrence. It then advances the date in the same
transaction. Month-end anchors are preserved (31 January, 28 February, 31 March), and yearly rules
handle leap years. The generated item is an obligation only; SharedHouse does not claim that an
invoice arrived or that money moved. **Add extra** creates an independent manual expense, so do not
use it for the same scheduled occurrence.

Editing a schedule changes only future occurrences and does not rewrite existing ledger entries.
Archive an obsolete schedule with a reason; archived schedules remain in the administrative
history and stop generating costs. The current release splits generated expenses equally across
members active at the occurrence time. Included-member selection and advanced split methods remain
planned.

When an amount changes, update the schedule before its next due date. Do not overwrite settled
historical cycles; reverse an incorrect generated entry with a reason and record the corrected one.

## 5. Verify payments

A member's **Declare paid** action creates a declaration for that member's exact approved allocation;
it does not move money or delete the charge. The declaration includes method, payment time and any
optional reference/note. The declaring member cannot confirm their own declaration.

Another active owner, administrator or member with write access can confirm after independently
checking the bank, cash or supplier evidence, or dispute it with a reason. Read-only members cannot
review payments. A confirmed allocation is shown as paid; declared and disputed allocations remain
in the outstanding summary. Use **Correct declaration** when a transfer was returned or the record
was wrong. Every declaration, confirmation, dispute and correction remains in audit history.

An expense with an active payment declaration cannot be reversed. Correct every active payment
record first, then reverse the incorrect charge with a separate reason. Never use SharedHouse as the
sole proof in a legal dispute; retain appropriate bank and supplier records.

## 6. Build a fair chore plan

Open **Tasks** and choose **Add task**. Give the responsibility a short title, clear optional
instructions, a room/zone, low/normal/high priority, local due date/time, realistic estimated
minutes and an active writable member. Read-only members cannot be assigned. Only owners/admins can
create assignments in the current release.

The assignee can start or complete the work and can submit help, swap, postponement or issue
requests. Open any card and review pending requests in its history. Approval of a swap changes the
assignee; approval of a postponement changes the deadline; help/issue decisions record the response.
Rejections require a reason. Cancelling or reopening a task preserves the prior completion and
decision trail. Stale device versions are rejected and refreshed rather than overwriting newer work.

Respond to swap/help/postpone requests promptly. Overrides require a reason and should not be used punitively.

Reusable templates, recurrence, separate bin-out/bin-return generation, proof photos,
round-robin/balanced assignment and private exemptions are the next chore-administration stages.
Until then, create each real occurrence explicitly; do not use placeholder assignments to simulate
automation.

## 7. Shopping rules

Define who may add items, purchase-approval threshold, receipt requirement and default reimbursement split. Before approving an unplanned purchase, verify actual total, included members and evidence where required.

## 8. Announcements and alerts

Use normal priority for routine notices. Reserve high-priority household alerts for time-sensitive home matters such as imminent access, urgent maintenance or bin deadline. Never present them as government/emergency-service warnings or use threatening language.

## 9. Member changes

Removing a member immediately ends future access and assignments after sync. Reassign upcoming responsibilities first. Historical ledger/assignment references remain. Transfer ownership before the final owner leaves.

## 10. Household closure and export

Export necessary household records, resolve active cycles/requests and cancel future recurring items. Closing the household stops future operation but does not necessarily erase every record immediately; approved retention and other members’ rights apply.

## 11. Admin checklist

Weekly: review overdue items, pending payment disputes, task requests and failed shopping approvals. Monthly/cycle end: reconcile bills, close cycle, review recurring amounts and member changes. Quarterly: review admin roles, invite links, notification defaults and subscription plan.
