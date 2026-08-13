import { Injectable } from '@nestjs/common';
import type {
  CalendarEventConfiguration,
  CalendarEventSummary,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface MembershipRow {
  readonly role: HouseholdSummary['role'];
}

interface CalendarEventRow {
  readonly id: string;
  readonly household_id: string;
  readonly created_by_user_id: string;
  readonly event_type: CalendarEventSummary['type'];
  readonly title: string;
  readonly description: string | null;
  readonly event_date: Date | string;
  readonly start_time: string | null;
  readonly end_time: string | null;
  readonly reminder_minutes_before: number | null;
  readonly source_chat_message_id: string | null;
  readonly status: 'active' | 'deleted';
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type CalendarEventListResult =
  | { readonly status: 'found'; readonly events: readonly CalendarEventSummary[] }
  | { readonly status: 'not_found' };

export type CalendarEventCreateResult =
  | { readonly status: 'created' | 'replayed'; readonly event: CalendarEventSummary }
  | { readonly status: 'not_found' | 'forbidden' | 'idempotency_conflict' };

export type CalendarEventUpdateResult =
  | { readonly status: 'updated'; readonly event: CalendarEventSummary }
  | { readonly status: 'not_found' | 'forbidden' | 'version_conflict' };

export type CalendarEventDeleteResult =
  | { readonly status: 'deleted' | 'already_deleted' }
  | { readonly status: 'not_found' | 'forbidden' | 'version_conflict' };

@Injectable()
export class CalendarRepository {
  constructor(private readonly database: DatabaseService) {}

  async list(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly from: string;
    readonly to: string;
  }): Promise<CalendarEventListResult> {
    const role = await findMembershipRole(this.database, input.userId, input.householdId);
    if (role === null) {
      return { status: 'not_found' };
    }

    const rows = await this.database.query<CalendarEventRow>(
      `${calendarEventSelect()}
       WHERE household_id = $1
         AND status = 'active'
         AND event_date BETWEEN $2 AND $3
       ORDER BY event_date, start_time NULLS FIRST, created_at, id`,
      [input.householdId, input.from, input.to],
    );
    return { status: 'found', events: rows.map(mapCalendarEvent) };
  }

  async create(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly configuration: CalendarEventConfiguration;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<CalendarEventCreateResult> {
    const eventId = newUuidV7(Date.parse(input.occurredAt));
    const event: CalendarEventSummary = {
      id: eventId,
      householdId: input.householdId,
      title: input.configuration.title,
      description: input.configuration.description ?? null,
      type: input.configuration.type,
      date: input.configuration.date,
      startTime: input.configuration.startTime ?? null,
      endTime: input.configuration.endTime ?? null,
      reminderMinutesBefore: input.configuration.reminderMinutesBefore ?? null,
      sourceChatMessageId: input.configuration.sourceChatMessageId ?? null,
      createdByUserId: input.userId,
      version: 1,
      createdAt: input.occurredAt,
      updatedAt: input.occurredAt,
    };

    return this.database.transaction(async (transaction) => {
      const role = await findMembershipRole(transaction, input.userId, input.householdId);
      if (role === null) {
        return { status: 'not_found' };
      }
      if (role === 'read_only') {
        return { status: 'forbidden' };
      }

      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (
           user_id, operation, idempotency_key, request_hash, response_status, response_body, created_at
         ) VALUES ($1, 'calendar_events.create', $2, $3, 201, $4::jsonb, $5)
         ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
         RETURNING idempotency_key`,
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify(event),
          input.occurredAt,
        ],
      );

      if (claimed.length === 0) {
        const existingRows = await transaction.query<IdempotencyRow>(
          `SELECT request_hash, response_body
           FROM idempotency_records
           WHERE user_id = $1
             AND operation = 'calendar_events.create'
             AND idempotency_key = $2`,
          [input.userId, input.idempotencyKey],
        );
        const existing = existingRows[0];
        if (existing?.request_hash !== input.requestHash) {
          return { status: 'idempotency_conflict' };
        }
        return { status: 'replayed', event: readCalendarEventResponse(existing.response_body) };
      }

      await transaction.query(
        `INSERT INTO calendar_events (
           id, household_id, created_by_user_id, event_type, title, description, event_date,
           start_time, end_time, reminder_minutes_before, source_chat_message_id, status, version, created_at, updated_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, 'active', 1, $12, $12)`,
        [
          event.id,
          event.householdId,
          event.createdByUserId,
          event.type,
          event.title,
          event.description,
          event.date,
          event.startTime,
          event.endTime,
          event.reminderMinutesBefore,
          event.sourceChatMessageId ?? null,
          input.occurredAt,
        ],
      );
      if (event.sourceChatMessageId !== null && event.sourceChatMessageId !== undefined) {
        const source = await transaction.query<{ readonly id: string }>(
          `UPDATE household_chat_messages SET source_calendar_event_id=$1 WHERE id=$2 AND household_id=$3 RETURNING id`,
          [event.id, event.sourceChatMessageId, event.householdId],
        );
        if (source.length === 0) return { status: 'not_found' };
        const systemMessageId = newUuidV7(Date.parse(input.occurredAt) + 1);
        const membership = await transaction.query<{ readonly id: string }>(
          `SELECT id FROM household_memberships WHERE household_id=$1 AND user_id=$2 AND status='active' LIMIT 1`,
          [event.householdId, input.userId],
        );
        await transaction.query(
          `INSERT INTO household_chat_messages (id,household_id,sender_membership_id,body,message_kind,source_chat_message_id,source_calendar_event_id,created_at) VALUES ($1,$2,$3,$4,'system',$5,$6,$7)`,
          [
            systemMessageId,
            event.householdId,
            membership[0]?.id,
            `${event.title} · ${event.date}`,
            event.sourceChatMessageId,
            event.id,
            input.occurredAt,
          ],
        );
      }
      await writeCalendarEventEvidence(transaction, {
        userId: input.userId,
        event,
        action: 'calendar_event.created',
        eventType: 'calendar_event.created.v1',
        occurredAt: input.occurredAt,
      });
      return { status: 'created', event };
    });
  }

  async update(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly eventId: string;
    readonly expectedVersion: number;
    readonly configuration: CalendarEventConfiguration;
    readonly occurredAt: string;
  }): Promise<CalendarEventUpdateResult> {
    return this.database.transaction(async (transaction) => {
      const role = await findMembershipRole(transaction, input.userId, input.householdId);
      if (role === null) {
        return { status: 'not_found' };
      }
      if (role === 'read_only') {
        return { status: 'forbidden' };
      }

      const currentRows = await transaction.query<CalendarEventRow>(
        `${calendarEventSelect()}
         WHERE id = $1 AND household_id = $2 AND status = 'active'
         LIMIT 1
         FOR UPDATE`,
        [input.eventId, input.householdId],
      );
      const current = currentRows[0];
      if (current === undefined) {
        return { status: 'not_found' };
      }
      if (role === 'member' && current.created_by_user_id !== input.userId) {
        return { status: 'forbidden' };
      }
      if (current.version !== input.expectedVersion) {
        return { status: 'version_conflict' };
      }

      const updatedRows = await transaction.query<CalendarEventRow>(
        `UPDATE calendar_events
         SET event_type = $4,
             title = $5,
             description = $6,
             event_date = $7,
             start_time = $8,
             end_time = $9,
             reminder_minutes_before = $10,
             version = version + 1,
             updated_at = $11
         WHERE id = $1 AND household_id = $2 AND version = $3 AND status = 'active'
         RETURNING
           id, household_id, created_by_user_id, event_type, title, description, event_date,
           start_time, end_time, reminder_minutes_before, source_chat_message_id, status, version, created_at, updated_at`,
        [
          input.eventId,
          input.householdId,
          input.expectedVersion,
          input.configuration.type,
          input.configuration.title,
          input.configuration.description ?? null,
          input.configuration.date,
          input.configuration.startTime ?? null,
          input.configuration.endTime ?? null,
          input.configuration.reminderMinutesBefore ?? null,
          input.occurredAt,
        ],
      );
      const updated = updatedRows[0];
      if (updated === undefined) {
        return { status: 'version_conflict' };
      }
      const event = mapCalendarEvent(updated);
      await writeCalendarEventEvidence(transaction, {
        userId: input.userId,
        event,
        action: 'calendar_event.updated',
        eventType: 'calendar_event.updated.v1',
        occurredAt: input.occurredAt,
      });
      return { status: 'updated', event };
    });
  }

  async delete(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly eventId: string;
    readonly expectedVersion: number;
    readonly occurredAt: string;
  }): Promise<CalendarEventDeleteResult> {
    return this.database.transaction(async (transaction) => {
      const role = await findMembershipRole(transaction, input.userId, input.householdId);
      if (role === null) {
        return { status: 'not_found' };
      }
      if (role === 'read_only') {
        return { status: 'forbidden' };
      }

      const currentRows = await transaction.query<CalendarEventRow>(
        `${calendarEventSelect()}
         WHERE id = $1 AND household_id = $2
         LIMIT 1
         FOR UPDATE`,
        [input.eventId, input.householdId],
      );
      const current = currentRows[0];
      if (current === undefined) {
        return { status: 'not_found' };
      }
      if (role === 'member' && current.created_by_user_id !== input.userId) {
        return { status: 'forbidden' };
      }
      if (current.status === 'deleted') {
        return { status: 'already_deleted' };
      }
      if (current.version !== input.expectedVersion) {
        return { status: 'version_conflict' };
      }

      const deletedRows = await transaction.query<CalendarEventRow>(
        `UPDATE calendar_events
         SET status = 'deleted', version = version + 1, updated_at = $4, deleted_at = $4
         WHERE id = $1 AND household_id = $2 AND version = $3 AND status = 'active'
         RETURNING
           id, household_id, created_by_user_id, event_type, title, description, event_date,
           start_time, end_time, reminder_minutes_before, source_chat_message_id, status, version, created_at, updated_at`,
        [input.eventId, input.householdId, input.expectedVersion, input.occurredAt],
      );
      const deleted = deletedRows[0];
      if (deleted === undefined) {
        return { status: 'version_conflict' };
      }
      await writeCalendarEventEvidence(transaction, {
        userId: input.userId,
        event: mapCalendarEvent(deleted),
        action: 'calendar_event.deleted',
        eventType: 'calendar_event.deleted.v1',
        occurredAt: input.occurredAt,
      });
      return { status: 'deleted' };
    });
  }
}

async function findMembershipRole(
  executor: SqlExecutor,
  userId: string,
  householdId: string,
): Promise<HouseholdSummary['role'] | null> {
  const rows = await executor.query<MembershipRow>(
    `SELECT m.role
     FROM household_memberships m
     JOIN households h ON h.id = m.household_id
     WHERE m.user_id = $1
       AND m.household_id = $2
       AND m.status = 'active'
       AND h.status = 'active'
     LIMIT 1`,
    [userId, householdId],
  );
  return rows[0]?.role ?? null;
}

function calendarEventSelect(): string {
  return `SELECT
    id,
    household_id,
    created_by_user_id,
    event_type,
    title,
    description,
    event_date,
    start_time,
    end_time,
    reminder_minutes_before,
    source_chat_message_id,
    status,
    version,
    created_at,
    updated_at
  FROM calendar_events`;
}

function mapCalendarEvent(row: CalendarEventRow): CalendarEventSummary {
  return {
    id: row.id,
    householdId: row.household_id,
    title: row.title,
    description: row.description,
    type: row.event_type,
    date: toLocalDate(row.event_date),
    startTime: toLocalTime(row.start_time),
    endTime: toLocalTime(row.end_time),
    reminderMinutesBefore: row.reminder_minutes_before,
    sourceChatMessageId: row.source_chat_message_id,
    createdByUserId: row.created_by_user_id,
    version: row.version,
    createdAt: toInstant(row.created_at),
    updatedAt: toInstant(row.updated_at),
  };
}

function toLocalDate(value: Date | string): string {
  return typeof value === 'string' ? value.slice(0, 10) : value.toISOString().slice(0, 10);
}

function toLocalTime(value: string | null): string | null {
  return value === null ? null : value.slice(0, 5);
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function readCalendarEventResponse(value: unknown): CalendarEventSummary {
  if (typeof value === 'string') {
    return JSON.parse(value) as CalendarEventSummary;
  }
  return value as CalendarEventSummary;
}

async function writeCalendarEventEvidence(
  transaction: SqlExecutor,
  input: {
    readonly userId: string;
    readonly event: CalendarEventSummary;
    readonly action: string;
    readonly eventType: string;
    readonly occurredAt: string;
  },
): Promise<void> {
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, occurred_at
     ) VALUES ($1, $2, $3, $4, 'calendar_event', $5, 'success', $6)`,
    [
      newUuidV7(),
      input.userId,
      input.event.householdId,
      input.action,
      input.event.id,
      input.occurredAt,
    ],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id, payload, occurred_at
     ) VALUES ($1, $2, 'calendar_event', $3, $4, $5, $6::jsonb, $7)`,
    [
      newUuidV7(),
      input.eventType,
      input.event.id,
      input.event.householdId,
      input.userId,
      JSON.stringify(input.event),
      input.occurredAt,
    ],
  );
}
