import { Injectable } from '@nestjs/common';
import type {
  HouseholdMemberActionRequest,
  HouseholdMemberBoard,
  HouseholdMemberRole,
  HouseholdMemberSummary,
  HouseholdMembershipStatus,
} from '@sharedhouse/contracts';
import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface MemberRow {
  readonly membership_id: string;
  readonly user_id: string;
  readonly display_name: string;
  readonly role: HouseholdMemberRole;
  readonly status: HouseholdMembershipStatus;
  readonly joined_at: Date | string;
  readonly updated_at: Date | string;
  readonly version: number;
}
interface ReplayRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}
export type MemberMutationResult =
  | { readonly status: 'ok' | 'replayed'; readonly member: HouseholdMemberSummary }
  | {
      readonly status:
        | 'not_found'
        | 'forbidden'
        | 'version_conflict'
        | 'invalid_transition'
        | 'idempotency_conflict';
    };

@Injectable()
export class HouseholdMembersRepository {
  constructor(private readonly database: DatabaseService) {}

  async list(userId: string, householdId: string): Promise<HouseholdMemberBoard | null> {
    const rows = await listMembers(this.database, householdId);
    const actor = rows.find((row) => row.user_id === userId && row.status === 'active');
    if (actor === undefined) return null;
    return {
      canInvite: isManager(actor.role),
      canEditHousehold: isManager(actor.role),
      members: rows
        .filter((row) => isManager(actor.role) || row.status === 'active')
        .map((row) => mapMember(row, actor)),
    };
  }

  async action(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly membershipId: string;
    readonly expectedVersion: number;
    readonly action: HouseholdMemberActionRequest;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<MemberMutationResult> {
    return this.database.transaction(async (tx) => {
      const rows = await tx.query<MemberRow>(
        `${memberSelect()} WHERE m.household_id = $1 ORDER BY m.joined_at, m.id FOR UPDATE OF m`,
        [input.householdId],
      );
      const actor = rows.find((row) => row.user_id === input.userId && row.status === 'active');
      const target = rows.find((row) => row.membership_id === input.membershipId);
      if (actor === undefined || target === undefined) return { status: 'not_found' };

      const replay = await readReplay(tx, input);
      if (replay.status === 'conflict') return { status: 'idempotency_conflict' };
      if (replay.status === 'found') return { status: 'replayed', member: replay.member };
      if (target.version !== input.expectedVersion) return { status: 'version_conflict' };
      if (!isAllowed(actor, target, input.action)) return { status: 'forbidden' };
      if (!isValidTransition(rows, actor, target, input.action))
        return { status: 'invalid_transition' };

      if (input.action.action === 'transfer_ownership') {
        await updateMembership(tx, actor, 'admin', 'active', input.occurredAt);
        await recordHistory(tx, input, actor, 'ownership_transferred_from', 'admin', 'active');
        await updateMembership(tx, target, 'owner', 'active', input.occurredAt);
        await recordHistory(tx, input, target, 'ownership_transferred_to', 'owner', 'active');
      } else {
        const next = nextState(target, input.action);
        await updateMembership(tx, target, next.role, next.status, input.occurredAt);
        await recordHistory(
          tx,
          input,
          target,
          historyAction(input.action.action),
          next.role,
          next.status,
        );
      }

      const updatedRows = await tx.query<MemberRow>(
        `${memberSelect()} WHERE m.household_id = $1 AND m.id = $2 LIMIT 1`,
        [input.householdId, input.membershipId],
      );
      const updated = updatedRows[0];
      if (updated === undefined) throw new Error('Updated membership could not be read.');
      const refreshedActor =
        input.action.action === 'transfer_ownership'
          ? {
              ...actor,
              role: 'admin' as const,
              version: actor.version + 1,
              updated_at: input.occurredAt,
            }
          : actor;
      const member = mapMember(updated, refreshedActor);
      await writeEvidence(tx, input, member);
      await storeReplay(tx, input, member);
      return { status: 'ok', member };
    });
  }
}

function memberSelect(): string {
  return `SELECT m.id AS membership_id, m.user_id, p.display_name, m.role, m.status,
    m.joined_at, m.updated_at, m.version
    FROM household_memberships m
    JOIN households h ON h.id = m.household_id AND h.status = 'active'
    JOIN user_profiles p ON p.user_id = m.user_id`;
}
async function listMembers(
  executor: SqlExecutor,
  householdId: string,
): Promise<readonly MemberRow[]> {
  return executor.query<MemberRow>(
    `${memberSelect()} WHERE m.household_id = $1
     ORDER BY CASE m.status WHEN 'active' THEN 0 WHEN 'suspended' THEN 1 ELSE 2 END,
       CASE m.role WHEN 'owner' THEN 0 WHEN 'admin' THEN 1 WHEN 'member' THEN 2 ELSE 3 END,
       lower(p.display_name), m.joined_at, m.id`,
    [householdId],
  );
}
function mapMember(row: MemberRow, actor: MemberRow): HouseholdMemberSummary {
  const active = row.status === 'active';
  const suspended = row.status === 'suspended';
  const notSelf = row.membership_id !== actor.membership_id;
  const owner = actor.role === 'owner';
  const adminTargetAllowed =
    actor.role === 'admin' && (row.role === 'member' || row.role === 'read_only');
  return {
    membershipId: row.membership_id,
    userId: row.user_id,
    displayName: row.display_name,
    role: row.role,
    status: row.status,
    isCurrentUser: !notSelf,
    canChangeRole: active && notSelf && row.role !== 'owner' && (owner || adminTargetAllowed),
    canSuspend: active && notSelf && row.role !== 'owner' && (owner || adminTargetAllowed),
    canReactivate: suspended && notSelf && (owner || adminTargetAllowed),
    canRemove:
      (active || suspended) && notSelf && row.role !== 'owner' && (owner || adminTargetAllowed),
    canTransferOwnership:
      owner && active && notSelf && (row.role === 'admin' || row.role === 'member'),
    assignableRoles:
      active && notSelf && row.role !== 'owner'
        ? owner
          ? (['admin', 'member', 'read_only'] as const).filter((role) => role !== row.role)
          : adminTargetAllowed
            ? (['member', 'read_only'] as const).filter((role) => role !== row.role)
            : []
        : [],
    joinedAt: instant(row.joined_at),
    updatedAt: instant(row.updated_at),
    version: row.version,
  };
}
function isManager(role: HouseholdMemberRole): boolean {
  return role === 'owner' || role === 'admin';
}
function isAllowed(
  actor: MemberRow,
  target: MemberRow,
  action: HouseholdMemberActionRequest,
): boolean {
  if (!isManager(actor.role) || actor.membership_id === target.membership_id) return false;
  if (action.action === 'transfer_ownership') return actor.role === 'owner';
  if (target.role === 'owner') return false;
  if (actor.role === 'owner') return true;
  if (target.role !== 'member' && target.role !== 'read_only') return false;
  return action.action !== 'change_role' || action.role === 'member' || action.role === 'read_only';
}
function isValidTransition(
  rows: readonly MemberRow[],
  actor: MemberRow,
  target: MemberRow,
  action: HouseholdMemberActionRequest,
): boolean {
  switch (action.action) {
    case 'change_role':
      return target.status === 'active' && action.role != null && action.role !== target.role;
    case 'suspend':
      return target.status === 'active';
    case 'reactivate':
      return (
        target.status === 'suspended' &&
        !rows.some(
          (row) =>
            row.user_id === target.user_id &&
            row.status === 'active' &&
            row.membership_id !== target.membership_id,
        )
      );
    case 'remove':
      return target.status === 'active' || target.status === 'suspended';
    case 'transfer_ownership':
      return (
        actor.role === 'owner' &&
        target.status === 'active' &&
        (target.role === 'admin' || target.role === 'member')
      );
  }
}
function nextState(
  target: MemberRow,
  action: HouseholdMemberActionRequest,
): { role: HouseholdMemberRole; status: HouseholdMembershipStatus } {
  if (action.action === 'change_role')
    return { role: action.role ?? target.role, status: target.status };
  if (action.action === 'suspend') return { role: target.role, status: 'suspended' };
  if (action.action === 'reactivate') return { role: target.role, status: 'active' };
  return { role: target.role, status: 'removed' };
}
function historyAction(
  action: HouseholdMemberActionRequest['action'],
): 'role_changed' | 'suspended' | 'reactivated' | 'removed' {
  if (action === 'change_role') return 'role_changed';
  if (action === 'suspend') return 'suspended';
  if (action === 'reactivate') return 'reactivated';
  return 'removed';
}
async function updateMembership(
  tx: SqlExecutor,
  row: MemberRow,
  role: HouseholdMemberRole,
  status: HouseholdMembershipStatus,
  at: string,
): Promise<void> {
  await tx.query(
    `UPDATE household_memberships SET role=$2, status=$3::varchar, version=version+1, updated_at=$4,
      left_at=CASE WHEN $3::text IN ('left','removed') THEN $4::timestamptz ELSE NULL END WHERE id=$1`,
    [row.membership_id, role, status, at],
  );
}
async function recordHistory(
  tx: SqlExecutor,
  input: {
    userId: string;
    householdId: string;
    action: HouseholdMemberActionRequest;
    occurredAt: string;
  },
  row: MemberRow,
  action: string,
  newRole: HouseholdMemberRole,
  newStatus: HouseholdMembershipStatus,
): Promise<void> {
  await tx.query(
    `INSERT INTO household_membership_history
      (id, household_id, membership_id, actor_user_id, action, previous_role, new_role,
       previous_status, new_status, reason, occurred_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
    [
      newUuidV7(),
      input.householdId,
      row.membership_id,
      input.userId,
      action,
      row.role,
      newRole,
      row.status,
      newStatus,
      input.action.reason ?? null,
      input.occurredAt,
    ],
  );
}
async function writeEvidence(
  tx: SqlExecutor,
  input: {
    userId: string;
    householdId: string;
    membershipId: string;
    action: HouseholdMemberActionRequest;
    occurredAt: string;
  },
  member: HouseholdMemberSummary,
): Promise<void> {
  const eventType = `household.member_${input.action.action}.v1`;
  await tx.query(
    `INSERT INTO audit_events (id,actor_user_id,household_id,action,target_type,target_id,outcome,safe_details,occurred_at)
     VALUES ($1,$2,$3,$4,'household_membership',$5,'success',$6::jsonb,$7)`,
    [
      newUuidV7(),
      input.userId,
      input.householdId,
      eventType,
      input.membershipId,
      JSON.stringify({ role: member.role, status: member.status }),
      input.occurredAt,
    ],
  );
  await tx.query(
    `INSERT INTO outbox_events (id,event_type,aggregate_type,aggregate_id,household_id,actor_user_id,payload,occurred_at)
     VALUES ($1,$2,'household_membership',$3,$4,$5,$6::jsonb,$7)`,
    [
      newUuidV7(),
      eventType,
      input.membershipId,
      input.householdId,
      input.userId,
      JSON.stringify(member),
      input.occurredAt,
    ],
  );
}
async function readReplay(
  tx: SqlExecutor,
  input: { userId: string; idempotencyKey: string; requestHash: string },
): Promise<
  | { status: 'missing' }
  | { status: 'conflict' }
  | { status: 'found'; member: HouseholdMemberSummary }
> {
  const rows = await tx.query<ReplayRow>(
    `SELECT request_hash,response_body FROM idempotency_records
     WHERE user_id=$1 AND operation='household_members.action' AND idempotency_key=$2`,
    [input.userId, input.idempotencyKey],
  );
  const row = rows[0];
  if (row === undefined) return { status: 'missing' };
  if (row.request_hash !== input.requestHash) return { status: 'conflict' };
  return {
    status: 'found',
    member:
      typeof row.response_body === 'string'
        ? (JSON.parse(row.response_body) as HouseholdMemberSummary)
        : (row.response_body as HouseholdMemberSummary),
  };
}
async function storeReplay(
  tx: SqlExecutor,
  input: { userId: string; idempotencyKey: string; requestHash: string; occurredAt: string },
  member: HouseholdMemberSummary,
): Promise<void> {
  await tx.query(
    `INSERT INTO idempotency_records
      (user_id,operation,idempotency_key,request_hash,response_status,response_body,created_at)
     VALUES ($1,'household_members.action',$2,$3,200,$4::jsonb,$5)`,
    [
      input.userId,
      input.idempotencyKey,
      input.requestHash,
      JSON.stringify(member),
      input.occurredAt,
    ],
  );
}
function instant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}
