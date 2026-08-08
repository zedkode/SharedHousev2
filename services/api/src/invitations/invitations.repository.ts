import { Injectable } from '@nestjs/common';
import type {
  HouseholdInvitationPreview,
  HouseholdInvitationRole,
  HouseholdInvitationStatus,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';
import type {
  AcceptInvitationResult,
  CreateInvitationRecord,
  CreateInvitationResult,
  ListInvitationsResult,
  RevokeInvitationResult,
} from './invitations.types.js';

interface MembershipAuthorityRow {
  readonly household_name: string;
  readonly role: HouseholdSummary['role'];
}

interface InvitationRow {
  readonly id: string;
  readonly household_id: string;
  readonly household_name: string;
  readonly email_normalized: string | null;
  readonly role: HouseholdInvitationRole;
  readonly status: HouseholdInvitationStatus;
  readonly expires_at: Date | string;
  readonly created_at: Date | string;
  readonly accepted_by: string | null;
}

interface AcceptInvitationRow extends InvitationRow {
  readonly country_code: string;
  readonly timezone: string;
  readonly default_currency: string;
  readonly first_day_of_week: 1 | 6 | 7;
  readonly cycle_type: HouseholdSummary['cycleType'];
  readonly cycle_anchor: Date | string;
  readonly household_status: 'active' | 'closed';
  readonly version: number;
  readonly household_created_at: Date | string;
  readonly household_updated_at: Date | string;
}

@Injectable()
export class InvitationsRepository {
  constructor(private readonly database: DatabaseService) {}

  async create(input: CreateInvitationRecord): Promise<CreateInvitationResult> {
    return this.database.transaction(async (transaction) => {
      const authority = await findAuthority(
        transaction,
        input.actorUserId,
        input.householdId,
        true,
      );
      if (authority === null) return { status: 'not_found' };
      if (authority.role !== 'owner' && authority.role !== 'admin') {
        return { status: 'forbidden' };
      }
      if (authority.role === 'admin' && input.role === 'admin') {
        return { status: 'delegation_forbidden' };
      }

      await transaction.query(
        `INSERT INTO household_invitations (
           id, household_id, token_hash, email_normalized, role, status,
           invited_by, expires_at, created_at, updated_at
         ) VALUES ($1, $2, $3, $4, $5, 'pending', $6, $7, $8, $8)`,
        [
          input.invitationId,
          input.householdId,
          input.tokenHash,
          input.email,
          input.role,
          input.actorUserId,
          input.expiresAt,
          input.occurredAt,
        ],
      );
      await writeInvitationEvents(transaction, {
        action: 'household.invitation_created',
        eventType: 'household.invitation_created.v1',
        invitationId: input.invitationId,
        householdId: input.householdId,
        actorUserId: input.actorUserId,
        role: input.role,
        occurredAt: input.occurredAt,
      });
      return {
        status: 'created',
        invitation: {
          id: input.invitationId,
          householdId: input.householdId,
          householdName: authority.household_name,
          role: input.role,
          email: input.email,
          status: 'pending',
          expiresAt: input.expiresAt,
          createdAt: input.occurredAt,
          token: input.token,
        },
      };
    });
  }

  async list(
    actorUserId: string,
    householdId: string,
    now: string,
  ): Promise<ListInvitationsResult> {
    const authority = await findAuthority(this.database, actorUserId, householdId, false);
    if (authority === null) return { status: 'not_found' };
    if (authority.role !== 'owner' && authority.role !== 'admin') {
      return { status: 'forbidden' };
    }
    const rows = await this.database.query<InvitationRow>(
      `SELECT
         i.id, i.household_id, h.name AS household_name, i.email_normalized,
         i.role, i.status, i.expires_at, i.created_at, i.accepted_by
       FROM household_invitations i
       JOIN households h ON h.id = i.household_id
       WHERE i.household_id = $1
       ORDER BY i.created_at DESC, i.id DESC`,
      [householdId],
    );
    return {
      status: 'listed',
      invitations: rows.map((row) => mapInvitation(row, now)),
    };
  }

  async preview(tokenHash: string, now: string): Promise<HouseholdInvitationPreview | null> {
    const rows = await this.database.query<InvitationRow>(
      `SELECT
         i.id, i.household_id, h.name AS household_name, i.email_normalized,
         i.role, i.status, i.expires_at, i.created_at, i.accepted_by
       FROM household_invitations i
       JOIN households h ON h.id = i.household_id
       WHERE i.token_hash = $1
       LIMIT 1`,
      [tokenHash],
    );
    const row = rows[0];
    if (row === undefined) return null;
    const effectiveStatus = effectiveInvitationStatus(row, now);
    return {
      householdName: row.household_name,
      role: row.role,
      emailRestricted: row.email_normalized !== null,
      status:
        effectiveStatus === 'pending'
          ? 'pending'
          : effectiveStatus === 'expired'
            ? 'expired'
            : 'unavailable',
      expiresAt: toInstant(row.expires_at),
    };
  }

  async accept(input: {
    readonly tokenHash: string;
    readonly userId: string;
    readonly accountEmail: string;
    readonly occurredAt: string;
  }): Promise<AcceptInvitationResult> {
    return this.database.transaction(async (transaction) => {
      const rows = await transaction.query<AcceptInvitationRow>(
        `SELECT
           i.id, i.household_id, h.name AS household_name, i.email_normalized,
           i.role, i.status, i.expires_at, i.created_at, i.accepted_by,
           h.country_code, h.timezone, h.default_currency, h.first_day_of_week,
           h.cycle_type, h.cycle_anchor, h.status AS household_status, h.version,
           h.created_at AS household_created_at, h.updated_at AS household_updated_at
         FROM household_invitations i
         JOIN households h ON h.id = i.household_id
         WHERE i.token_hash = $1
         LIMIT 1
         FOR UPDATE OF i, h`,
        [input.tokenHash],
      );
      const invitation = rows[0];
      if (invitation === undefined) return { status: 'not_found' };
      if (invitation.household_status !== 'active') return { status: 'household_unavailable' };

      if (invitation.status === 'accepted' && invitation.accepted_by === input.userId) {
        const existingRole = await findActiveMembershipRole(
          transaction,
          invitation.household_id,
          input.userId,
        );
        return existingRole === null
          ? { status: 'unavailable' }
          : {
              status: 'accepted',
              household: mapAcceptedHousehold(invitation, existingRole),
            };
      }
      if (invitation.status !== 'pending') return { status: 'unavailable' };
      if (Date.parse(toInstant(invitation.expires_at)) <= Date.parse(input.occurredAt)) {
        await transaction.query(
          `UPDATE household_invitations
           SET status = 'expired', updated_at = $2
           WHERE id = $1 AND status = 'pending'`,
          [invitation.id, input.occurredAt],
        );
        return { status: 'expired' };
      }
      if (
        invitation.email_normalized !== null &&
        invitation.email_normalized !== input.accountEmail.trim().toLowerCase()
      ) {
        return { status: 'email_mismatch' };
      }

      const existingRole = await findActiveMembershipRole(
        transaction,
        invitation.household_id,
        input.userId,
      );
      const membershipRole = existingRole ?? invitation.role;
      if (existingRole === null) {
        await transaction.query(
          `INSERT INTO household_memberships (
             id, household_id, user_id, role, status, joined_at
           ) VALUES ($1, $2, $3, $4, 'active', $5)`,
          [
            newUuidV7(Date.parse(input.occurredAt)),
            invitation.household_id,
            input.userId,
            invitation.role,
            input.occurredAt,
          ],
        );
      }
      await transaction.query(
        `UPDATE household_invitations
         SET status = 'accepted', accepted_by = $2, accepted_at = $3, updated_at = $3
         WHERE id = $1 AND status = 'pending'`,
        [invitation.id, input.userId, input.occurredAt],
      );
      await writeInvitationEvents(transaction, {
        action: 'household.invitation_accepted',
        eventType: 'household.invitation_accepted.v1',
        invitationId: invitation.id,
        householdId: invitation.household_id,
        actorUserId: input.userId,
        role: membershipRole,
        occurredAt: input.occurredAt,
      });
      return {
        status: 'accepted',
        household: mapAcceptedHousehold(invitation, membershipRole),
      };
    });
  }

  async revoke(input: {
    readonly householdId: string;
    readonly invitationId: string;
    readonly actorUserId: string;
    readonly occurredAt: string;
  }): Promise<RevokeInvitationResult> {
    return this.database.transaction(async (transaction) => {
      const authority = await findAuthority(
        transaction,
        input.actorUserId,
        input.householdId,
        true,
      );
      if (authority === null) return { status: 'not_found' };
      if (authority.role !== 'owner' && authority.role !== 'admin') {
        return { status: 'forbidden' };
      }
      const rows = await transaction.query<{ readonly status: HouseholdInvitationStatus }>(
        `SELECT status
         FROM household_invitations
         WHERE id = $1 AND household_id = $2
         LIMIT 1
         FOR UPDATE`,
        [input.invitationId, input.householdId],
      );
      const current = rows[0];
      if (current === undefined) return { status: 'not_found' };
      if (current.status === 'accepted') return { status: 'already_accepted' };
      if (current.status === 'revoked' || current.status === 'expired') {
        return { status: 'revoked' };
      }
      await transaction.query(
        `UPDATE household_invitations
         SET status = 'revoked', revoked_at = $2, updated_at = $2
         WHERE id = $1 AND status = 'pending'`,
        [input.invitationId, input.occurredAt],
      );
      await writeInvitationEvents(transaction, {
        action: 'household.invitation_revoked',
        eventType: 'household.invitation_revoked.v1',
        invitationId: input.invitationId,
        householdId: input.householdId,
        actorUserId: input.actorUserId,
        role: null,
        occurredAt: input.occurredAt,
      });
      return { status: 'revoked' };
    });
  }
}

async function findAuthority(
  executor: SqlExecutor,
  actorUserId: string,
  householdId: string,
  lock: boolean,
): Promise<MembershipAuthorityRow | null> {
  const rows = await executor.query<MembershipAuthorityRow>(
    `SELECT h.name AS household_name, m.role
     FROM households h
     JOIN household_memberships m ON m.household_id = h.id
     WHERE h.id = $1 AND h.status = 'active'
       AND m.user_id = $2 AND m.status = 'active'
     LIMIT 1${lock ? ' FOR UPDATE OF h, m' : ''}`,
    [householdId, actorUserId],
  );
  return rows[0] ?? null;
}

async function findActiveMembershipRole(
  executor: SqlExecutor,
  householdId: string,
  userId: string,
): Promise<HouseholdSummary['role'] | null> {
  const rows = await executor.query<{ readonly role: HouseholdSummary['role'] }>(
    `SELECT role
     FROM household_memberships
     WHERE household_id = $1 AND user_id = $2 AND status = 'active'
     LIMIT 1`,
    [householdId, userId],
  );
  return rows[0]?.role ?? null;
}

function mapInvitation(row: InvitationRow, now: string) {
  return {
    id: row.id,
    householdId: row.household_id,
    householdName: row.household_name,
    role: row.role,
    email: row.email_normalized,
    status: effectiveInvitationStatus(row, now),
    expiresAt: toInstant(row.expires_at),
    createdAt: toInstant(row.created_at),
  } as const;
}

function effectiveInvitationStatus(
  row: Pick<InvitationRow, 'status' | 'expires_at'>,
  now: string,
): HouseholdInvitationStatus {
  return row.status === 'pending' && Date.parse(toInstant(row.expires_at)) <= Date.parse(now)
    ? 'expired'
    : row.status;
}

function mapAcceptedHousehold(
  row: AcceptInvitationRow,
  role: HouseholdSummary['role'],
): HouseholdSummary {
  return {
    id: row.household_id,
    name: row.household_name,
    countryCode: row.country_code,
    timezone: row.timezone,
    currency: row.default_currency,
    firstDayOfWeek: row.first_day_of_week,
    cycleType: row.cycle_type,
    cycleAnchor: toLocalDate(row.cycle_anchor),
    role,
    status: 'active',
    version: row.version,
    createdAt: toInstant(row.household_created_at),
    updatedAt: toInstant(row.household_updated_at),
  };
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function toLocalDate(value: Date | string): string {
  return typeof value === 'string' ? value.slice(0, 10) : value.toISOString().slice(0, 10);
}

async function writeInvitationEvents(
  transaction: SqlExecutor,
  input: {
    readonly action: string;
    readonly eventType: string;
    readonly invitationId: string;
    readonly householdId: string;
    readonly actorUserId: string;
    readonly role: HouseholdSummary['role'] | null;
    readonly occurredAt: string;
  },
): Promise<void> {
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, occurred_at
     ) VALUES ($1, $2, $3, $4, 'household_invitation', $5, 'success', $6)`,
    [
      newUuidV7(),
      input.actorUserId,
      input.householdId,
      input.action,
      input.invitationId,
      input.occurredAt,
    ],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id,
       payload, occurred_at
     ) VALUES ($1, $2, 'household_invitation', $3, $4, $5, $6::jsonb, $7)`,
    [
      newUuidV7(),
      input.eventType,
      input.invitationId,
      input.householdId,
      input.actorUserId,
      JSON.stringify({ invitationId: input.invitationId, role: input.role }),
      input.occurredAt,
    ],
  );
}
