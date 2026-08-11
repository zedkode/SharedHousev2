import { Injectable } from '@nestjs/common';
import type {
  BillingCoupleConfiguration,
  BillingCoupleSummary,
  BillingRosterSummary,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface MembershipRow {
  readonly id: string;
  readonly user_id: string;
  readonly display_name: string;
  readonly role: HouseholdSummary['role'];
}

interface RosterRow {
  readonly version: number;
  readonly updated_at: Date | string;
}

interface CoupleRow {
  readonly id: string;
  readonly primary_membership_id: string;
  readonly primary_display_name: string;
  readonly partner_membership_id: string | null;
  readonly partner_display_name: string;
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type BillingRosterMutationResult =
  | { readonly status: 'updated' | 'replayed'; readonly roster: BillingRosterSummary }
  | {
      readonly status:
        'not_found' | 'forbidden' | 'version_conflict' | 'invalid_roster' | 'idempotency_conflict';
    };

@Injectable()
export class BillingRosterRepository {
  constructor(private readonly database: DatabaseService) {}

  async get(userId: string, householdId: string): Promise<BillingRosterSummary | null> {
    const members = await listActiveMembers(this.database, householdId);
    const caller = members.find((member) => member.user_id === userId);
    if (caller === undefined) return null;
    return readBoard(this.database, householdId, members, caller, null);
  }

  async replace(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly expectedVersion: number;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly couples: readonly BillingCoupleConfiguration[];
    readonly occurredAt: string;
  }): Promise<BillingRosterMutationResult> {
    return this.database.transaction(async (transaction) => {
      const members = await listActiveMembers(transaction, input.householdId);
      const caller = members.find((member) => member.user_id === input.userId);
      if (caller === undefined) return { status: 'not_found' };
      if (!isManager(caller.role)) return { status: 'forbidden' };

      const replayRows = await transaction.query<IdempotencyRow>(
        `SELECT request_hash, response_body FROM idempotency_records
         WHERE user_id = $1 AND operation = 'billing_roster.replace' AND idempotency_key = $2`,
        [input.userId, input.idempotencyKey],
      );
      const replay = replayRows[0];
      if (replay !== undefined) {
        if (replay.request_hash !== input.requestHash) return { status: 'idempotency_conflict' };
        return { status: 'replayed', roster: readStoredBoard(replay.response_body) };
      }

      await transaction.query(
        `INSERT INTO household_billing_rosters (
           household_id, version, updated_by_membership_id, created_at, updated_at
         ) VALUES ($1, 1, $2, $3, $3)
         ON CONFLICT (household_id) DO NOTHING`,
        [input.householdId, caller.id, input.occurredAt],
      );
      const roster = (
        await transaction.query<RosterRow>(
          `SELECT version, updated_at FROM household_billing_rosters
           WHERE household_id = $1 FOR UPDATE`,
          [input.householdId],
        )
      )[0];
      if (roster === undefined) return { status: 'not_found' };
      if (roster.version !== input.expectedVersion) return { status: 'version_conflict' };

      const memberById = new Map(members.map((member) => [member.id, member]));
      const usedMembershipIds = new Set<string>();
      const resolved: {
        configuration: BillingCoupleConfiguration;
        primary: MembershipRow;
        partner: MembershipRow | null;
        guestName: string | null;
      }[] = [];
      for (const configuration of input.couples) {
        const primary = memberById.get(configuration.primaryMembershipId);
        const partner = configuration.partnerMembershipId
          ? memberById.get(configuration.partnerMembershipId)
          : undefined;
        const guestName = configuration.partnerDisplayName?.trim() ?? null;
        if (
          primary === undefined ||
          (configuration.partnerMembershipId != null && partner === undefined) ||
          (partner === undefined) === (guestName === null) ||
          partner?.id === primary.id ||
          usedMembershipIds.has(primary.id) ||
          (partner !== undefined && usedMembershipIds.has(partner.id))
        ) {
          return { status: 'invalid_roster' };
        }
        usedMembershipIds.add(primary.id);
        if (partner !== undefined) usedMembershipIds.add(partner.id);
        resolved.push({ configuration, primary, partner: partner ?? null, guestName });
      }

      await transaction.query(
        `UPDATE household_billing_couples
         SET status = 'archived', version = version + 1, archived_by_membership_id = $2,
             archived_at = $3, updated_at = $3
         WHERE household_id = $1 AND status = 'active'`,
        [input.householdId, caller.id, input.occurredAt],
      );
      for (const couple of resolved) {
        await transaction.query(
          `INSERT INTO household_billing_couples (
             id, household_id, primary_membership_id, partner_membership_id,
             partner_display_name, status, version, created_by_membership_id,
             created_at, updated_at
           ) VALUES ($1, $2, $3, $4, $5, 'active', 1, $6, $7, $7)`,
          [
            newUuidV7(),
            input.householdId,
            couple.primary.id,
            couple.partner?.id ?? null,
            couple.guestName,
            caller.id,
            input.occurredAt,
          ],
        );
      }

      await transaction.query(
        `UPDATE household_billing_rosters
         SET version = version + 1, updated_by_membership_id = $2, updated_at = $3
         WHERE household_id = $1 AND version = $4`,
        [input.householdId, caller.id, input.occurredAt, input.expectedVersion],
      );
      const board = await readBoard(
        transaction,
        input.householdId,
        members,
        caller,
        input.expectedVersion + 1,
      );

      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (
           user_id, operation, idempotency_key, request_hash, response_status, response_body, created_at
         ) VALUES ($1, 'billing_roster.replace', $2, $3, 200, $4::jsonb, $5)
         ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
         RETURNING idempotency_key`,
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify(board),
          input.occurredAt,
        ],
      );
      if (claimed.length === 0) return { status: 'idempotency_conflict' };

      await transaction.query(
        `INSERT INTO audit_events (
           id, actor_user_id, household_id, action, target_type, target_id,
           outcome, safe_details, occurred_at
         ) VALUES ($1, $2, $3, 'ledger.billing_roster_replaced', 'billing_roster', $3,
           'success', $4::jsonb, $5)`,
        [
          newUuidV7(),
          input.userId,
          input.householdId,
          JSON.stringify({ coupleCount: board.couples.length, residentCount: board.residentCount }),
          input.occurredAt,
        ],
      );
      await transaction.query(
        `INSERT INTO outbox_events (
           id, event_type, aggregate_type, aggregate_id, household_id,
           actor_user_id, payload, occurred_at
         ) VALUES ($1, 'ledger.billing_roster_replaced.v1', 'billing_roster', $2, $2,
           $3, $4::jsonb, $5)`,
        [newUuidV7(), input.householdId, input.userId, JSON.stringify(board), input.occurredAt],
      );
      return { status: 'updated', roster: board };
    });
  }
}

async function readBoard(
  executor: SqlExecutor,
  householdId: string,
  members: readonly MembershipRow[],
  caller: MembershipRow,
  forcedVersion: number | null,
): Promise<BillingRosterSummary> {
  const roster = (
    await executor.query<RosterRow>(
      `SELECT version, updated_at FROM household_billing_rosters WHERE household_id = $1`,
      [householdId],
    )
  )[0];
  const couples = await executor.query<CoupleRow>(
    `SELECT couple.id, couple.primary_membership_id,
       primary_profile.display_name AS primary_display_name,
       couple.partner_membership_id,
       COALESCE(partner_profile.display_name, couple.partner_display_name) AS partner_display_name
     FROM household_billing_couples couple
     JOIN household_memberships primary_member ON primary_member.id = couple.primary_membership_id
     JOIN user_profiles primary_profile ON primary_profile.user_id = primary_member.user_id
     LEFT JOIN household_memberships partner_member ON partner_member.id = couple.partner_membership_id
     LEFT JOIN user_profiles partner_profile ON partner_profile.user_id = partner_member.user_id
     WHERE couple.household_id = $1 AND couple.status = 'active'
     ORDER BY lower(primary_profile.display_name), couple.id`,
    [householdId],
  );
  const summaries: BillingCoupleSummary[] = couples.map((couple) => ({
    id: couple.id,
    primaryMembershipId: couple.primary_membership_id,
    primaryDisplayName: couple.primary_display_name,
    partnerMembershipId: couple.partner_membership_id,
    partnerDisplayName: couple.partner_display_name,
  }));
  return {
    householdId,
    members: members.map((member) => ({
      membershipId: member.id,
      displayName: member.display_name,
      isCurrentUser: member.user_id === caller.user_id,
    })),
    couples: summaries,
    residentCount:
      members.length + summaries.filter((couple) => couple.partnerMembershipId === null).length,
    billingUnitCount:
      members.length - summaries.filter((couple) => couple.partnerMembershipId !== null).length,
    canManage: isManager(caller.role),
    version: forcedVersion ?? roster?.version ?? 1,
    updatedAt: roster === undefined ? new Date(0).toISOString() : toInstant(roster.updated_at),
  };
}

async function listActiveMembers(
  executor: SqlExecutor,
  householdId: string,
): Promise<readonly MembershipRow[]> {
  return executor.query<MembershipRow>(
    `SELECT membership.id, membership.user_id, profile.display_name, membership.role
     FROM household_memberships membership
     JOIN households household ON household.id = membership.household_id AND household.status = 'active'
     JOIN users account ON account.id = membership.user_id AND account.status = 'active'
     JOIN user_profiles profile ON profile.user_id = membership.user_id
     WHERE membership.household_id = $1 AND membership.status = 'active'
     ORDER BY lower(profile.display_name), membership.joined_at, membership.id`,
    [householdId],
  );
}

function isManager(role: HouseholdSummary['role']): boolean {
  return role === 'owner' || role === 'admin';
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function readStoredBoard(value: unknown): BillingRosterSummary {
  return (typeof value === 'string' ? JSON.parse(value) : value) as BillingRosterSummary;
}
