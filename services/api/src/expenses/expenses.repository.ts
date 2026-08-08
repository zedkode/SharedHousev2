import { Injectable } from '@nestjs/common';
import type {
  ExpenseAllocationSummary,
  ExpenseConfiguration,
  ExpenseStatus,
  ExpenseSummary,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface MembershipContextRow {
  readonly id: string;
  readonly role: HouseholdSummary['role'];
  readonly default_currency: string;
}

interface ActiveMembershipRow {
  readonly id: string;
  readonly user_id: string;
  readonly display_name: string;
}

interface ExpenseBaseRow {
  readonly id: string;
  readonly household_id: string;
  readonly created_by_membership_id: string;
  readonly created_by_user_id: string;
  readonly title: string;
  readonly category: ExpenseSummary['category'];
  readonly amount_minor: number | string | bigint;
  readonly currency: string;
  readonly due_date: Date | string;
  readonly notes: string | null;
  readonly split_method: 'equal';
  readonly status: ExpenseStatus;
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
}

interface ExpenseJoinRow extends ExpenseBaseRow {
  readonly allocation_membership_id: string;
  readonly allocation_user_id: string;
  readonly allocation_display_name: string;
  readonly allocation_amount_minor: number | string | bigint;
  readonly rounding_adjustment_minor: number;
  readonly allocation_status: 'outstanding';
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type ExpenseListResult =
  | { readonly status: 'found'; readonly expenses: readonly ExpenseSummary[] }
  | { readonly status: 'not_found' };

export type ExpenseGetResult =
  | { readonly status: 'found'; readonly expense: ExpenseSummary }
  | { readonly status: 'not_found' };

export type ExpenseCreateResult =
  | { readonly status: 'created' | 'replayed'; readonly expense: ExpenseSummary }
  | {
      readonly status:
        | 'not_found'
        | 'forbidden'
        | 'currency_mismatch'
        | 'idempotency_conflict';
    };

export type ExpenseTransitionResult =
  | { readonly status: 'updated'; readonly expense: ExpenseSummary }
  | {
      readonly status: 'not_found' | 'forbidden' | 'version_conflict' | 'status_conflict';
    };

@Injectable()
export class ExpensesRepository {
  constructor(private readonly database: DatabaseService) {}

  async list(input: { readonly userId: string; readonly householdId: string }): Promise<ExpenseListResult> {
    const membership = await findMembershipContext(this.database, input.userId, input.householdId);
    if (membership === null) return { status: 'not_found' };
    const rows = await selectExpenseRows(this.database, input.householdId);
    return {
      status: 'found',
      expenses: mapExpenseRows(rows, membership, input.userId),
    };
  }

  async get(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly expenseId: string;
  }): Promise<ExpenseGetResult> {
    const membership = await findMembershipContext(this.database, input.userId, input.householdId);
    if (membership === null) return { status: 'not_found' };
    const rows = await selectExpenseRows(this.database, input.householdId, input.expenseId);
    const expense = mapExpenseRows(rows, membership, input.userId)[0];
    return expense === undefined ? { status: 'not_found' } : { status: 'found', expense };
  }

  async create(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly configuration: ExpenseConfiguration;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<ExpenseCreateResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembershipContext(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (membership.role === 'read_only') return { status: 'forbidden' };
      if (membership.default_currency !== input.configuration.amount.currency) {
        return { status: 'currency_mismatch' };
      }

      const activeMemberships = await transaction.query<ActiveMembershipRow>(
        `SELECT m.id, m.user_id, p.display_name
         FROM household_memberships m
         JOIN users u ON u.id = m.user_id AND u.status = 'active'
         JOIN user_profiles p ON p.user_id = m.user_id
         WHERE m.household_id = $1 AND m.status = 'active'
         ORDER BY m.id`,
        [input.householdId],
      );
      if (activeMemberships.length === 0) return { status: 'not_found' };

      const expenseId = newUuidV7(Date.parse(input.occurredAt));
      const status: ExpenseStatus = membership.role === 'member' ? 'proposed' : 'approved';
      const allocations = equalAllocations(
        input.configuration.amount.minorUnits,
        input.configuration.amount.currency,
        activeMemberships,
        input.userId,
      );
      const expense: ExpenseSummary = {
        id: expenseId,
        householdId: input.householdId,
        title: input.configuration.title,
        category: input.configuration.category,
        amount: input.configuration.amount,
        dueDate: input.configuration.dueDate,
        notes: input.configuration.notes ?? null,
        splitMethod: 'equal',
        status,
        allocations,
        currentUserShare: allocations.find((allocation) => allocation.isCurrentUser)?.amount ?? {
          minorUnits: 0,
          currency: input.configuration.amount.currency,
        },
        createdByUserId: input.userId,
        canApprove: status === 'proposed' && isManager(membership.role),
        canReverse: true,
        version: 1,
        createdAt: input.occurredAt,
        updatedAt: input.occurredAt,
      };

      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (
           user_id, operation, idempotency_key, request_hash, response_status, response_body, created_at
         ) VALUES ($1, 'expenses.create', $2, $3, 201, $4::jsonb, $5)
         ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
         RETURNING idempotency_key`,
        [input.userId, input.idempotencyKey, input.requestHash, JSON.stringify(expense), input.occurredAt],
      );
      if (claimed.length === 0) {
        const existingRows = await transaction.query<IdempotencyRow>(
          `SELECT request_hash, response_body FROM idempotency_records
           WHERE user_id = $1 AND operation = 'expenses.create' AND idempotency_key = $2`,
          [input.userId, input.idempotencyKey],
        );
        const existing = existingRows[0];
        if (existing?.request_hash !== input.requestHash) return { status: 'idempotency_conflict' };
        return { status: 'replayed', expense: readExpenseResponse(existing.response_body) };
      }

      await transaction.query(
        `INSERT INTO expenses (
           id, household_id, created_by_membership_id, title, category, amount_minor, currency,
           due_date, notes, split_method, status, version, created_at, updated_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'equal', $10, 1, $11, $11)`,
        [
          expense.id,
          expense.householdId,
          membership.id,
          expense.title,
          expense.category,
          expense.amount.minorUnits,
          expense.amount.currency,
          expense.dueDate,
          expense.notes,
          expense.status,
          input.occurredAt,
        ],
      );
      for (const allocation of allocations) {
        await transaction.query(
          `INSERT INTO expense_allocations (
             id, expense_id, membership_id, amount_minor, rounding_adjustment_minor, status, created_at
           ) VALUES ($1, $2, $3, $4, $5, 'outstanding', $6)`,
          [
            newUuidV7(),
            expense.id,
            allocation.membershipId,
            allocation.amount.minorUnits,
            allocation.roundingAdjustmentMinor,
            input.occurredAt,
          ],
        );
      }
      await transaction.query(
        `INSERT INTO expense_status_events (
           id, expense_id, actor_membership_id, previous_status, next_status, occurred_at
         ) VALUES ($1, $2, $3, NULL, $4, $5)`,
        [newUuidV7(), expense.id, membership.id, expense.status, input.occurredAt],
      );
      await writeExpenseEvidence(transaction, input.userId, expense, 'ledger.expense_created.v1', input.occurredAt);
      return { status: 'created', expense };
    });
  }

  async transition(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly expenseId: string;
    readonly expectedVersion: number;
    readonly action: 'approve' | 'reverse';
    readonly reason: string | null;
    readonly occurredAt: string;
  }): Promise<ExpenseTransitionResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembershipContext(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      const currentRows = await transaction.query<ExpenseBaseRow>(
        `${expenseBaseSelect()}
         WHERE e.id = $1 AND e.household_id = $2
         LIMIT 1 FOR UPDATE`,
        [input.expenseId, input.householdId],
      );
      const current = currentRows[0];
      if (current === undefined) return { status: 'not_found' };
      if (current.version !== input.expectedVersion) return { status: 'version_conflict' };

      const ownsProposal =
        current.created_by_membership_id === membership.id && current.status === 'proposed';
      if (input.action === 'approve') {
        if (!isManager(membership.role)) return { status: 'forbidden' };
        if (current.status !== 'proposed') return { status: 'status_conflict' };
      } else {
        if (!isManager(membership.role) && !ownsProposal) return { status: 'forbidden' };
        if (current.status === 'reversed') return { status: 'status_conflict' };
      }

      const nextStatus: ExpenseStatus = input.action === 'approve' ? 'approved' : 'reversed';
      const updatedRows = await transaction.query<ExpenseBaseRow>(
        `UPDATE expenses SET status = $4, version = version + 1, updated_at = $5
         WHERE id = $1 AND household_id = $2 AND version = $3
         RETURNING id, household_id, created_by_membership_id,
           (SELECT user_id FROM household_memberships WHERE id = created_by_membership_id) AS created_by_user_id,
           title, category, amount_minor, currency, due_date, notes, split_method, status,
           version, created_at, updated_at`,
        [input.expenseId, input.householdId, input.expectedVersion, nextStatus, input.occurredAt],
      );
      if (updatedRows[0] === undefined) return { status: 'version_conflict' };
      await transaction.query(
        `INSERT INTO expense_status_events (
           id, expense_id, actor_membership_id, previous_status, next_status, reason, occurred_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [
          newUuidV7(),
          input.expenseId,
          membership.id,
          current.status,
          nextStatus,
          input.reason,
          input.occurredAt,
        ],
      );
      const rows = await selectExpenseRows(transaction, input.householdId, input.expenseId);
      const expense = mapExpenseRows(rows, membership, input.userId)[0];
      if (expense === undefined) return { status: 'not_found' };
      await writeExpenseEvidence(
        transaction,
        input.userId,
        expense,
        input.action === 'approve' ? 'ledger.expense_approved.v1' : 'ledger.expense_reversed.v1',
        input.occurredAt,
      );
      return { status: 'updated', expense };
    });
  }
}

function equalAllocations(
  totalMinor: number,
  currency: string,
  members: readonly ActiveMembershipRow[],
  currentUserId: string,
): readonly ExpenseAllocationSummary[] {
  const base = Math.floor(totalMinor / members.length);
  const remainder = totalMinor % members.length;
  const allocations = members.map((member, index) => ({
    membershipId: member.id,
    displayName: member.display_name,
    amount: { minorUnits: base + (index < remainder ? 1 : 0), currency },
    roundingAdjustmentMinor: index < remainder ? 1 : 0,
    status: 'outstanding' as const,
    isCurrentUser: member.user_id === currentUserId,
  }));
  const allocated = allocations.reduce((sum, allocation) => sum + allocation.amount.minorUnits, 0);
  if (allocated !== totalMinor) throw new Error('Equal split did not reconcile to the expense total.');
  return allocations;
}

async function findMembershipContext(
  executor: SqlExecutor,
  userId: string,
  householdId: string,
): Promise<MembershipContextRow | null> {
  const rows = await executor.query<MembershipContextRow>(
    `SELECT m.id, m.role, h.default_currency
     FROM household_memberships m
     JOIN households h ON h.id = m.household_id AND h.status = 'active'
     WHERE m.user_id = $1 AND m.household_id = $2 AND m.status = 'active'
     LIMIT 1`,
    [userId, householdId],
  );
  return rows[0] ?? null;
}

function expenseBaseSelect(): string {
  return `SELECT e.id, e.household_id, e.created_by_membership_id,
    creator.user_id AS created_by_user_id, e.title, e.category, e.amount_minor,
    e.currency, e.due_date, e.notes, e.split_method, e.status, e.version,
    e.created_at, e.updated_at
    FROM expenses e
    JOIN household_memberships creator ON creator.id = e.created_by_membership_id`;
}

async function selectExpenseRows(
  executor: SqlExecutor,
  householdId: string,
  expenseId?: string,
): Promise<readonly ExpenseJoinRow[]> {
  return executor.query<ExpenseJoinRow>(
    `SELECT e.id, e.household_id, e.created_by_membership_id,
       creator.user_id AS created_by_user_id, e.title, e.category, e.amount_minor,
       e.currency, e.due_date, e.notes, e.split_method, e.status, e.version,
       e.created_at, e.updated_at,
       a.membership_id AS allocation_membership_id,
       allocated.user_id AS allocation_user_id,
       profile.display_name AS allocation_display_name,
       a.amount_minor AS allocation_amount_minor,
       a.rounding_adjustment_minor,
       a.status AS allocation_status
     FROM expenses e
     JOIN household_memberships creator ON creator.id = e.created_by_membership_id
     JOIN expense_allocations a ON a.expense_id = e.id
     JOIN household_memberships allocated ON allocated.id = a.membership_id
     JOIN user_profiles profile ON profile.user_id = allocated.user_id
     WHERE e.household_id = $1 ${expenseId === undefined ? '' : 'AND e.id = $2'}
     ORDER BY e.due_date, e.created_at DESC, e.id, a.membership_id`,
    expenseId === undefined ? [householdId] : [householdId, expenseId],
  );
}

function mapExpenseRows(
  rows: readonly ExpenseJoinRow[],
  membership: MembershipContextRow,
  currentUserId: string,
): readonly ExpenseSummary[] {
  const grouped = new Map<string, ExpenseJoinRow[]>();
  for (const row of rows) grouped.set(row.id, [...(grouped.get(row.id) ?? []), row]);
  return [...grouped.values()].map((expenseRows) => {
    const row = expenseRows[0];
    if (row === undefined) throw new Error('Expense row group is empty.');
    const allocations = expenseRows.map((allocation): ExpenseAllocationSummary => ({
      membershipId: allocation.allocation_membership_id,
      displayName: allocation.allocation_display_name,
      amount: {
        minorUnits: toSafeNumber(allocation.allocation_amount_minor),
        currency: row.currency,
      },
      roundingAdjustmentMinor: allocation.rounding_adjustment_minor,
      status: allocation.allocation_status,
      isCurrentUser: allocation.allocation_user_id === currentUserId,
    }));
    const ownsProposal = row.created_by_membership_id === membership.id && row.status === 'proposed';
    return {
      id: row.id,
      householdId: row.household_id,
      title: row.title,
      category: row.category,
      amount: { minorUnits: toSafeNumber(row.amount_minor), currency: row.currency },
      dueDate: toLocalDate(row.due_date),
      notes: row.notes,
      splitMethod: row.split_method,
      status: row.status,
      allocations,
      currentUserShare: allocations.find((allocation) => allocation.isCurrentUser)?.amount ?? {
        minorUnits: 0,
        currency: row.currency,
      },
      createdByUserId: row.created_by_user_id,
      canApprove: row.status === 'proposed' && isManager(membership.role),
      canReverse: row.status !== 'reversed' && (isManager(membership.role) || ownsProposal),
      version: row.version,
      createdAt: toInstant(row.created_at),
      updatedAt: toInstant(row.updated_at),
    };
  });
}

function isManager(role: HouseholdSummary['role']): boolean {
  return role === 'owner' || role === 'admin';
}

function toSafeNumber(value: number | string | bigint): number {
  const result = Number(value);
  if (!Number.isSafeInteger(result)) throw new Error('Stored money exceeds the supported JSON range.');
  return result;
}

function toLocalDate(value: Date | string): string {
  return typeof value === 'string' ? value.slice(0, 10) : value.toISOString().slice(0, 10);
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function readExpenseResponse(value: unknown): ExpenseSummary {
  return (typeof value === 'string' ? JSON.parse(value) : value) as ExpenseSummary;
}

async function writeExpenseEvidence(
  transaction: SqlExecutor,
  userId: string,
  expense: ExpenseSummary,
  eventType: string,
  occurredAt: string,
): Promise<void> {
  const action = eventType.replace('.v1', '');
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, occurred_at
     ) VALUES ($1, $2, $3, $4, 'expense', $5, 'success', $6)`,
    [newUuidV7(), userId, expense.householdId, action, expense.id, occurredAt],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id, payload, occurred_at
     ) VALUES ($1, $2, 'expense', $3, $4, $5, $6::jsonb, $7)`,
    [newUuidV7(), eventType, expense.id, expense.householdId, userId, JSON.stringify(expense), occurredAt],
  );
}
