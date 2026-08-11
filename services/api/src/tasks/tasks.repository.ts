import { Injectable } from '@nestjs/common';
import type {
  HouseholdTaskActionRequest,
  HouseholdTaskBoard,
  HouseholdTaskConfiguration,
  HouseholdTaskMemberSummary,
  HouseholdTaskRequestSummary,
  HouseholdTaskStatus,
  HouseholdTaskSummary,
} from '@sharedhouse/contracts';
import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

type HouseholdRole = 'owner' | 'admin' | 'member' | 'read_only';
interface MemberRow {
  readonly membership_id: string;
  readonly user_id: string;
  readonly display_name: string;
  readonly role: HouseholdRole;
}
interface TaskRow {
  readonly id: string;
  readonly household_id: string;
  readonly created_by_user_id: string;
  readonly assignee_membership_id: string;
  readonly assignee_display_name: string;
  readonly title: string;
  readonly instructions: string | null;
  readonly zone: string | null;
  readonly priority: 'low' | 'normal' | 'high';
  readonly due_date: Date | string;
  readonly due_time: string | null;
  readonly estimated_minutes: number | null;
  readonly status: HouseholdTaskStatus;
  readonly completion_note: string | null;
  readonly completed_by_user_id: string | null;
  readonly completed_at: Date | string | null;
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
}
interface RequestRow {
  readonly id: string;
  readonly task_id: string;
  readonly request_type: HouseholdTaskRequestSummary['type'];
  readonly status: HouseholdTaskRequestSummary['status'];
  readonly reason: string;
  readonly requested_assignee_membership_id: string | null;
  readonly requested_due_date: Date | string | null;
  readonly requested_due_time: string | null;
  readonly created_by_membership_id: string;
  readonly created_by_display_name: string;
  readonly resolved_by_user_id: string | null;
  readonly resolution_note: string | null;
  readonly resolved_at: Date | string | null;
  readonly created_at: Date | string;
}
interface StoredResponse {
  readonly request_hash: string;
  readonly response_body: unknown;
}
type MutationResult =
  | { readonly status: 'ok' | 'replayed'; readonly task: HouseholdTaskSummary }
  | {
      readonly status:
        | 'not_found'
        | 'forbidden'
        | 'version_conflict'
        | 'invalid_transition'
        | 'request_conflict'
        | 'invalid_member'
        | 'idempotency_conflict';
    };

@Injectable()
export class TasksRepository {
  constructor(private readonly database: DatabaseService) {}

  async list(userId: string, householdId: string): Promise<HouseholdTaskBoard | null> {
    const members = await listMembers(this.database, householdId);
    const actor = members.find((member) => member.user_id === userId);
    if (actor === undefined) return null;
    const rows = await this.database.query<TaskRow>(
      `${taskSelect()} WHERE t.household_id = $1 ORDER BY CASE t.status WHEN 'in_progress' THEN 0 WHEN 'open' THEN 1 WHEN 'completed' THEN 2 ELSE 3 END, t.due_date, t.due_time NULLS LAST, t.id`,
      [householdId],
    );
    const requests = await listRequests(
      this.database,
      rows.map((row) => row.id),
    );
    return {
      canCreate: isManager(actor.role),
      members: members
        .filter((member) => member.role !== 'read_only')
        .map((member) => mapMember(member, userId)),
      tasks: rows.map((row) =>
        mapTask(
          row,
          requests.filter((request) => request.task_id === row.id),
          actor,
        ),
      ),
    };
  }

  async create(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly configuration: HouseholdTaskConfiguration;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<MutationResult> {
    return this.database.transaction(async (tx) => {
      const members = await listMembers(tx, input.householdId);
      const actor = members.find((member) => member.user_id === input.userId);
      if (actor === undefined) return { status: 'not_found' };
      if (!isManager(actor.role)) return { status: 'forbidden' };
      if (
        !members.some(
          (member) =>
            member.membership_id === input.configuration.assigneeMembershipId &&
            member.role !== 'read_only',
        )
      )
        return { status: 'invalid_member' };
      const replay = await readReplay(
        tx,
        input.userId,
        'household_tasks.create',
        input.idempotencyKey,
        input.requestHash,
      );
      if (replay.status === 'conflict') return { status: 'idempotency_conflict' };
      if (replay.status === 'found') return { status: 'replayed', task: replay.task };
      const id = newUuidV7(Date.parse(input.occurredAt));
      await tx.query(
        `INSERT INTO household_tasks (id, household_id, created_by_user_id, assignee_membership_id, title, instructions, zone, priority, due_date, due_time, estimated_minutes, status, version, created_at, updated_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,'open',1,$12,$12)`,
        [
          id,
          input.householdId,
          input.userId,
          input.configuration.assigneeMembershipId,
          input.configuration.title,
          input.configuration.instructions ?? null,
          input.configuration.zone ?? null,
          input.configuration.priority,
          input.configuration.dueDate,
          input.configuration.dueTime ?? null,
          input.configuration.estimatedMinutes ?? null,
          input.occurredAt,
        ],
      );
      await writeHistoryAndEvidence(tx, {
        taskId: id,
        householdId: input.householdId,
        actorUserId: input.userId,
        eventType: 'created',
        fromStatus: null,
        toStatus: 'open',
        details: input.configuration,
        occurredAt: input.occurredAt,
      });
      const row = await getTask(tx, input.householdId, id);
      if (row === undefined) throw new Error('Created task could not be read.');
      const task = mapTask(row, [], actor);
      await storeReplay(
        tx,
        input.userId,
        'household_tasks.create',
        input.idempotencyKey,
        input.requestHash,
        task,
        input.occurredAt,
      );
      return { status: 'ok', task };
    });
  }

  async action(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly taskId: string;
    readonly expectedVersion: number;
    readonly action: HouseholdTaskActionRequest;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<MutationResult> {
    return this.database.transaction(async (tx) => {
      const members = await listMembers(tx, input.householdId);
      const actor = members.find((member) => member.user_id === input.userId);
      if (actor === undefined) return { status: 'not_found' };
      const replay = await readReplay(
        tx,
        input.userId,
        'household_tasks.action',
        input.idempotencyKey,
        input.requestHash,
      );
      if (replay.status === 'conflict') return { status: 'idempotency_conflict' };
      if (replay.status === 'found') return { status: 'replayed', task: replay.task };
      const current = await getTask(tx, input.householdId, input.taskId, true);
      if (current === undefined) return { status: 'not_found' };
      if (current.version !== input.expectedVersion) return { status: 'version_conflict' };
      const manager = isManager(actor.role);
      const assignee = actor.membership_id === current.assignee_membership_id;
      let eventType: string = input.action.action;
      let nextStatus = current.status;
      let completionNote = current.completion_note;
      let completedByUserId = current.completed_by_user_id;
      let completedAt = current.completed_at === null ? null : toInstant(current.completed_at);
      let assigneeMembershipId = current.assignee_membership_id;
      let dueDate = toDate(current.due_date);
      let dueTime = toTime(current.due_time);

      if (input.action.action === 'start') {
        if ((!manager && !assignee) || current.status !== 'open')
          return { status: manager || assignee ? 'invalid_transition' : 'forbidden' };
        nextStatus = 'in_progress';
      } else if (input.action.action === 'complete') {
        if (
          (!manager && !assignee) ||
          (current.status !== 'open' && current.status !== 'in_progress')
        )
          return { status: manager || assignee ? 'invalid_transition' : 'forbidden' };
        nextStatus = 'completed';
        completionNote = input.action.note ?? null;
        completedByUserId = input.userId;
        completedAt = input.occurredAt;
      } else if (input.action.action === 'cancel' || input.action.action === 'reopen') {
        if (!manager) return { status: 'forbidden' };
        if (input.action.action === 'cancel') {
          if (current.status === 'cancelled') return { status: 'invalid_transition' };
          nextStatus = 'cancelled';
          completionNote = null;
          completedByUserId = null;
          completedAt = null;
        } else {
          if (current.status !== 'completed' && current.status !== 'cancelled')
            return { status: 'invalid_transition' };
          nextStatus = 'open';
          completionNote = null;
          completedByUserId = null;
          completedAt = null;
        }
      } else if (
        input.action.action.startsWith('request_') ||
        input.action.action === 'report_issue'
      ) {
        if (!assignee || (current.status !== 'open' && current.status !== 'in_progress'))
          return { status: assignee ? 'invalid_transition' : 'forbidden' };
        const requestType =
          input.action.action === 'request_help'
            ? 'help'
            : input.action.action === 'request_swap'
              ? 'swap'
              : input.action.action === 'request_postpone'
                ? 'postpone'
                : 'issue';
        const duplicate = await tx.query<{ readonly id: string }>(
          "SELECT id FROM household_task_requests WHERE task_id=$1 AND request_type=$2 AND status='pending' LIMIT 1",
          [input.taskId, requestType],
        );
        if (duplicate.length > 0) return { status: 'request_conflict' };
        if (
          requestType === 'swap' &&
          !members.some(
            (member) =>
              member.membership_id === input.action.requestedAssigneeMembershipId &&
              member.role !== 'read_only' &&
              member.membership_id !== current.assignee_membership_id,
          )
        )
          return { status: 'invalid_member' };
        if (
          requestType === 'postpone' &&
          input.action.requestedDueDate !== undefined &&
          input.action.requestedDueDate !== null &&
          input.action.requestedDueDate <= dueDate
        )
          return { status: 'invalid_transition' };
        await tx.query(
          `INSERT INTO household_task_requests (id,task_id,created_by_membership_id,request_type,status,reason,requested_assignee_membership_id,requested_due_date,requested_due_time,created_at)
           VALUES ($1,$2,$3,$4,'pending',$5,$6,$7,$8,$9)`,
          [
            newUuidV7(),
            input.taskId,
            actor.membership_id,
            requestType,
            input.action.note,
            input.action.requestedAssigneeMembershipId ?? null,
            input.action.requestedDueDate ?? null,
            input.action.requestedDueTime ?? null,
            input.occurredAt,
          ],
        );
        eventType = `${requestType}_requested`;
      } else {
        if (!manager) return { status: 'forbidden' };
        const requestRows = await tx.query<RequestRow>(
          `${requestSelect()} WHERE r.id=$1 AND r.task_id=$2 LIMIT 1 FOR UPDATE`,
          [input.action.requestId, input.taskId],
        );
        const request = requestRows[0];
        if (request?.status !== 'pending') return { status: 'request_conflict' };
        const approved = input.action.action === 'approve_request';
        await tx.query(
          `UPDATE household_task_requests SET status=$3,resolved_by_user_id=$4,resolution_note=$5,resolved_at=$6 WHERE id=$1 AND task_id=$2 AND status='pending'`,
          [
            request.id,
            input.taskId,
            approved ? 'approved' : 'rejected',
            input.userId,
            input.action.note ?? null,
            input.occurredAt,
          ],
        );
        if (
          approved &&
          request.request_type === 'swap' &&
          request.requested_assignee_membership_id !== null
        )
          assigneeMembershipId = request.requested_assignee_membership_id;
        if (
          approved &&
          request.request_type === 'postpone' &&
          request.requested_due_date !== null
        ) {
          dueDate = toDate(request.requested_due_date);
          dueTime = toTime(request.requested_due_time);
        }
        eventType = approved
          ? `${request.request_type}_approved`
          : `${request.request_type}_rejected`;
      }

      const updatedRows = await tx.query<TaskRow>(
        `UPDATE household_tasks SET assignee_membership_id=$4,due_date=$5,due_time=$6,status=$7,completion_note=$8,completed_by_user_id=$9,completed_at=$10,version=version+1,updated_at=$11
         WHERE id=$1 AND household_id=$2 AND version=$3 RETURNING id`,
        [
          input.taskId,
          input.householdId,
          input.expectedVersion,
          assigneeMembershipId,
          dueDate,
          dueTime,
          nextStatus,
          completionNote,
          completedByUserId,
          completedAt,
          input.occurredAt,
        ],
      );
      if (updatedRows.length === 0) return { status: 'version_conflict' };
      await writeHistoryAndEvidence(tx, {
        taskId: input.taskId,
        householdId: input.householdId,
        actorUserId: input.userId,
        eventType,
        fromStatus: current.status,
        toStatus: nextStatus,
        details: input.action,
        occurredAt: input.occurredAt,
      });
      const row = await getTask(tx, input.householdId, input.taskId);
      if (row === undefined) throw new Error('Updated task could not be read.');
      const requests = await listRequests(tx, [input.taskId]);
      const task = mapTask(row, requests, actor);
      await storeReplay(
        tx,
        input.userId,
        'household_tasks.action',
        input.idempotencyKey,
        input.requestHash,
        task,
        input.occurredAt,
      );
      return { status: 'ok', task };
    });
  }
}

async function listMembers(tx: SqlExecutor, householdId: string): Promise<readonly MemberRow[]> {
  return tx.query<MemberRow>(
    `SELECT m.id membership_id,m.user_id,p.display_name,m.role FROM household_memberships m JOIN households h ON h.id=m.household_id JOIN user_profiles p ON p.user_id=m.user_id WHERE m.household_id=$1 AND m.status='active' AND h.status='active' ORDER BY lower(p.display_name),m.joined_at,m.id`,
    [householdId],
  );
}
async function getTask(
  tx: SqlExecutor,
  householdId: string,
  taskId: string,
  lock = false,
): Promise<TaskRow | undefined> {
  const rows = await tx.query<TaskRow>(
    `${taskSelect()} WHERE t.household_id=$1 AND t.id=$2${lock ? ' FOR UPDATE' : ''}`,
    [householdId, taskId],
  );
  return rows[0];
}
function taskSelect(): string {
  return `SELECT t.id,t.household_id,t.created_by_user_id,t.assignee_membership_id,p.display_name assignee_display_name,t.title,t.instructions,t.zone,t.priority,t.due_date,t.due_time,t.estimated_minutes,t.status,t.completion_note,t.completed_by_user_id,t.completed_at,t.version,t.created_at,t.updated_at FROM household_tasks t JOIN household_memberships m ON m.id=t.assignee_membership_id JOIN user_profiles p ON p.user_id=m.user_id`;
}
function requestSelect(): string {
  return `SELECT r.id,r.task_id,r.request_type,r.status,r.reason,r.requested_assignee_membership_id,r.requested_due_date,r.requested_due_time,r.created_by_membership_id,p.display_name created_by_display_name,r.resolved_by_user_id,r.resolution_note,r.resolved_at,r.created_at FROM household_task_requests r JOIN household_memberships m ON m.id=r.created_by_membership_id JOIN user_profiles p ON p.user_id=m.user_id`;
}
async function listRequests(
  tx: SqlExecutor,
  taskIds: readonly string[],
): Promise<readonly RequestRow[]> {
  if (taskIds.length === 0) return [];
  return tx.query<RequestRow>(
    `${requestSelect()} WHERE r.task_id = ANY($1::uuid[]) ORDER BY r.created_at DESC,r.id DESC`,
    [taskIds],
  );
}
function mapMember(row: MemberRow, userId: string): HouseholdTaskMemberSummary {
  return {
    membershipId: row.membership_id,
    userId: row.user_id,
    displayName: row.display_name,
    role: row.role,
    isCurrentUser: row.user_id === userId,
  };
}
function mapTask(
  row: TaskRow,
  requests: readonly RequestRow[],
  actor: MemberRow,
): HouseholdTaskSummary {
  const manager = isManager(actor.role);
  const assignee = actor.membership_id === row.assignee_membership_id;
  const active = row.status === 'open' || row.status === 'in_progress';
  return {
    id: row.id,
    householdId: row.household_id,
    title: row.title,
    instructions: row.instructions,
    zone: row.zone,
    priority: row.priority,
    dueDate: toDate(row.due_date),
    dueTime: toTime(row.due_time),
    estimatedMinutes: row.estimated_minutes,
    assigneeMembershipId: row.assignee_membership_id,
    assigneeDisplayName: row.assignee_display_name,
    status: row.status,
    completionNote: row.completion_note,
    completedByUserId: row.completed_by_user_id,
    completedAt: row.completed_at === null ? null : toInstant(row.completed_at),
    requests: requests.map(mapRequest),
    canManage: manager,
    canStart: active && row.status === 'open' && (manager || assignee),
    canComplete: active && (manager || assignee),
    canRequest: active && assignee,
    version: row.version,
    createdAt: toInstant(row.created_at),
    updatedAt: toInstant(row.updated_at),
  };
}
function mapRequest(row: RequestRow): HouseholdTaskRequestSummary {
  return {
    id: row.id,
    type: row.request_type,
    status: row.status,
    reason: row.reason,
    requestedAssigneeMembershipId: row.requested_assignee_membership_id,
    requestedDueDate: row.requested_due_date === null ? null : toDate(row.requested_due_date),
    requestedDueTime: toTime(row.requested_due_time),
    createdByMembershipId: row.created_by_membership_id,
    createdByDisplayName: row.created_by_display_name,
    resolvedByUserId: row.resolved_by_user_id,
    resolutionNote: row.resolution_note,
    resolvedAt: row.resolved_at === null ? null : toInstant(row.resolved_at),
    createdAt: toInstant(row.created_at),
  };
}
function isManager(role: HouseholdRole): boolean {
  return role === 'owner' || role === 'admin';
}
function toDate(value: Date | string): string {
  return typeof value === 'string' ? value.slice(0, 10) : value.toISOString().slice(0, 10);
}
function toTime(value: string | null): string | null {
  return value === null ? null : value.slice(0, 5);
}
function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

async function readReplay(
  tx: SqlExecutor,
  userId: string,
  operation: string,
  key: string,
  hash: string,
): Promise<
  | { readonly status: 'none' }
  | { readonly status: 'conflict' }
  | { readonly status: 'found'; readonly task: HouseholdTaskSummary }
> {
  const rows = await tx.query<StoredResponse>(
    'SELECT request_hash,response_body FROM idempotency_records WHERE user_id=$1 AND operation=$2 AND idempotency_key=$3',
    [userId, operation, key],
  );
  const row = rows[0];
  if (row === undefined) return { status: 'none' };
  if (row.request_hash !== hash) return { status: 'conflict' };
  return {
    status: 'found',
    task: (typeof row.response_body === 'string'
      ? JSON.parse(row.response_body)
      : row.response_body) as HouseholdTaskSummary,
  };
}
async function storeReplay(
  tx: SqlExecutor,
  userId: string,
  operation: string,
  key: string,
  hash: string,
  task: HouseholdTaskSummary,
  occurredAt: string,
): Promise<void> {
  await tx.query(
    `INSERT INTO idempotency_records (user_id,operation,idempotency_key,request_hash,response_status,response_body,created_at) VALUES ($1,$2,$3,$4,200,$5::jsonb,$6)`,
    [userId, operation, key, hash, JSON.stringify(task), occurredAt],
  );
}
async function writeHistoryAndEvidence(
  tx: SqlExecutor,
  input: {
    readonly taskId: string;
    readonly householdId: string;
    readonly actorUserId: string;
    readonly eventType: string;
    readonly fromStatus: string | null;
    readonly toStatus: string;
    readonly details: unknown;
    readonly occurredAt: string;
  },
): Promise<void> {
  await tx.query(
    `INSERT INTO household_task_history (id,task_id,actor_user_id,event_type,from_status,to_status,details,occurred_at) VALUES ($1,$2,$3,$4,$5,$6,$7::jsonb,$8)`,
    [
      newUuidV7(),
      input.taskId,
      input.actorUserId,
      input.eventType,
      input.fromStatus,
      input.toStatus,
      JSON.stringify(input.details),
      input.occurredAt,
    ],
  );
  await tx.query(
    `INSERT INTO audit_events (id,actor_user_id,household_id,action,target_type,target_id,outcome,safe_details,occurred_at) VALUES ($1,$2,$3,$4,'household_task',$5,'success',$6::jsonb,$7)`,
    [
      newUuidV7(),
      input.actorUserId,
      input.householdId,
      `household_task.${input.eventType}`,
      input.taskId,
      JSON.stringify({ fromStatus: input.fromStatus, toStatus: input.toStatus }),
      input.occurredAt,
    ],
  );
  await tx.query(
    `INSERT INTO outbox_events (id,event_type,aggregate_type,aggregate_id,household_id,actor_user_id,payload,occurred_at) VALUES ($1,$2,'household_task',$3,$4,$5,$6::jsonb,$7)`,
    [
      newUuidV7(),
      `household_task.${input.eventType}.v1`,
      input.taskId,
      input.householdId,
      input.actorUserId,
      JSON.stringify({
        taskId: input.taskId,
        fromStatus: input.fromStatus,
        toStatus: input.toStatus,
      }),
      input.occurredAt,
    ],
  );
}
