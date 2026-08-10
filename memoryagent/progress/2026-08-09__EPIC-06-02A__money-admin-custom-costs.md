# EPIC-06-02A — Household Money administration and custom costs

**Date:** 2026-08-09  
**Area:** contracts / database / API / Kotlin network / Android / privacy export / documentation  
**Status:** completed reusable-template vertical; automatic generation remains

## Outcome

Household owners and administrators now have a real Money administration surface. They can define
standard rent/bill costs or name a custom category, set the exact price in the household currency,
choose weekly/monthly/quarterly/yearly frequency and next due date, edit active configurations and
archive obsolete ones with a reason. Active templates can prefill a real expense for confirmation.

## Schema and API

- Migration `0007_expense_templates_custom_categories.sql` adds validated custom-category metadata
  to expenses plus versioned `expense_templates` and append-only template status events.
- OpenAPI 1.6 adds list/create/update/archive template endpoints. Create is idempotent, mutations use
  `If-Match`, tenant boundaries return 404 and only owner/admin roles may mutate.
- Template changes write audit and outbox evidence. Editing a template never rewrites existing
  ledger expenses. Archived templates are hidden from ordinary members but retained for managers.
- Account export includes relevant expense templates with action capabilities disabled.

## Android UX

- Owner/admin sees a Money settings action and a dedicated Material 3 administration sheet.
- Forms support standard and free-text custom categories, exact locale-aware currency input,
  frequency, date picker, notes, edit and reasoned archive.
- All members can see active planned costs and use one to prefill an expense. The final expense still
  requires confirmation, preserving honest financial state.
- English and Romanian resources are included; read-only/member roles never receive admin actions.

## Security and integrity

- Monetary values remain integer minor units and must match the household settlement currency.
- Database constraints enforce the relationship between `custom` and its required label.
- No payment, debt-collection, wallet or automatic charge claim is introduced.

## Remaining work

- Scheduled occurrence generation with deterministic occurrence keys and worker deduplication.
- Included-member selection and fixed/percentage/weighted/usage/custom allocation methods.
- Payment declarations, disputes, evidence and cycle reconciliation.
- VPS migration rollout and physical-device accessibility/usability verification are separate gates.
