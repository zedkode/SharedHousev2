import { Injectable } from '@nestjs/common';
import type {
  HouseholdChatAttachment,
  HouseholdChatMessage,
  HouseholdChatPage,
  HouseholdSummary,
  HouseholdTaskMemberSummary,
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
  readonly message_kind: 'member' | 'system';
  readonly latitude: number | string | null;
  readonly longitude: number | string | null;
  readonly source_chat_message_id: string | null;
  readonly source_calendar_event_id: string | null;
  readonly pin_id: string | null;
  readonly pinned_by_display_name: string | null;
  readonly created_at: Date | string;
}

interface AttachmentRow {
  readonly id: string;
  readonly message_id: string | null;
  readonly media_type: HouseholdChatAttachment['mediaType'];
  readonly byte_size: number;
  readonly width: number;
  readonly height: number;
}

interface AttachmentContentRow extends AttachmentRow {
  readonly content: Buffer;
}

interface MentionRow {
  readonly message_id: string;
  readonly mentioned_user_id: string;
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
        ? `SELECT * FROM (${messageSelect()} WHERE message.household_id=$1 ORDER BY message.id DESC LIMIT $2) recent ORDER BY recent.id`
        : `${messageSelect()} WHERE message.household_id=$1 AND message.id>$2 ORDER BY message.id LIMIT $3`,
      parameters,
    );
    const pinnedRows = await this.database.query<MessageRow>(
      `${messageSelect()} WHERE message.household_id=$1 AND active_pin.id IS NOT NULL ORDER BY active_pin.pinned_at DESC LIMIT 5`,
      [input.householdId],
    );
    const uniqueRows = [...new Map([...rows, ...pinnedRows].map((row) => [row.id, row])).values()];
    const details = await loadDetails(
      this.database,
      uniqueRows.map((row) => row.id),
    );
    const messages = rows.map((row) => mapMessage(row, input.userId, details));
    const members = await this.database.query<{
      readonly membership_id: string;
      readonly user_id: string;
      readonly display_name: string;
      readonly role: HouseholdSummary['role'];
    }>(
      `SELECT membership.id AS membership_id,membership.user_id,profile.display_name,membership.role
       FROM household_memberships membership JOIN users account ON account.id=membership.user_id AND account.status='active'
       JOIN user_profiles profile ON profile.user_id=membership.user_id
       WHERE membership.household_id=$1 AND membership.status='active' ORDER BY profile.display_name,membership.id`,
      [input.householdId],
    );
    return {
      status: 'found',
      page: {
        messages,
        pinnedMessages: pinnedRows.map((row) => mapMessage(row, input.userId, details)),
        members: members.map((row): HouseholdTaskMemberSummary => ({
          membershipId: row.membership_id,
          userId: row.user_id,
          displayName: row.display_name,
          role: row.role,
          isCurrentUser: row.user_id === input.userId,
        })),
        canMentionAll: membership.role === 'owner' || membership.role === 'admin',
        nextCursor: messages.at(-1)?.id ?? input.after,
      },
    };
  }

  async create(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly body: string;
    readonly attachmentIds: readonly string[];
    readonly mentionedUserIds: readonly string[];
    readonly mentionAll: boolean;
    readonly location: { readonly latitude: number; readonly longitude: number } | null;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<ChatCreateResult> {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (membership.role === 'read_only') return { status: 'forbidden' };
      if (input.mentionAll && membership.role !== 'owner' && membership.role !== 'admin')
        return { status: 'forbidden' };
      const messageId = newUuidV7(Date.parse(input.occurredAt));
      const baseMessage: HouseholdChatMessage = {
        id: messageId,
        householdId: input.householdId,
        senderMembershipId: membership.membership_id,
        senderUserId: membership.user_id,
        senderDisplayName: membership.display_name,
        isCurrentUser: true,
        body: input.body,
        kind: 'member',
        attachments: [],
        mentionedUserIds: input.mentionedUserIds,
        location: input.location,
        isPinned: false,
        pinnedByDisplayName: null,
        sourceChatMessageId: null,
        sourceCalendarEventId: null,
        createdAt: input.occurredAt,
      };
      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (user_id,operation,idempotency_key,request_hash,response_status,response_body,created_at)
         VALUES ($1,'household_chat.create',$2,$3,201,$4::jsonb,$5)
         ON CONFLICT (user_id,operation,idempotency_key) DO NOTHING RETURNING idempotency_key`,
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify(baseMessage),
          input.occurredAt,
        ],
      );
      if (claimed.length === 0) {
        const existingRows = await transaction.query<IdempotencyRow>(
          `SELECT request_hash,response_body FROM idempotency_records WHERE user_id=$1 AND operation='household_chat.create' AND idempotency_key=$2`,
          [input.userId, input.idempotencyKey],
        );
        const existing = existingRows[0];
        if (existing?.request_hash !== input.requestHash) return { status: 'idempotency_conflict' };
        return { status: 'replayed', message: readMessage(existing.response_body) };
      }
      if (!(await validMentionTargets(transaction, input.householdId, input.mentionedUserIds)))
        return { status: 'forbidden' };
      if (!(await claimAttachments(transaction, input, membership.membership_id, messageId)))
        return { status: 'forbidden' };
      await transaction.query(
        `INSERT INTO household_chat_messages
         (id,household_id,sender_membership_id,body,message_kind,latitude,longitude,created_at)
         VALUES ($1,$2,$3,$4,'member',$5,$6,$7)`,
        [
          messageId,
          input.householdId,
          membership.membership_id,
          input.body,
          input.location?.latitude ?? null,
          input.location?.longitude ?? null,
          input.occurredAt,
        ],
      );
      if (input.mentionAll) {
        await transaction.query(
          `INSERT INTO household_chat_mentions (message_id,mentioned_user_id,created_at)
           SELECT $1,m.user_id,$2 FROM household_memberships m
           WHERE m.household_id=$3 AND m.status='active' AND m.user_id<>$4`,
          [messageId, input.occurredAt, input.householdId, input.userId],
        );
      } else {
        for (const mentionedUserId of input.mentionedUserIds) {
          await transaction.query(
            `INSERT INTO household_chat_mentions (message_id,mentioned_user_id,created_at) VALUES ($1,$2,$3) ON CONFLICT DO NOTHING`,
            [messageId, mentionedUserId, input.occurredAt],
          );
        }
      }
      await insertAuditAndOutbox(transaction, input, messageId, membership.membership_id);
      const details = await loadDetails(transaction, [messageId]);
      const rows = await transaction.query<MessageRow>(`${messageSelect()} WHERE message.id=$1`, [
        messageId,
      ]);
      const message = mapMessage(requireRow(rows[0]), input.userId, details);
      await transaction.query(
        `UPDATE idempotency_records SET response_body=$3::jsonb WHERE user_id=$1 AND operation='household_chat.create' AND idempotency_key=$2`,
        [input.userId, input.idempotencyKey, JSON.stringify(message)],
      );
      return { status: 'created', message };
    });
  }

  async uploadAttachment(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly mediaType: HouseholdChatAttachment['mediaType'];
    readonly width: number;
    readonly height: number;
    readonly content: Buffer;
    readonly occurredAt: string;
  }): Promise<
    | { readonly status: 'created'; readonly attachment: HouseholdChatAttachment }
    | { readonly status: 'not_found' | 'forbidden' }
  > {
    const membership = await findMembership(this.database, input.userId, input.householdId);
    if (membership === null) return { status: 'not_found' };
    if (membership.role === 'read_only') return { status: 'forbidden' };
    const id = newUuidV7(Date.parse(input.occurredAt));
    await this.database.query(
      `INSERT INTO household_chat_attachments
       (id,message_id,household_id,uploaded_by_membership_id,media_type,byte_size,width,height,content,created_at)
       VALUES ($1,NULL,$2,$3,$4,$5,$6,$7,$8,$9)`,
      [
        id,
        input.householdId,
        membership.membership_id,
        input.mediaType,
        input.content.length,
        input.width,
        input.height,
        input.content,
        input.occurredAt,
      ],
    );
    return {
      status: 'created',
      attachment: mapAttachment(
        {
          id,
          message_id: null,
          media_type: input.mediaType,
          byte_size: input.content.length,
          width: input.width,
          height: input.height,
        },
        input.householdId,
      ),
    };
  }

  async downloadAttachment(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly attachmentId: string;
  }): Promise<
    | {
        readonly status: 'found';
        readonly attachment: {
          readonly mediaType: HouseholdChatAttachment['mediaType'];
          readonly content: Buffer;
        };
      }
    | { readonly status: 'not_found' }
  > {
    if ((await findMembership(this.database, input.userId, input.householdId)) === null)
      return { status: 'not_found' };
    const rows = await this.database.query<AttachmentContentRow>(
      `SELECT id,message_id,media_type,byte_size,width,height,content FROM household_chat_attachments
       WHERE id=$1 AND household_id=$2 AND message_id IS NOT NULL LIMIT 1`,
      [input.attachmentId, input.householdId],
    );
    const row = rows[0];
    return row === undefined
      ? { status: 'not_found' }
      : { status: 'found', attachment: { mediaType: row.media_type, content: row.content } };
  }

  async setPinned(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly messageId: string;
    readonly pinned: boolean;
    readonly occurredAt: string;
  }): Promise<
    | { readonly status: 'updated'; readonly message: HouseholdChatMessage }
    | { readonly status: 'not_found' | 'forbidden' | 'limit' }
  > {
    return this.database.transaction(async (transaction) => {
      const membership = await findMembership(transaction, input.userId, input.householdId);
      if (membership === null) return { status: 'not_found' };
      if (membership.role === 'read_only') return { status: 'forbidden' };
      const messages = await transaction.query<{ readonly id: string }>(
        `SELECT id FROM household_chat_messages WHERE id=$1 AND household_id=$2 FOR UPDATE`,
        [input.messageId, input.householdId],
      );
      if (messages.length === 0) return { status: 'not_found' };
      if (input.pinned) {
        const count = await transaction.query<{ readonly count: number }>(
          `SELECT count(*)::int AS count FROM household_chat_message_pins WHERE household_id=$1 AND unpinned_at IS NULL`,
          [input.householdId],
        );
        if ((count[0]?.count ?? 0) >= 5) return { status: 'limit' };
        await transaction.query(
          `INSERT INTO household_chat_message_pins (id,household_id,message_id,pinned_by_membership_id,pinned_at)
           VALUES ($1,$2,$3,$4,$5) ON CONFLICT (message_id) WHERE unpinned_at IS NULL DO NOTHING`,
          [
            newUuidV7(Date.parse(input.occurredAt)),
            input.householdId,
            input.messageId,
            membership.membership_id,
            input.occurredAt,
          ],
        );
      } else {
        await transaction.query(
          `UPDATE household_chat_message_pins SET unpinned_by_membership_id=$3,unpinned_at=$4
           WHERE message_id=$1 AND household_id=$2 AND unpinned_at IS NULL`,
          [input.messageId, input.householdId, membership.membership_id, input.occurredAt],
        );
      }
      await transaction.query(
        `INSERT INTO audit_events (id,actor_user_id,household_id,action,target_type,target_id,outcome,occurred_at)
         VALUES ($1,$2,$3,$4,'household_chat_message',$5,'success',$6)`,
        [
          newUuidV7(),
          input.userId,
          input.householdId,
          input.pinned ? 'household_chat.message_pinned' : 'household_chat.message_unpinned',
          input.messageId,
          input.occurredAt,
        ],
      );
      const rows = await transaction.query<MessageRow>(`${messageSelect()} WHERE message.id=$1`, [
        input.messageId,
      ]);
      const details = await loadDetails(transaction, [input.messageId]);
      return { status: 'updated', message: mapMessage(requireRow(rows[0]), input.userId, details) };
    });
  }
}

async function findMembership(
  executor: SqlExecutor,
  userId: string,
  householdId: string,
): Promise<MembershipRow | null> {
  const rows = await executor.query<MembershipRow>(
    `SELECT membership.id AS membership_id,membership.user_id,membership.role,profile.display_name
     FROM household_memberships membership JOIN households household ON household.id=membership.household_id AND household.status='active'
     JOIN users account ON account.id=membership.user_id AND account.status='active'
     JOIN user_profiles profile ON profile.user_id=membership.user_id
     WHERE membership.household_id=$1 AND membership.user_id=$2 AND membership.status='active' LIMIT 1`,
    [householdId, userId],
  );
  return rows[0] ?? null;
}

function messageSelect(): string {
  return `SELECT message.id,message.household_id,message.sender_membership_id,sender.user_id AS sender_user_id,
          profile.display_name AS sender_display_name,message.body,message.message_kind,message.latitude,message.longitude,
          message.source_chat_message_id,message.source_calendar_event_id,message.created_at,
          active_pin.id AS pin_id,pinner_profile.display_name AS pinned_by_display_name
          FROM household_chat_messages message
          JOIN household_memberships sender ON sender.id=message.sender_membership_id
          JOIN user_profiles profile ON profile.user_id=sender.user_id
          LEFT JOIN household_chat_message_pins active_pin ON active_pin.message_id=message.id AND active_pin.unpinned_at IS NULL
          LEFT JOIN household_memberships pinner ON pinner.id=active_pin.pinned_by_membership_id
          LEFT JOIN user_profiles pinner_profile ON pinner_profile.user_id=pinner.user_id`;
}

async function loadDetails(executor: SqlExecutor, messageIds: readonly string[]) {
  const attachments =
    messageIds.length === 0
      ? []
      : await executor.query<AttachmentRow>(
          `SELECT id,message_id,media_type,byte_size,width,height FROM household_chat_attachments WHERE message_id=ANY($1::uuid[]) ORDER BY id`,
          [messageIds],
        );
  const mentions =
    messageIds.length === 0
      ? []
      : await executor.query<MentionRow>(
          `SELECT message_id,mentioned_user_id FROM household_chat_mentions WHERE message_id=ANY($1::uuid[]) ORDER BY mentioned_user_id`,
          [messageIds],
        );
  return { attachments, mentions };
}

function mapMessage(
  row: MessageRow,
  currentUserId: string,
  details: {
    readonly attachments: readonly AttachmentRow[];
    readonly mentions: readonly MentionRow[];
  },
): HouseholdChatMessage {
  return {
    id: row.id,
    householdId: row.household_id,
    senderMembershipId: row.sender_membership_id,
    senderUserId: row.sender_user_id,
    senderDisplayName: row.sender_display_name,
    isCurrentUser: row.sender_user_id === currentUserId,
    body: row.body,
    kind: row.message_kind,
    attachments: details.attachments
      .filter((item) => item.message_id === row.id)
      .map((item) => mapAttachment(item, row.household_id)),
    mentionedUserIds: details.mentions
      .filter((item) => item.message_id === row.id)
      .map((item) => item.mentioned_user_id),
    location:
      row.latitude === null || row.longitude === null
        ? null
        : { latitude: Number(row.latitude), longitude: Number(row.longitude) },
    isPinned: row.pin_id !== null,
    pinnedByDisplayName: row.pinned_by_display_name,
    sourceChatMessageId: row.source_chat_message_id,
    sourceCalendarEventId: row.source_calendar_event_id,
    createdAt: new Date(row.created_at).toISOString(),
  };
}

function mapAttachment(row: AttachmentRow, householdId: string): HouseholdChatAttachment {
  return {
    id: row.id,
    mediaType: row.media_type,
    byteSize: row.byte_size,
    width: row.width,
    height: row.height,
    downloadPath: `/v1/households/${householdId}/chat/messages/attachments/${row.id}`,
  };
}

async function validMentionTargets(
  executor: SqlExecutor,
  householdId: string,
  userIds: readonly string[],
): Promise<boolean> {
  if (userIds.length === 0) return true;
  const rows = await executor.query<{ readonly count: number }>(
    `SELECT count(DISTINCT user_id)::int AS count FROM household_memberships WHERE household_id=$1 AND status='active' AND user_id=ANY($2::uuid[])`,
    [householdId, userIds],
  );
  return (rows[0]?.count ?? 0) === userIds.length;
}

async function claimAttachments(
  executor: SqlExecutor,
  input: {
    readonly attachmentIds: readonly string[];
    readonly householdId: string;
    readonly userId: string;
    readonly occurredAt: string;
  },
  membershipId: string,
  messageId: string,
): Promise<boolean> {
  if (input.attachmentIds.length === 0) return true;
  const rows = await executor.query<{ readonly id: string }>(
    `UPDATE household_chat_attachments SET message_id=$1
     WHERE id=ANY($2::uuid[]) AND household_id=$3 AND uploaded_by_membership_id=$4 AND message_id IS NULL
       AND created_at >= ($5::timestamptz - interval '24 hours') RETURNING id`,
    [messageId, input.attachmentIds, input.householdId, membershipId, input.occurredAt],
  );
  return rows.length === input.attachmentIds.length;
}

async function insertAuditAndOutbox(
  transaction: SqlExecutor,
  input: {
    readonly userId: string;
    readonly householdId: string;
    readonly body: string;
    readonly mentionedUserIds: readonly string[];
    readonly mentionAll: boolean;
    readonly occurredAt: string;
  },
  messageId: string,
  membershipId: string,
) {
  await transaction.query(
    `INSERT INTO audit_events (id,actor_user_id,household_id,action,target_type,target_id,outcome,safe_details,occurred_at)
     VALUES ($1,$2,$3,'household_chat.message_created','household_chat_message',$4,'success',$5::jsonb,$6)`,
    [
      newUuidV7(),
      input.userId,
      input.householdId,
      messageId,
      JSON.stringify({
        characterCount: input.body.length,
        mentionCount: input.mentionedUserIds.length,
        mentionAll: input.mentionAll,
      }),
      input.occurredAt,
    ],
  );
  await transaction.query(
    `INSERT INTO outbox_events (id,event_type,aggregate_type,aggregate_id,household_id,actor_user_id,payload,occurred_at)
     VALUES ($1,'household_chat.message_created.v2','household_chat_message',$2,$3,$4,$5::jsonb,$6)`,
    [
      newUuidV7(),
      messageId,
      input.householdId,
      input.userId,
      JSON.stringify({
        messageId,
        senderMembershipId: membershipId,
        mentionedUserIds: input.mentionedUserIds,
        mentionAll: input.mentionAll,
      }),
      input.occurredAt,
    ],
  );
}

function requireRow<T>(row: T | undefined): T {
  if (row === undefined) throw new Error('Stored household chat response is unavailable.');
  return row;
}
function readMessage(value: unknown): HouseholdChatMessage {
  if (typeof value !== 'object' || value === null || !('id' in value) || !('body' in value))
    throw new Error('Stored household chat response is invalid.');
  return value as HouseholdChatMessage;
}
