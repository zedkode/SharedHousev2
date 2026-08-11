import { Injectable } from '@nestjs/common';
import type {
  ExpenseAllocationSummary,
  ExpenseConfiguration,
  ExpensePaymentSummary,
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

interface ActiveCoupleRow {
  readonly id: string;
  readonly primary_membership_id: string;
  readonly primary_user_id: string;
  readonly primary_display_name: string;
  readonly partner_membership_id: string | null;
  readonly partner_user_id: string | null;
  readonly partner_display_name: string;
}

interface PendingAllocation {
  readonly response: ExpenseAllocationSummary;
  readonly billingCoupleId: string | null;
}

interface BillingUnit {
  readonly membershipId: string;
  readonly displayName: string;
  readonly billingUnitType: 'individual' | 'couple';
  readonly participantCount: 1 | 2;
  readonly billingCoupleId: string | null;
  readonly eligibleMembershipIds: readonly string[];
  readonly eligibleUserIds: readonly string[];
}

interface ExpenseBaseRow {
  readonly id: string;
  readonly household_id: string;
  readonly created_by_membership_id: string;
  readonly created_by_user_id: string;
  readonly title: string;
  readonly category: ExpenseSummary['category'];
  readonly custom_category_name: string | null;
  readonly amount_minor: number | string | bigint;
  readonly currency: string;
  readonly due_date: Date | string;
  readonly notes: string | null;
  readonly source_template_id: string | null;
  readonly occurrence_date: Date | string | null;
  readonly split_method: 'equal';
  readonly status: ExpenseStatus;
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
}

interface ExpenseJoinRow extends ExpenseBaseRow {
  readonly allocation_id: string;
  readonly allocation_membership_id: string;
  readonly allocation_display_name: string;
  readonly allocation_billing_unit_type: 'individual' | 'couple';
  readonly allocation_participant_count: 1 | 2;
  readonly allocation_eligible_membership_ids: readonly string[];
  readonly allocation_eligible_user_ids: readonly string[];
  readonly allocation_amount_minor: number | string | bigint;
  readonly rounding_adjustment_minor: number;
  readonly allocation_status: 'outstanding';
  readonly payment_id: string | null;
  readonly payment_amount_minor: number | string | bigint | null;
  readonly payment_method: ExpensePaymentSummary['method'] | null;
  readonly payment_reference: string | null;
  readonly payment_note: string | null;
  readonly payment_paid_at: Date | string | null;
  readonly payment_status: ExpensePaymentSummary['status'] | null;
  readonly payment_declared_by_user_id: string | null;
  readonly payment_confirmed_by_user_id: string | null;
  readonly payment_confirmed_at: Date | string | null;
  readonly payment_disputed_by_user_id: string | null;
  readonly payment_disputed_at: Date | string | null;
  readonly payment_dispute_reason: string | null;
  readonly payment_reversed_by_user_id: string | null;
  readonly payment_reversed_at: Date | string | null;
  readonly payment_reversal_reason: string | null;
  readonly payment_version: number | null;
  readonly payment_created_at: Date | string | null;
  readonly payment_updated_at: Date | string | null;
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type ExpenseListResult =
  | { readonly status: 'found'; readonly expenses: readonly ExpenseSummary[] }
  | { readonly status: 'not_found' };

export type ExpenseGetResult =
  { readonly status: 'found'; readonly expense: ExpenseSummary } | { readonly status: 'not_found' };

export type ExpenseCreateResult =
  | { readonly status: 'created' | 'replayed'; readonly expense: ExpenseSummary }
  | {
      readonly status: 'not_found' | 'forbidden' | 'currency_mismatch' | 'idempotency_conflict';
    };

export type ExpenseTransitionResult =
  | { readonly status: 'updated'; readonly expense: ExpenseSummary }
  | {
      readonly status:
        'not_found' | 'forbidden' | 'version_conflict' | 'status_conflict' | 'payment_conflict';
    };

@Injectable()
export class ExpensesRepository {
  constructor(private readonly database: DatabaseService) {}

  async list(input: {
    readonly userId: string;
    readonly householdId: string;
  }): Promise<ExpenseListResult> {
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

      const activeCouples = await transaction.query<ActiveCoupleRow>(
        `SELECT couple.id, couple.primary_membership_id,
           primary_member.user_id AS primary_user_id,
           primary_profile.display_name AS primary_display_name,
           couple.partner_membership_id,
           partner_member.user_id AS partner_user_id,
           COALESCE(partner_profile.display_name, couple.partner_display_name) AS partner_display_name
         FROM household_billing_couples couple
         JOIN household_memberships primary_member ON primary_member.id = couple.primary_membership_id
         JOIN user_profiles primary_profile ON primary_profile.user_id = primary_member.user_id
         LEFT JOIN household_memberships partner_member ON partner_member.id = couple.partner_membership_id
         LEFT JOIN user_profiles partner_profile ON partner_profile.user_id = partner_member.user_id
         WHERE couple.household_id = $1 AND couple.status = 'active'
         ORDER BY couple.primary_membership_id, couple.id`,
        [input.householdId],
      );

      const expenseId = newUuidV7(Date.parse(input.occurredAt));
      const status: ExpenseStatus = membership.role === 'member' ? 'proposed' : 'approved';
      const pendingAllocations = equalAllocations(
        input.configuration.amount.minorUnits,
        input.configuration.amount.currency,
        activeMemberships,
        activeCouples,
        input.userId,
        status === 'approved',
      );
      const allocations = pendingAllocations.map((allocation) => allocation.response);
      const expense: ExpenseSummary = {
        id: expenseId,
        householdId: input.householdId,
        title: input.configuration.title,
        category: input.configuration.category,
        customCategoryName: input.configuration.customCategoryName ?? null,
        amount: input.configuration.amount,
        dueDate: input.configuration.dueDate,
        notes: input.configuration.notes ?? null,
        sourceTemplateId: null,
        occurrenceDate: null,
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
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify(expense),
          input.occurredAt,
        ],
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
           id, household_id, created_by_membership_id, title, category, custom_category_name, amount_minor, currency,
           due_date, notes, split_method, status, version, created_at, updated_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'equal', $11, 1, $12, $12)`,
        [
          expense.id,
          expense.householdId,
          membership.id,
          expense.title,
          expense.category,
          expense.customCategoryName,
          expense.amount.minorUnits,
          expense.amount.currency,
          expense.dueDate,
          expense.notes,
          expense.status,
          input.occurredAt,
        ],
      );
      for (const pending of pendingAllocations) {
        const allocation = pending.response;
        const allocationId = newUuidV7();
        await transaction.query(
          `INSERT INTO expense_allocations (
             id, expense_id, membership_id, amount_minor, rounding_adjustment_minor, status,
             billing_unit_type, billing_unit_label, participant_count, billing_couple_id, created_at
           ) VALUES ($1, $2, $3, $4, $5, 'outstanding', $6, $7, $8, $9, $10)`,
          [
            allocationId,
            expense.id,
            allocation.membershipId,
            allocation.amount.minorUnits,
            allocation.roundingAdjustmentMinor,
            allocation.billingUnitType,
            allocation.displayName,
            allocation.participantCount,
            pending.billingCoupleId,
            input.occurredAt,
          ],
        );
        for (const eligibleMembershipId of allocation.eligibleMembershipIds) {
          await transaction.query(
            `INSERT INTO expense_allocation_members (allocation_id, membership_id, created_at)
             VALUES ($1, $2, $3)`,
            [allocationId, eligibleMembershipId, input.occurredAt],
          );
        }
      }
      await transaction.query(
        `INSERT INTO expense_status_events (
           id, expense_id, actor_membership_id, previous_status, next_status, occurred_at
         ) VALUES ($1, $2, $3, NULL, $4, $5)`,
        [newUuidV7(), expense.id, membership.id, expense.status, input.occurredAt],
      );
      await writeExpenseEvidence(
        transaction,
        input.userId,
        expense,
        'ledger.expense_created.v1',
        input.occurredAt,
      );
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
        const activePayments = await transaction.query<{ readonly id: string }>(
          `SELECT p.id
           FROM expense_payment_declarations p
           WHERE p.expense_id = $1 AND p.household_id = $2 AND p.status <> 'reversed'
           LIMIT 1`,
          [input.expenseId, input.householdId],
        );
        if (activePayments.length > 0) return { status: 'payment_conflict' };
      }

      const nextStatus: ExpenseStatus = input.action === 'approve' ? 'approved' : 'reversed';
      const updatedRows = await transaction.query<ExpenseBaseRow>(
        `UPDATE expenses SET status = $4, version = version + 1, updated_at = $5
         WHERE id = $1 AND household_id = $2 AND version = $3
         RETURNING id, household_id, created_by_membership_id,
           (SELECT user_id FROM household_memberships WHERE id = created_by_membership_id) AS created_by_user_id,
           title, category, custom_category_name, amount_minor, currency, due_date, notes, split_method, status,
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
  couples: readonly ActiveCoupleRow[],
  currentUserId: string,
  canDeclareCurrentPayment: boolean,
): readonly PendingAllocation[] {
  const memberById = new Map(members.map((member) => [member.id, member]));
  const coupleByPrimary = new Map(couples.map((couple) => [couple.primary_membership_id, couple]));
  const coupledPartners = new Set(
    couples.flatMap((couple) =>
      couple.partner_membership_id === null ? [] : [couple.partner_membership_id],
    ),
  );
  const units: BillingUnit[] = [];
  for (const member of members) {
    if (coupledPartners.has(member.id)) continue;
    const couple = coupleByPrimary.get(member.id);
    if (couple === undefined) {
      units.push({
        membershipId: member.id,
        displayName: member.display_name,
        billingUnitType: 'individual',
        participantCount: 1,
        billingCoupleId: null,
        eligibleMembershipIds: [member.id],
        eligibleUserIds: [member.user_id],
      });
      continue;
    }
    const partner =
      couple.partner_membership_id === null
        ? null
        : (memberById.get(couple.partner_membership_id) ?? null);
    units.push({
      membershipId: member.id,
      displayName: `${couple.primary_display_name} & ${couple.partner_display_name}`,
      billingUnitType: 'couple',
      participantCount: 2,
      billingCoupleId: couple.id,
      eligibleMembershipIds: [member.id, ...(partner === null ? [] : [partner.id])],
      eligibleUserIds: [member.user_id, ...(partner === null ? [] : [partner.user_id])],
    });
  }
  const residentCount = units.reduce((sum, unit) => sum + unit.participantCount, 0);
  const base = Math.floor(totalMinor / residentCount);
  let remaining = totalMinor % residentCount;
  const allocations: PendingAllocation[] = units.map((unit) => {
    const roundingAdjustmentMinor = Math.min(unit.participantCount, remaining);
    remaining -= roundingAdjustmentMinor;
    const amountMinor = base * unit.participantCount + roundingAdjustmentMinor;
    const isCurrentUser = unit.eligibleUserIds.includes(currentUserId);
    return {
      billingCoupleId: unit.billingCoupleId,
      response: {
        membershipId: unit.membershipId,
        displayName: unit.displayName,
        billingUnitType: unit.billingUnitType,
        participantCount: unit.participantCount,
        eligibleMembershipIds: unit.eligibleMembershipIds,
        amount: { minorUnits: amountMinor, currency },
        roundingAdjustmentMinor,
        status: 'outstanding' as const,
        paymentDeclarations: [],
        canDeclarePayment: canDeclareCurrentPayment && isCurrentUser && amountMinor > 0,
        isCurrentUser,
      },
    };
  });
  const allocated = allocations.reduce(
    (sum, allocation) => sum + allocation.response.amount.minorUnits,
    0,
  );
  if (allocated !== totalMinor)
    throw new Error('Equal split did not reconcile to the expense total.');
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
    creator.user_id AS created_by_user_id, e.title, e.category, e.custom_category_name, e.amount_minor,
    e.currency, e.due_date, e.notes, e.source_template_id, e.occurrence_date,
    e.split_method, e.status, e.version,
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
       creator.user_id AS created_by_user_id, e.title, e.category, e.custom_category_name, e.amount_minor,
       e.currency, e.due_date, e.notes, e.source_template_id, e.occurrence_date,
       e.split_method, e.status, e.version,
       e.created_at, e.updated_at,
       a.id AS allocation_id,
       a.membership_id AS allocation_membership_id,
       a.billing_unit_label AS allocation_display_name,
       a.billing_unit_type AS allocation_billing_unit_type,
       a.participant_count AS allocation_participant_count,
       eligible.membership_ids AS allocation_eligible_membership_ids,
       eligible.user_ids AS allocation_eligible_user_ids,
       a.amount_minor AS allocation_amount_minor,
       a.rounding_adjustment_minor,
       a.status AS allocation_status,
       p.id AS payment_id, p.amount_minor AS payment_amount_minor, p.method AS payment_method,
       p.payment_reference, p.note AS payment_note, p.paid_at AS payment_paid_at,
       p.status AS payment_status, declared.user_id AS payment_declared_by_user_id,
       confirmed.user_id AS payment_confirmed_by_user_id, p.confirmed_at AS payment_confirmed_at,
       disputed.user_id AS payment_disputed_by_user_id, p.disputed_at AS payment_disputed_at,
       p.dispute_reason AS payment_dispute_reason,
       reversed.user_id AS payment_reversed_by_user_id, p.reversed_at AS payment_reversed_at,
       p.reversal_reason AS payment_reversal_reason, p.version AS payment_version,
       p.created_at AS payment_created_at, p.updated_at AS payment_updated_at
     FROM expenses e
     JOIN household_memberships creator ON creator.id = e.created_by_membership_id
     JOIN expense_allocations a ON a.expense_id = e.id
     JOIN LATERAL (
       SELECT array_agg(allocation_member.membership_id ORDER BY allocation_member.membership_id) AS membership_ids,
         array_agg(member.user_id ORDER BY allocation_member.membership_id) AS user_ids
       FROM expense_allocation_members allocation_member
       JOIN household_memberships member ON member.id = allocation_member.membership_id
       WHERE allocation_member.allocation_id = a.id
     ) eligible ON true
     LEFT JOIN expense_payment_declarations p ON p.allocation_id = a.id
     LEFT JOIN household_memberships declared ON declared.id = p.declared_by_membership_id
     LEFT JOIN household_memberships confirmed ON confirmed.id = p.confirmed_by_membership_id
     LEFT JOIN household_memberships disputed ON disputed.id = p.disputed_by_membership_id
     LEFT JOIN household_memberships reversed ON reversed.id = p.reversed_by_membership_id
     WHERE e.household_id = $1 ${expenseId === undefined ? '' : 'AND e.id = $2'}
     ORDER BY e.due_date, e.created_at DESC, e.id, a.id, p.created_at, p.id`,
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
    const allocationGroups = new Map<string, ExpenseJoinRow[]>();
    for (const allocationRow of expenseRows) {
      allocationGroups.set(allocationRow.allocation_id, [
        ...(allocationGroups.get(allocationRow.allocation_id) ?? []),
        allocationRow,
      ]);
    }
    const allocations = [...allocationGroups.values()].map(
      (allocationRows): ExpenseAllocationSummary => {
        const allocation = allocationRows[0];
        if (allocation === undefined) throw new Error('Expense allocation row group is empty.');
        const paymentDeclarations = allocationRows.flatMap((paymentRow) => {
          const payment = mapPaymentRow(paymentRow, row, membership, currentUserId);
          return payment === null ? [] : [payment];
        });
        const activePayment = [...paymentDeclarations]
          .reverse()
          .find((payment) => payment.status !== 'reversed');
        const isCurrentUser = allocation.allocation_eligible_user_ids.includes(currentUserId);
        return {
          membershipId: allocation.allocation_membership_id,
          displayName: allocation.allocation_display_name,
          billingUnitType: allocation.allocation_billing_unit_type,
          participantCount: allocation.allocation_participant_count,
          eligibleMembershipIds: allocation.allocation_eligible_membership_ids,
          amount: {
            minorUnits: toSafeNumber(allocation.allocation_amount_minor),
            currency: row.currency,
          },
          roundingAdjustmentMinor: allocation.rounding_adjustment_minor,
          status: paymentAllocationStatus(activePayment?.status),
          paymentDeclarations,
          canDeclarePayment:
            row.status === 'approved' &&
            membership.role !== 'read_only' &&
            isCurrentUser &&
            toSafeNumber(allocation.allocation_amount_minor) > 0 &&
            activePayment === undefined,
          isCurrentUser,
        };
      },
    );
    const ownsProposal =
      row.created_by_membership_id === membership.id && row.status === 'proposed';
    return {
      id: row.id,
      householdId: row.household_id,
      title: row.title,
      category: row.category,
      customCategoryName: row.custom_category_name,
      amount: { minorUnits: toSafeNumber(row.amount_minor), currency: row.currency },
      dueDate: toLocalDate(row.due_date),
      notes: row.notes,
      sourceTemplateId: row.source_template_id,
      occurrenceDate: row.occurrence_date === null ? null : toLocalDate(row.occurrence_date),
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
  if (!Number.isSafeInteger(result))
    throw new Error('Stored money exceeds the supported JSON range.');
  return result;
}

function toLocalDate(value: Date | string): string {
  return typeof value === 'string' ? value.slice(0, 10) : value.toISOString().slice(0, 10);
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function readExpenseResponse(value: unknown): ExpenseSummary {
  type StoredExpenseAllocation = Omit<
    ExpenseAllocationSummary,
    | 'status'
    | 'paymentDeclarations'
    | 'canDeclarePayment'
    | 'billingUnitType'
    | 'participantCount'
    | 'eligibleMembershipIds'
  > & {
    readonly status?: ExpenseAllocationSummary['status'];
    readonly paymentDeclarations?: ExpenseAllocationSummary['paymentDeclarations'];
    readonly canDeclarePayment?: boolean;
    readonly billingUnitType?: ExpenseAllocationSummary['billingUnitType'];
    readonly participantCount?: ExpenseAllocationSummary['participantCount'];
    readonly eligibleMembershipIds?: ExpenseAllocationSummary['eligibleMembershipIds'];
  };
  type StoredExpenseResponse = Omit<
    ExpenseSummary,
    'sourceTemplateId' | 'occurrenceDate' | 'allocations'
  > & {
    readonly sourceTemplateId?: string | null;
    readonly occurrenceDate?: string | null;
    readonly allocations: readonly StoredExpenseAllocation[];
  };
  const stored = (typeof value === 'string' ? JSON.parse(value) : value) as StoredExpenseResponse;
  return {
    ...stored,
    allocations: stored.allocations.map((allocation) => ({
      ...allocation,
      status: allocation.status ?? 'outstanding',
      billingUnitType: allocation.billingUnitType ?? 'individual',
      participantCount: allocation.participantCount ?? 1,
      eligibleMembershipIds: allocation.eligibleMembershipIds ?? [allocation.membershipId],
      paymentDeclarations: allocation.paymentDeclarations ?? [],
      canDeclarePayment:
        allocation.canDeclarePayment ??
        (stored.status === 'approved' &&
          allocation.isCurrentUser &&
          allocation.amount.minorUnits > 0),
    })),
    sourceTemplateId: stored.sourceTemplateId ?? null,
    occurrenceDate: stored.occurrenceDate ?? null,
  };
}

function mapPaymentRow(
  payment: ExpenseJoinRow,
  expense: ExpenseBaseRow,
  membership: MembershipContextRow,
  currentUserId: string,
): ExpensePaymentSummary | null {
  if (
    payment.payment_id === null ||
    payment.payment_amount_minor === null ||
    payment.payment_method === null ||
    payment.payment_paid_at === null ||
    payment.payment_status === null ||
    payment.payment_declared_by_user_id === null ||
    payment.payment_version === null ||
    payment.payment_created_at === null ||
    payment.payment_updated_at === null
  ) {
    return null;
  }
  const isDeclarer = payment.payment_declared_by_user_id === currentUserId;
  const canReview = !isDeclarer && membership.role !== 'read_only';
  const canReverse =
    payment.payment_status !== 'reversed' &&
    (isDeclarer ||
      isManager(membership.role) ||
      expense.created_by_membership_id === membership.id);
  return {
    id: payment.payment_id,
    expenseId: expense.id,
    allocationMembershipId: payment.allocation_membership_id,
    payerDisplayName: payment.allocation_display_name,
    amount: { minorUnits: toSafeNumber(payment.payment_amount_minor), currency: expense.currency },
    method: payment.payment_method,
    reference: payment.payment_reference,
    note: payment.payment_note,
    paidAt: toInstant(payment.payment_paid_at),
    status: payment.payment_status,
    declaredByUserId: payment.payment_declared_by_user_id,
    confirmedByUserId: payment.payment_confirmed_by_user_id,
    confirmedAt:
      payment.payment_confirmed_at === null ? null : toInstant(payment.payment_confirmed_at),
    disputedByUserId: payment.payment_disputed_by_user_id,
    disputedAt:
      payment.payment_disputed_at === null ? null : toInstant(payment.payment_disputed_at),
    disputeReason: payment.payment_dispute_reason,
    reversedByUserId: payment.payment_reversed_by_user_id,
    reversedAt:
      payment.payment_reversed_at === null ? null : toInstant(payment.payment_reversed_at),
    reversalReason: payment.payment_reversal_reason,
    canConfirm: payment.payment_status === 'declared' && canReview,
    canDispute:
      (payment.payment_status === 'declared' || payment.payment_status === 'confirmed') &&
      canReview,
    canReverse,
    version: payment.payment_version,
    createdAt: toInstant(payment.payment_created_at),
    updatedAt: toInstant(payment.payment_updated_at),
  };
}

function paymentAllocationStatus(
  status: ExpensePaymentSummary['status'] | undefined,
): ExpenseAllocationSummary['status'] {
  if (status === 'confirmed') return 'paid';
  if (status === 'declared' || status === 'disputed') return status;
  return 'outstanding';
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
    [
      newUuidV7(),
      eventType,
      expense.id,
      expense.householdId,
      userId,
      JSON.stringify(expense),
      occurredAt,
    ],
  );
}
