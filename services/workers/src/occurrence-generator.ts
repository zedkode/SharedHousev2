import { randomBytes } from 'node:crypto';

import type { SqlExecutor, WorkerDatabase } from './database.js';
import {
  equalAllocationMinorUnits,
  nextOccurrenceDate,
  type ExpenseCadence,
} from './recurrence.js';

interface DueTemplateRow {
  readonly id: string;
  readonly household_id: string;
  readonly created_by_membership_id: string;
  readonly title: string;
  readonly category: string;
  readonly custom_category_name: string | null;
  readonly amount_minor: string | number | bigint;
  readonly currency: string;
  readonly cadence: ExpenseCadence;
  readonly next_due_date: Date | string;
  readonly schedule_anchor_day: number;
  readonly schedule_anchor_month: number;
  readonly notes: string | null;
}

interface ActiveMemberRow {
  readonly id: string;
}

export interface OccurrenceRunSummary {
  readonly processed: number;
  readonly generated: number;
  readonly alreadyPresent: number;
}

type ProcessResult = 'generated' | 'already_present' | 'none_due';

export async function generateDueExpenseOccurrences(
  database: WorkerDatabase,
  checkedAt: Date,
  batchSize: number,
): Promise<OccurrenceRunSummary> {
  if (!Number.isSafeInteger(batchSize) || batchSize < 1 || batchSize > 500) {
    throw new Error('batchSize must be between 1 and 500.');
  }

  let generated = 0;
  let alreadyPresent = 0;
  let processed = 0;
  for (let index = 0; index < batchSize; index += 1) {
    const result = await database.transaction((transaction) =>
      processOneOccurrence(transaction, checkedAt),
    );
    if (result === 'none_due') break;
    processed += 1;
    if (result === 'generated') generated += 1;
    else alreadyPresent += 1;
  }
  return { processed, generated, alreadyPresent };
}

async function processOneOccurrence(
  transaction: SqlExecutor,
  checkedAt: Date,
): Promise<ProcessResult> {
  const occurredAt = checkedAt.toISOString();
  const template = (
    await transaction.query<DueTemplateRow>(
      `SELECT t.id, t.household_id, t.created_by_membership_id, t.title, t.category,
         t.custom_category_name, t.amount_minor, t.currency, t.cadence, t.next_due_date,
         t.schedule_anchor_day, t.schedule_anchor_month, t.notes
       FROM expense_templates t
       JOIN households h ON h.id = t.household_id AND h.status = 'active'
       WHERE t.status = 'active'
         AND t.next_due_date <= ($1::timestamptz AT TIME ZONE h.timezone)::date
         AND EXISTS (
           SELECT 1 FROM household_memberships member
           WHERE member.household_id = t.household_id AND member.status = 'active'
         )
       ORDER BY t.next_due_date, t.id
       FOR UPDATE OF t SKIP LOCKED
       LIMIT 1`,
      [occurredAt],
    )
  )[0];
  if (template === undefined) return 'none_due';

  const members = await transaction.query<ActiveMemberRow>(
    `SELECT id FROM household_memberships
     WHERE household_id = $1 AND status = 'active'
     ORDER BY joined_at, id`,
    [template.household_id],
  );
  if (members.length === 0) throw new Error('Due expense template has no active members.');

  const occurrenceDate = toLocalDate(template.next_due_date);
  const expenseId = newUuidV7(checkedAt.getTime());
  const inserted = await transaction.query<{ readonly id: string }>(
    `INSERT INTO expenses (
       id, household_id, created_by_membership_id, title, category, custom_category_name,
       amount_minor, currency, due_date, notes, source_template_id, occurrence_date,
       split_method, status, version, created_at, updated_at
     ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $9,
       'equal', 'approved', 1, $12, $12)
     ON CONFLICT (source_template_id, occurrence_date)
       WHERE source_template_id IS NOT NULL DO NOTHING
     RETURNING id`,
    [
      expenseId,
      template.household_id,
      template.created_by_membership_id,
      template.title,
      template.category,
      template.custom_category_name,
      template.amount_minor,
      template.currency,
      occurrenceDate,
      template.notes,
      template.id,
      occurredAt,
    ],
  );

  if (inserted.length > 0) {
    const allocations = equalAllocationMinorUnits(template.amount_minor, members.length);
    for (const [index, member] of members.entries()) {
      const amount = allocations[index];
      if (amount === undefined) throw new Error('Allocation generation failed.');
      await transaction.query(
        `INSERT INTO expense_allocations (
           id, expense_id, membership_id, amount_minor, rounding_adjustment_minor, status, created_at
         ) VALUES ($1, $2, $3, $4, $5, 'outstanding', $6)`,
        [
          newUuidV7(checkedAt.getTime()),
          expenseId,
          member.id,
          amount.toString(),
          amount > BigInt(template.amount_minor) / BigInt(members.length) ? 1 : 0,
          occurredAt,
        ],
      );
    }
    await writeOccurrenceEvidence(transaction, template, expenseId, occurrenceDate, occurredAt);
  }

  const nextDueDate = nextOccurrenceDate(
    occurrenceDate,
    template.cadence,
    template.schedule_anchor_day,
    template.schedule_anchor_month,
  );
  await transaction.query(
    `UPDATE expense_templates
     SET next_due_date = $2, version = version + 1, updated_at = $3
     WHERE id = $1`,
    [template.id, nextDueDate, occurredAt],
  );
  return inserted.length > 0 ? 'generated' : 'already_present';
}

async function writeOccurrenceEvidence(
  transaction: SqlExecutor,
  template: DueTemplateRow,
  expenseId: string,
  occurrenceDate: string,
  occurredAt: string,
): Promise<void> {
  await transaction.query(
    `INSERT INTO expense_status_events (
       id, expense_id, actor_membership_id, previous_status, next_status, reason, occurred_at
     ) VALUES ($1, $2, NULL, NULL, 'approved', 'Generated from active household cost', $3)`,
    [newUuidV7(Date.parse(occurredAt)), expenseId, occurredAt],
  );
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, safe_details, occurred_at
     ) VALUES ($1, NULL, $2, 'ledger.recurring_expense_generated', 'expense', $3,
       'success', $4::jsonb, $5)`,
    [
      newUuidV7(Date.parse(occurredAt)),
      template.household_id,
      expenseId,
      JSON.stringify({ sourceTemplateId: template.id, occurrenceDate }),
      occurredAt,
    ],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id, payload, occurred_at
     ) VALUES ($1, 'ledger.recurring_expense_generated.v1', 'expense', $2, $3, NULL, $4::jsonb, $5)`,
    [
      newUuidV7(Date.parse(occurredAt)),
      expenseId,
      template.household_id,
      JSON.stringify({
        expenseId,
        sourceTemplateId: template.id,
        occurrenceDate,
        amount: { minorUnits: Number(template.amount_minor), currency: template.currency },
      }),
      occurredAt,
    ],
  );
}

function toLocalDate(value: Date | string): string {
  return value instanceof Date ? value.toISOString().slice(0, 10) : value.slice(0, 10);
}

function newUuidV7(timestamp = Date.now()): string {
  if (!Number.isSafeInteger(timestamp) || timestamp < 0 || timestamp > 0xffff_ffff_ffff) {
    throw new Error('UUIDv7 timestamp is outside the 48-bit range.');
  }
  const bytes = randomBytes(16);
  let remaining = BigInt(timestamp);
  for (let index = 5; index >= 0; index -= 1) {
    bytes[index] = Number(remaining & 0xffn);
    remaining >>= 8n;
  }
  bytes[6] = ((bytes[6] ?? 0) & 0x0f) | 0x70;
  bytes[8] = ((bytes[8] ?? 0) & 0x3f) | 0x80;
  const hex = bytes.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
