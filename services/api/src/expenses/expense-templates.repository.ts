import { Injectable } from '@nestjs/common';
import type {
  ExpenseTemplateConfiguration,
  ExpenseTemplateSummary,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface MembershipRow {
  readonly id: string;
  readonly role: HouseholdSummary['role'];
  readonly default_currency: string;
}

interface TemplateRow {
  readonly id: string;
  readonly household_id: string;
  readonly title: string;
  readonly category: ExpenseTemplateSummary['category'];
  readonly custom_category_name: string | null;
  readonly amount_minor: number | string | bigint;
  readonly currency: string;
  readonly cadence: ExpenseTemplateSummary['cadence'];
  readonly next_due_date: Date | string;
  readonly schedule_ends_on: Date | string | null;
  readonly notes: string | null;
  readonly status: ExpenseTemplateSummary['status'];
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type TemplateListResult =
  | { readonly status: 'found'; readonly templates: readonly ExpenseTemplateSummary[] }
  | { readonly status: 'not_found' };

export type TemplateCreateResult =
  | { readonly status: 'created' | 'replayed'; readonly template: ExpenseTemplateSummary }
  | {
      readonly status: 'not_found' | 'forbidden' | 'currency_mismatch' | 'idempotency_conflict';
    };

export type TemplateMutationResult =
  | { readonly status: 'updated'; readonly template: ExpenseTemplateSummary }
  | {
      readonly status:
        'not_found' | 'forbidden' | 'currency_mismatch' | 'version_conflict' | 'status_conflict';
    };

@Injectable()
export class ExpenseTemplatesRepository {
  constructor(private readonly database: DatabaseService) {}

  async list(userId: string, householdId: string): Promise<TemplateListResult> {
    const membership = await findMembership(this.database, userId, householdId);
    if (membership === null) return { status: 'not_found' };
    const rows = await this.database.query<TemplateRow>(
      `${templateSelect()} WHERE t.household_id = $1
       ${isManager(membership.role) ? '' : "AND t.status = 'active'"}
       ORDER BY t.status, t.next_due_date, t.title, t.id`,
      [householdId],
    );
    return {
      status: 'found',
      templates: rows.map((row) => mapTemplate(row, isManager(membership.role))),
    };
  }

  async create(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly configuration: ExpenseTemplateConfiguration;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<TemplateCreateResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (!isManager(membership.role)) return { status: 'forbidden' };
      if (membership.default_currency !== input.configuration.amount.currency) {
        return { status: 'currency_mismatch' };
      }
      const template: ExpenseTemplateSummary = {
        id: newUuidV7(Date.parse(input.occurredAt)),
        householdId: input.householdId,
        ...input.configuration,
        customCategoryName: input.configuration.customCategoryName ?? null,
        notes: input.configuration.notes ?? null,
        endsOn: input.configuration.endsOn ?? null,
        status: 'active',
        canManage: true,
        version: 1,
        createdAt: input.occurredAt,
        updatedAt: input.occurredAt,
      };
      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (
           user_id, operation, idempotency_key, request_hash, response_status, response_body, created_at
         ) VALUES ($1, 'expense_templates.create', $2, $3, 201, $4::jsonb, $5)
         ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
         RETURNING idempotency_key`,
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify(template),
          input.occurredAt,
        ],
      );
      if (claimed.length === 0) {
        const rows = await transaction.query<IdempotencyRow>(
          `SELECT request_hash, response_body FROM idempotency_records
           WHERE user_id = $1 AND operation = 'expense_templates.create' AND idempotency_key = $2`,
          [input.userId, input.idempotencyKey],
        );
        const existing = rows[0];
        if (existing?.request_hash !== input.requestHash) return { status: 'idempotency_conflict' };
        return { status: 'replayed', template: readTemplate(existing.response_body) };
      }
      await transaction.query(
        `INSERT INTO expense_templates (
           id, household_id, created_by_membership_id, title, category, custom_category_name,
           amount_minor, currency, cadence, next_due_date, schedule_anchor_day,
           schedule_anchor_month, schedule_ends_on, notes, status, version, created_at, updated_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
           EXTRACT(DAY FROM $10::date), EXTRACT(MONTH FROM $10::date), $11, $12, 'active', 1, $13, $13)`,
        [
          template.id,
          template.householdId,
          membership.id,
          template.title,
          template.category,
          template.customCategoryName,
          template.amount.minorUnits,
          template.amount.currency,
          template.cadence,
          template.nextDueDate,
          template.endsOn,
          template.notes,
          input.occurredAt,
        ],
      );
      await transaction.query(
        `INSERT INTO expense_template_status_events (
           id, template_id, actor_membership_id, previous_status, next_status, occurred_at
         ) VALUES ($1, $2, $3, NULL, 'active', $4)`,
        [newUuidV7(), template.id, membership.id, input.occurredAt],
      );
      await writeEvidence(
        transaction,
        input.userId,
        template,
        'ledger.expense_template_created.v1',
        input.occurredAt,
      );
      return { status: 'created', template };
    });
  }

  async update(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly templateId: string;
    readonly expectedVersion: number;
    readonly configuration: ExpenseTemplateConfiguration;
    readonly occurredAt: string;
  }): Promise<TemplateMutationResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (!isManager(membership.role)) return { status: 'forbidden' };
      if (membership.default_currency !== input.configuration.amount.currency) {
        return { status: 'currency_mismatch' };
      }
      const current = (
        await transaction.query<TemplateRow>(
          `${templateSelect()} WHERE t.id = $1 AND t.household_id = $2 FOR UPDATE`,
          [input.templateId, input.householdId],
        )
      )[0];
      if (current === undefined) return { status: 'not_found' };
      if (current.version !== input.expectedVersion) return { status: 'version_conflict' };
      if (current.status !== 'active') return { status: 'status_conflict' };
      await transaction.query(
        `UPDATE expense_templates SET title = $4, category = $5, custom_category_name = $6,
           amount_minor = $7, currency = $8, cadence = $9, next_due_date = $10,
           schedule_ends_on = $11, notes = $12,
           schedule_anchor_day = CASE
             WHEN cadence = $9::varchar AND next_due_date = $10::date THEN schedule_anchor_day
             ELSE EXTRACT(DAY FROM $10::date)::smallint
           END,
           schedule_anchor_month = CASE
             WHEN cadence = $9::varchar AND next_due_date = $10::date THEN schedule_anchor_month
             ELSE EXTRACT(MONTH FROM $10::date)::smallint
           END,
           version = version + 1, updated_at = $13
         WHERE id = $1 AND household_id = $2 AND version = $3`,
        [
          input.templateId,
          input.householdId,
          input.expectedVersion,
          input.configuration.title,
          input.configuration.category,
          input.configuration.customCategoryName ?? null,
          input.configuration.amount.minorUnits,
          input.configuration.amount.currency,
          input.configuration.cadence,
          input.configuration.nextDueDate,
          input.configuration.endsOn ?? null,
          input.configuration.notes ?? null,
          input.occurredAt,
        ],
      );
      const updated = (
        await transaction.query<TemplateRow>(
          `${templateSelect()} WHERE t.id = $1 AND t.household_id = $2`,
          [input.templateId, input.householdId],
        )
      )[0];
      if (updated === undefined) return { status: 'not_found' };
      const template = mapTemplate(updated, true);
      await writeEvidence(
        transaction,
        input.userId,
        template,
        'ledger.expense_template_updated.v1',
        input.occurredAt,
      );
      return { status: 'updated', template };
    });
  }

  async archive(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly templateId: string;
    readonly expectedVersion: number;
    readonly reason: string;
    readonly occurredAt: string;
  }): Promise<TemplateMutationResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (!isManager(membership.role)) return { status: 'forbidden' };
      const current = (
        await transaction.query<TemplateRow>(
          `${templateSelect()} WHERE t.id = $1 AND t.household_id = $2 FOR UPDATE`,
          [input.templateId, input.householdId],
        )
      )[0];
      if (current === undefined) return { status: 'not_found' };
      if (current.version !== input.expectedVersion) return { status: 'version_conflict' };
      if (current.status === 'archived') return { status: 'status_conflict' };
      await transaction.query(
        `UPDATE expense_templates SET status = 'archived', version = version + 1, updated_at = $4
         WHERE id = $1 AND household_id = $2 AND version = $3`,
        [input.templateId, input.householdId, input.expectedVersion, input.occurredAt],
      );
      await transaction.query(
        `INSERT INTO expense_template_status_events (
           id, template_id, actor_membership_id, previous_status, next_status, reason, occurred_at
         ) VALUES ($1, $2, $3, 'active', 'archived', $4, $5)`,
        [newUuidV7(), input.templateId, membership.id, input.reason, input.occurredAt],
      );
      const archived = (
        await transaction.query<TemplateRow>(
          `${templateSelect()} WHERE t.id = $1 AND t.household_id = $2`,
          [input.templateId, input.householdId],
        )
      )[0];
      if (archived === undefined) return { status: 'not_found' };
      const template = mapTemplate(archived, true);
      await writeEvidence(
        transaction,
        input.userId,
        template,
        'ledger.expense_template_archived.v1',
        input.occurredAt,
      );
      return { status: 'updated', template };
    });
  }
}

async function findMembership(
  executor: SqlExecutor,
  userId: string,
  householdId: string,
): Promise<MembershipRow | null> {
  const rows = await executor.query<MembershipRow>(
    `SELECT m.id, m.role, h.default_currency FROM household_memberships m
     JOIN households h ON h.id = m.household_id AND h.status = 'active'
     WHERE m.user_id = $1 AND m.household_id = $2 AND m.status = 'active' LIMIT 1`,
    [userId, householdId],
  );
  return rows[0] ?? null;
}

function templateSelect(): string {
  return `SELECT t.id, t.household_id, t.title, t.category, t.custom_category_name,
    t.amount_minor, t.currency, t.cadence, t.next_due_date, t.schedule_ends_on, t.notes, t.status,
    t.version, t.created_at, t.updated_at FROM expense_templates t`;
}

function mapTemplate(row: TemplateRow, canManage: boolean): ExpenseTemplateSummary {
  return {
    id: row.id,
    householdId: row.household_id,
    title: row.title,
    category: row.category,
    customCategoryName: row.custom_category_name,
    amount: { minorUnits: toSafeNumber(row.amount_minor), currency: row.currency },
    cadence: row.cadence,
    nextDueDate: toDate(row.next_due_date),
    endsOn: row.schedule_ends_on === null ? null : toDate(row.schedule_ends_on),
    notes: row.notes,
    status: row.status,
    canManage,
    version: row.version,
    createdAt: toInstant(row.created_at),
    updatedAt: toInstant(row.updated_at),
  };
}

function isManager(role: HouseholdSummary['role']): boolean {
  return role === 'owner' || role === 'admin';
}

function toSafeNumber(value: number | string | bigint): number {
  const result = Number(value);
  if (!Number.isSafeInteger(result))
    throw new Error('Stored money exceeds the supported JSON range.');
  return result;
}

function toDate(value: Date | string): string {
  return value instanceof Date ? value.toISOString().slice(0, 10) : value.slice(0, 10);
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function readTemplate(value: unknown): ExpenseTemplateSummary {
  return (typeof value === 'string' ? JSON.parse(value) : value) as ExpenseTemplateSummary;
}

async function writeEvidence(
  transaction: SqlExecutor,
  userId: string,
  template: ExpenseTemplateSummary,
  eventType: string,
  occurredAt: string,
): Promise<void> {
  const action = eventType.replace('.v1', '');
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, occurred_at
     ) VALUES ($1, $2, $3, $4, 'expense_template', $5, 'success', $6)`,
    [newUuidV7(), userId, template.householdId, action, template.id, occurredAt],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id, payload, occurred_at
     ) VALUES ($1, $2, 'expense_template', $3, $4, $5, $6::jsonb, $7)`,
    [
      newUuidV7(),
      eventType,
      template.id,
      template.householdId,
      userId,
      JSON.stringify(template),
      occurredAt,
    ],
  );
}
