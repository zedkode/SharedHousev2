import { randomBytes } from 'node:crypto';

import type { SqlExecutor, WorkerDatabase } from './database.js';
import {
  nextOccurrenceDate,
  type ExpenseCadence,
  weightedEqualAllocationMinorUnits,
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
  readonly user_id: string;
  readonly display_name: string;
}

interface ActiveCoupleRow {
  readonly id: string;
  readonly primary_membership_id: string;
  readonly partner_membership_id: string | null;
  readonly primary_display_name: string;
  readonly partner_display_name: string;
}

interface BillingUnit {
  readonly membershipId: string;
  readonly label: string;
  readonly type: 'individual' | 'couple';
  readonly participantCount: 1 | 2;
  readonly billingCoupleId: string | null;
  readonly eligibleMembershipIds: readonly string[];
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
    `SELECT membership.id, membership.user_id, profile.display_name
     FROM household_memberships membership
     JOIN users account ON account.id = membership.user_id AND account.status = 'active'
     JOIN user_profiles profile ON profile.user_id = membership.user_id
     WHERE membership.household_id = $1 AND membership.status = 'active'
     ORDER BY membership.id`,
    [template.household_id],
  );
  if (members.length === 0) throw new Error('Due expense template has no active members.');
  const couples = await transaction.query<ActiveCoupleRow>(
    `SELECT couple.id, couple.primary_membership_id, couple.partner_membership_id,
       primary_profile.display_name AS primary_display_name,
       COALESCE(partner_profile.display_name, couple.partner_display_name) AS partner_display_name
     FROM household_billing_couples couple
     JOIN household_memberships primary_member ON primary_member.id = couple.primary_membership_id
     JOIN user_profiles primary_profile ON primary_profile.user_id = primary_member.user_id
     LEFT JOIN household_memberships partner_member ON partner_member.id = couple.partner_membership_id
     LEFT JOIN user_profiles partner_profile ON partner_profile.user_id = partner_member.user_id
     WHERE couple.household_id = $1 AND couple.status = 'active'
     ORDER BY couple.primary_membership_id, couple.id`,
    [template.household_id],
  );
  const billingUnits = buildBillingUnits(members, couples);

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
    const allocations = weightedEqualAllocationMinorUnits(
      template.amount_minor,
      billingUnits.map((unit) => unit.participantCount),
    );
    for (const [index, unit] of billingUnits.entries()) {
      const amount = allocations[index];
      if (amount === undefined) throw new Error('Allocation generation failed.');
      const allocationId = newUuidV7(checkedAt.getTime());
      await transaction.query(
        `INSERT INTO expense_allocations (
           id, expense_id, membership_id, amount_minor, rounding_adjustment_minor, status,
           billing_unit_type, billing_unit_label, participant_count, billing_couple_id, created_at
         ) VALUES ($1, $2, $3, $4, $5, 'outstanding', $6, $7, $8, $9, $10)`,
        [
          allocationId,
          expenseId,
          unit.membershipId,
          amount.amountMinor.toString(),
          amount.roundingAdjustmentMinor,
          unit.type,
          unit.label,
          unit.participantCount,
          unit.billingCoupleId,
          occurredAt,
        ],
      );
      for (const membershipId of unit.eligibleMembershipIds) {
        await transaction.query(
          `INSERT INTO expense_allocation_members (allocation_id, membership_id, created_at)
           VALUES ($1, $2, $3)`,
          [allocationId, membershipId, occurredAt],
        );
      }
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

function buildBillingUnits(
  members: readonly ActiveMemberRow[],
  couples: readonly ActiveCoupleRow[],
): readonly BillingUnit[] {
  const coupleByPrimary = new Map(couples.map((couple) => [couple.primary_membership_id, couple]));
  const partnerMemberships = new Set(
    couples.flatMap((couple) =>
      couple.partner_membership_id === null ? [] : [couple.partner_membership_id],
    ),
  );
  const units: BillingUnit[] = [];
  for (const member of members) {
    if (partnerMemberships.has(member.id)) continue;
    const couple = coupleByPrimary.get(member.id);
    if (couple === undefined) {
      units.push({
        membershipId: member.id,
        label: member.display_name,
        type: 'individual',
        participantCount: 1,
        billingCoupleId: null,
        eligibleMembershipIds: [member.id],
      });
      continue;
    }
    units.push({
      membershipId: member.id,
      label: `${couple.primary_display_name} & ${couple.partner_display_name}`,
      type: 'couple',
      participantCount: 2,
      billingCoupleId: couple.id,
      eligibleMembershipIds: [
        member.id,
        ...(couple.partner_membership_id === null ? [] : [couple.partner_membership_id]),
      ],
    });
  }
  return units;
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
