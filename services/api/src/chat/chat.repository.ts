import { Injectable } from '@nestjs/common';
import type {
  HouseholdChatMessage,
  HouseholdChatPage,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface MembershipRow {
  readonly membership_id: string;
  readonly user_id: string;
  readonly role: HouseholdSummary['role'];
  readonly display_name: string;
}

interface MessageRow {
  readonly id: string;
  readonly household_id: string;
  readonly sender_membership_id: string;
  readonly sender_user_id: string;
  readonly sender_display_name: string;
  readonly body: string;
  readonly created_at: Date | string;
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type ChatListResult =
  { readonly status: 'found'; readonly page: HouseholdChatPage } | { readonly status: 'not_found' };

export type ChatCreateResult =
  | { readonly status: 'created' | 'replayed'; readonly message: HouseholdChatMessage }
  | { readonly status: 'not_found' | 'forbidden' | 'idempotency_conflict' };

@Injectable()
export class ChatRepository {
  constructor(private readonly database: DatabaseService) {}

  async list(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly after: string | null;
    readonly limit: number;
  }): Promise<ChatListResult> {
    const membership = await findMembership(this.database, input.userId, input.householdId);
    if (membership === null) return { status: 'not_found' };

    const parameters: readonly unknown[] =
      input.after === null
        ? [input.householdId, input.limit]
        : [input.householdId, input.after, input.limit];
    const rows = await this.database.query<MessageRow>(
      input.after === null
        ? `SELECT * FROM (
             ${messageSelect()}
             WHERE message.household_id = $1
             ORDER BY message.id DESC
             LIMIT $2
           ) recent
           ORDER BY recent.id`
        : `${messageSelect()}
           WHERE message.household_id = $1 AND message.id > $2
           ORDER BY message.id
           LIMIT $3`,
      parameters,
    );
    const messages = rows.map((row) => mapMessage(row, input.userId));
    return {
      status: 'found',
      page: { messages, nextCursor: messages.at(-1)?.id ?? input.after },
    };
  }

  async create(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly body: string;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<ChatCreateResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (membership.role === 'read_only') return { status: 'forbidden' };

      const message: HouseholdChatMessage = {
        id: newUuidV7(Date.parse(input.occurredAt)),
        householdId: input.householdId,
        senderMembershipId: membership.membership_id,
        senderUserId: membership.user_id,
        senderDisplayName: membership.display_name,
        isCurrentUser: true,
        body: input.body,
        createdAt: input.occurredAt,
      };
      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (
           user_id, operation, idempotency_key, request_hash, response_status, response_body, created_at
         ) VALUES ($1, 'household_chat.create', $2, $3, 201, $4::jsonb, $5)
         ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
         RETURNING idempotency_key`,
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify(message),
          input.occurredAt,
        ],
      );
      if (claimed.length === 0) {
        const existingRows = await transaction.query<IdempotencyRow>(
          `SELECT request_hash, response_body FROM idempotency_records
           WHERE user_id = $1 AND operation = 'household_chat.create' AND idempotency_key = $2`,
          [input.userId, input.idempotencyKey],
        );
        const existing = existingRows[0];
        if (existing?.request_hash !== input.requestHash) return { status: 'idempotency_conflict' };
        return { status: 'replayed', message: readMessage(existing.response_body) };
      }

      await transaction.query(
        `INSERT INTO household_chat_messages (id, household_id, sender_membership_id, body, created_at)
         VALUES ($1, $2, $3, $4, $5)`,
        [
          message.id,
          message.householdId,
          message.senderMembershipId,
          message.body,
          message.createdAt,
        ],
      );
      const eventId = newUuidV7(Date.parse(input.occurredAt) + 1);
      await transaction.query(
        `INSERT INTO audit_events (
           id, actor_user_id, household_id, action, target_type, target_id, outcome, safe_details, occurred_at
         ) VALUES ($1, $2, $3, 'household_chat.message_created', 'household_chat_message', $4, 'success', $5::jsonb, $6)`,
        [
          eventId,
          input.userId,
          input.householdId,
          message.id,
          JSON.stringify({ characterCount: message.body.length }),
          input.occurredAt,
        ],
      );
      await transaction.query(
        `INSERT INTO outbox_events (
           id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id, payload, occurred_at
         ) VALUES ($1, 'household_chat.message_created.v1', 'household_chat_message', $2, $3, $4, $5::jsonb, $6)`,
        [
          newUuidV7(Date.parse(input.occurredAt) + 2),
          message.id,
          input.householdId,
          input.userId,
          JSON.stringify({ messageId: message.id, senderMembershipId: message.senderMembershipId }),
          input.occurredAt,
        ],
      );
      return { status: 'created', message };
    });
  }
}

async function findMembership(
  executor: SqlExecutor,
  userId: string,
  householdId: string,
): Promise<MembershipRow | null> {
  const rows = await executor.query<MembershipRow>(
    `SELECT membership.id AS membership_id, membership.user_id, membership.role, profile.display_name
     FROM household_memberships membership
     JOIN households household ON household.id = membership.household_id AND household.status = 'active'
     JOIN users account ON account.id = membership.user_id AND account.status = 'active'
     JOIN user_profiles profile ON profile.user_id = membership.user_id
     WHERE membership.household_id = $1 AND membership.user_id = $2 AND membership.status = 'active'
     LIMIT 1`,
    [householdId, userId],
  );
  return rows[0] ?? null;
}

function messageSelect(): string {
  return `SELECT message.id, message.household_id, message.sender_membership_id,
                 sender.user_id AS sender_user_id, profile.display_name AS sender_display_name,
                 message.body, message.created_at
          FROM household_chat_messages message
          JOIN household_memberships sender ON sender.id = message.sender_membership_id
          JOIN user_profiles profile ON profile.user_id = sender.user_id`;
}

function mapMessage(row: MessageRow, currentUserId: string): HouseholdChatMessage {
  return {
    id: row.id,
    householdId: row.household_id,
    senderMembershipId: row.sender_membership_id,
    senderUserId: row.sender_user_id,
    senderDisplayName: row.sender_display_name,
    isCurrentUser: row.sender_user_id === currentUserId,
    body: row.body,
    createdAt: new Date(row.created_at).toISOString(),
  };
}

function readMessage(value: unknown): HouseholdChatMessage {
  if (typeof value !== 'object' || value === null || !('id' in value) || !('body' in value)) {
    throw new Error('Stored household chat response is invalid.');
  }
  return value as HouseholdChatMessage;
}
