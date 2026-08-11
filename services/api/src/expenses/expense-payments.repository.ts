import { Injectable } from '@nestjs/common';
import type {
  ExpensePaymentDeclarationRequest,
  ExpensePaymentStatus,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface MembershipRow {
  readonly id: string;
  readonly role: HouseholdSummary['role'];
}

interface AllocationRow {
  readonly allocation_id: string;
  readonly amount_minor: number | string | bigint;
  readonly currency: string;
  readonly expense_status: 'proposed' | 'approved' | 'reversed';
}

interface PaymentRow {
  readonly id: string;
  readonly status: ExpensePaymentStatus;
  readonly version: number;
  readonly declared_by_membership_id: string;
  readonly created_by_membership_id: string;
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type DeclarePaymentResult =
  | { readonly status: 'created' | 'replayed'; readonly paymentVersion: number }
  | {
      readonly status: 'not_found' | 'forbidden' | 'status_conflict' | 'idempotency_conflict';
    };

export type PaymentAction = 'confirm' | 'dispute' | 'reverse';

export type PaymentTransitionResult =
  | { readonly status: 'updated'; readonly paymentVersion: number }
  | {
      readonly status: 'not_found' | 'forbidden' | 'status_conflict' | 'version_conflict';
    };

@Injectable()
export class ExpensePaymentsRepository {
  constructor(private readonly database: DatabaseService) {}

  async declare(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly expenseId: string;
    readonly configuration: ExpensePaymentDeclarationRequest;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<DeclarePaymentResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (membership.role === 'read_only') return { status: 'forbidden' };

      const existingIdempotency = await findIdempotency(
        transaction,
        input.userId,
        input.idempotencyKey,
      );
      if (existingIdempotency !== null) {
        if (existingIdempotency.request_hash !== input.requestHash) {
          return { status: 'idempotency_conflict' };
        }
        const paymentId = readPaymentId(existingIdempotency.response_body);
        const payment = await findPaymentVersion(
          transaction,
          paymentId,
          input.expenseId,
          input.householdId,
        );
        return payment === null
          ? { status: 'not_found' }
          : { status: 'replayed', paymentVersion: payment };
      }

      const allocations = await transaction.query<AllocationRow>(
        `SELECT a.id AS allocation_id, a.amount_minor, e.currency, e.status AS expense_status
         FROM expenses e
         JOIN expense_allocations a ON a.expense_id = e.id
         JOIN expense_allocation_members eligible
           ON eligible.allocation_id = a.id AND eligible.membership_id = $3
         WHERE e.id = $1 AND e.household_id = $2
         LIMIT 1 FOR UPDATE`,
        [input.expenseId, input.householdId, membership.id],
      );
      const allocation = allocations[0];
      if (allocation === undefined) return { status: 'not_found' };
      if (allocation.expense_status !== 'approved' || toSafeNumber(allocation.amount_minor) <= 0) {
        return { status: 'status_conflict' };
      }

      const active = await transaction.query<{ readonly id: string }>(
        `SELECT id FROM expense_payment_declarations
         WHERE allocation_id = $1 AND status <> 'reversed'
         LIMIT 1`,
        [allocation.allocation_id],
      );
      if (active.length > 0) return { status: 'status_conflict' };

      const paymentId = newUuidV7(Date.parse(input.occurredAt));
      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (
           user_id, operation, idempotency_key, request_hash, response_status, response_body, created_at
         ) VALUES ($1, 'expense_payments.declare', $2, $3, 201, $4::jsonb, $5)
         ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
         RETURNING idempotency_key`,
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify({ paymentId }),
          input.occurredAt,
        ],
      );
      if (claimed.length === 0) {
        const raced = await findIdempotency(transaction, input.userId, input.idempotencyKey);
        if (raced?.request_hash !== input.requestHash) return { status: 'idempotency_conflict' };
        const racedId = readPaymentId(raced.response_body);
        const paymentVersion = await findPaymentVersion(
          transaction,
          racedId,
          input.expenseId,
          input.householdId,
        );
        return paymentVersion === null
          ? { status: 'not_found' }
          : { status: 'replayed', paymentVersion };
      }

      await transaction.query(
        `INSERT INTO expense_payment_declarations (
           id, household_id, expense_id, allocation_id, declared_by_membership_id,
           amount_minor, currency, method, payment_reference, note, paid_at,
           status, version, created_at, updated_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, 'declared', 1, $12, $12)`,
        [
          paymentId,
          input.householdId,
          input.expenseId,
          allocation.allocation_id,
          membership.id,
          toSafeNumber(allocation.amount_minor),
          allocation.currency,
          input.configuration.method,
          input.configuration.reference ?? null,
          input.configuration.note ?? null,
          input.configuration.paidAt,
          input.occurredAt,
        ],
      );
      await writeStatusEvent(
        transaction,
        paymentId,
        membership.id,
        null,
        'declared',
        null,
        input.occurredAt,
      );
      await writePaymentEvidence(
        transaction,
        input.userId,
        input.householdId,
        input.expenseId,
        paymentId,
        'ledger.payment_declared.v1',
        'declared',
        input.occurredAt,
      );
      return { status: 'created', paymentVersion: 1 };
    });
  }

  async transition(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly expenseId: string;
    readonly paymentId: string;
    readonly expectedVersion: number;
    readonly action: PaymentAction;
    readonly reason: string | null;
    readonly occurredAt: string;
  }): Promise<PaymentTransitionResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (membership.role === 'read_only') return { status: 'forbidden' };

      const rows = await transaction.query<PaymentRow>(
        `SELECT p.id, p.status, p.version, p.declared_by_membership_id,
           e.created_by_membership_id
         FROM expense_payment_declarations p
         JOIN expenses e ON e.id = p.expense_id AND e.household_id = p.household_id
         WHERE p.id = $1 AND p.expense_id = $2 AND p.household_id = $3
         LIMIT 1 FOR UPDATE`,
        [input.paymentId, input.expenseId, input.householdId],
      );
      const payment = rows[0];
      if (payment === undefined) return { status: 'not_found' };
      if (payment.version !== input.expectedVersion) return { status: 'version_conflict' };

      const isDeclarer = payment.declared_by_membership_id === membership.id;
      const canReview = !isDeclarer;
      const canReverse =
        isDeclarer ||
        isManager(membership.role) ||
        payment.created_by_membership_id === membership.id;

      if ((input.action === 'confirm' || input.action === 'dispute') && !canReview) {
        return { status: 'forbidden' };
      }
      if (input.action === 'reverse' && !canReverse) return { status: 'forbidden' };
      if (input.action === 'confirm' && payment.status !== 'declared') {
        return { status: 'status_conflict' };
      }
      if (
        input.action === 'dispute' &&
        payment.status !== 'declared' &&
        payment.status !== 'confirmed'
      ) {
        return { status: 'status_conflict' };
      }
      if (input.action === 'reverse' && payment.status === 'reversed') {
        return { status: 'status_conflict' };
      }

      const nextStatus: ExpensePaymentStatus =
        input.action === 'confirm'
          ? 'confirmed'
          : input.action === 'dispute'
            ? 'disputed'
            : 'reversed';
      const updated = await updatePaymentStatus(
        transaction,
        input.paymentId,
        input.expectedVersion,
        membership.id,
        input.action,
        input.reason,
        input.occurredAt,
      );
      if (!updated) return { status: 'version_conflict' };
      await writeStatusEvent(
        transaction,
        input.paymentId,
        membership.id,
        payment.status,
        nextStatus,
        input.reason,
        input.occurredAt,
      );
      await writePaymentEvidence(
        transaction,
        input.userId,
        input.householdId,
        input.expenseId,
        input.paymentId,
        `ledger.payment_${input.action === 'confirm' ? 'confirmed' : input.action === 'dispute' ? 'disputed' : 'reversed'}.v1`,
        nextStatus,
        input.occurredAt,
      );
      return { status: 'updated', paymentVersion: input.expectedVersion + 1 };
    });
  }
}

async function findMembership(
  executor: SqlExecutor,
  userId: string,
  householdId: string,
): Promise<MembershipRow | null> {
  const rows = await executor.query<MembershipRow>(
    `SELECT m.id, m.role
     FROM household_memberships m
     JOIN households h ON h.id = m.household_id AND h.status = 'active'
     WHERE m.user_id = $1 AND m.household_id = $2 AND m.status = 'active'
     LIMIT 1`,
    [userId, householdId],
  );
  return rows[0] ?? null;
}

async function findIdempotency(
  executor: SqlExecutor,
  userId: string,
  idempotencyKey: string,
): Promise<IdempotencyRow | null> {
  const rows = await executor.query<IdempotencyRow>(
    `SELECT request_hash, response_body FROM idempotency_records
     WHERE user_id = $1 AND operation = 'expense_payments.declare' AND idempotency_key = $2`,
    [userId, idempotencyKey],
  );
  return rows[0] ?? null;
}

async function findPaymentVersion(
  executor: SqlExecutor,
  paymentId: string,
  expenseId: string,
  householdId: string,
): Promise<number | null> {
  const rows = await executor.query<{ readonly version: number }>(
    `SELECT version FROM expense_payment_declarations
     WHERE id = $1 AND expense_id = $2 AND household_id = $3`,
    [paymentId, expenseId, householdId],
  );
  return rows[0]?.version ?? null;
}

async function updatePaymentStatus(
  transaction: SqlExecutor,
  paymentId: string,
  expectedVersion: number,
  actorMembershipId: string,
  action: PaymentAction,
  reason: string | null,
  occurredAt: string,
): Promise<boolean> {
  let rows: readonly { readonly version: number }[];
  if (action === 'confirm') {
    rows = await transaction.query<{ readonly version: number }>(
      `UPDATE expense_payment_declarations
           SET status = 'confirmed', confirmed_by_membership_id = $3, confirmed_at = $4,
             version = version + 1, updated_at = $4
           WHERE id = $1 AND version = $2
           RETURNING version`,
      [paymentId, expectedVersion, actorMembershipId, occurredAt],
    );
  } else if (action === 'dispute') {
    rows = await transaction.query<{ readonly version: number }>(
      `UPDATE expense_payment_declarations
           SET status = 'disputed', disputed_by_membership_id = $3, disputed_at = $5,
             dispute_reason = $4, version = version + 1, updated_at = $5
           WHERE id = $1 AND version = $2
           RETURNING version`,
      [paymentId, expectedVersion, actorMembershipId, reason, occurredAt],
    );
  } else {
    rows = await transaction.query<{ readonly version: number }>(
      `UPDATE expense_payment_declarations
       SET status = 'reversed', reversed_by_membership_id = $3, reversed_at = $5,
         reversal_reason = $4, version = version + 1, updated_at = $5
       WHERE id = $1 AND version = $2
       RETURNING version`,
      [paymentId, expectedVersion, actorMembershipId, reason, occurredAt],
    );
  }
  return rows.length === 1;
}

async function writeStatusEvent(
  transaction: SqlExecutor,
  paymentId: string,
  actorMembershipId: string,
  previousStatus: ExpensePaymentStatus | null,
  nextStatus: ExpensePaymentStatus,
  reason: string | null,
  occurredAt: string,
): Promise<void> {
  await transaction.query(
    `INSERT INTO expense_payment_status_events (
       id, payment_id, actor_membership_id, previous_status, next_status, reason, occurred_at
     ) VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [newUuidV7(), paymentId, actorMembershipId, previousStatus, nextStatus, reason, occurredAt],
  );
}

async function writePaymentEvidence(
  transaction: SqlExecutor,
  userId: string,
  householdId: string,
  expenseId: string,
  paymentId: string,
  eventType: string,
  status: ExpensePaymentStatus,
  occurredAt: string,
): Promise<void> {
  const action = eventType.replace('.v1', '');
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, occurred_at
     ) VALUES ($1, $2, $3, $4, 'expense_payment', $5, 'success', $6)`,
    [newUuidV7(), userId, householdId, action, paymentId, occurredAt],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id, payload, occurred_at
     ) VALUES ($1, $2, 'expense_payment', $3, $4, $5, $6::jsonb, $7)`,
    [
      newUuidV7(),
      eventType,
      paymentId,
      householdId,
      userId,
      JSON.stringify({ paymentId, expenseId, householdId, status }),
      occurredAt,
    ],
  );
}

function readPaymentId(value: unknown): string {
  const parsed = (typeof value === 'string' ? JSON.parse(value) : value) as {
    readonly paymentId?: unknown;
  } | null;
  if (parsed === null || typeof parsed.paymentId !== 'string') {
    throw new Error('Stored payment idempotency response is invalid.');
  }
  return parsed.paymentId;
}

function isManager(role: HouseholdSummary['role']): boolean {
  return role === 'owner' || role === 'admin';
}

function toSafeNumber(value: number | string | bigint): number {
  const result = Number(value);
  if (!Number.isSafeInteger(result)) throw new Error('Stored money exceeds the JSON range.');
  return result;
}
