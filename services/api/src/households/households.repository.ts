import { Injectable } from '@nestjs/common';
import type { HouseholdConfiguration, HouseholdSummary } from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';

interface HouseholdRow {
  readonly id: string;
  readonly name: string;
  readonly country_code: string;
  readonly timezone: string;
  readonly default_currency: string;
  readonly first_day_of_week: 1 | 6 | 7;
  readonly cycle_type: HouseholdConfiguration['cycleType'];
  readonly cycle_anchor: Date | string;
  readonly status: 'active';
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
  readonly role: HouseholdSummary['role'];
}

interface IdempotencyRow {
  readonly request_hash: string;
  readonly response_body: unknown;
}

export type HouseholdUpdateResult =
  | { readonly status: 'updated'; readonly household: HouseholdSummary }
  | { readonly status: 'not_found' | 'forbidden' | 'version_conflict' };

@Injectable()
export class HouseholdsRepository {
  constructor(private readonly database: DatabaseService) {}

  async createHousehold(input: {
    readonly userId: string;
    readonly configuration: HouseholdConfiguration;
    readonly idempotencyKey: string;
    readonly requestHash: string;
    readonly occurredAt: string;
  }): Promise<
    | { readonly status: 'created' | 'replayed'; readonly household: HouseholdSummary }
    | {
        readonly status: 'idempotency_conflict';
      }
  > {
    const householdId = newUuidV7(Date.parse(input.occurredAt));
    const membershipId = newUuidV7(Date.parse(input.occurredAt));
    const household: HouseholdSummary = {
      id: householdId,
      ...input.configuration,
      role: 'owner',
      status: 'active',
      version: 1,
      createdAt: input.occurredAt,
      updatedAt: input.occurredAt,
    };

    return this.database.transaction(async (transaction) => {
      const claimed = await transaction.query<{ readonly idempotency_key: string }>(
        `INSERT INTO idempotency_records (
           user_id, operation, idempotency_key, request_hash, response_status, response_body, created_at
         ) VALUES ($1, 'households.create', $2, $3, 201, $4::jsonb, $5)
         ON CONFLICT (user_id, operation, idempotency_key) DO NOTHING
         RETURNING idempotency_key`,
        [
          input.userId,
          input.idempotencyKey,
          input.requestHash,
          JSON.stringify(household),
          input.occurredAt,
        ],
      );

      if (claimed.length === 0) {
        const existingRows = await transaction.query<IdempotencyRow>(
          `SELECT request_hash, response_body
           FROM idempotency_records
           WHERE user_id = $1 AND operation = 'households.create' AND idempotency_key = $2`,
          [input.userId, input.idempotencyKey],
        );
        const existing = existingRows[0];
        if (existing?.request_hash !== input.requestHash) {
          return { status: 'idempotency_conflict' };
        }
        return { status: 'replayed', household: readHouseholdResponse(existing.response_body) };
      }

      await transaction.query(
        `INSERT INTO households (
           id, name, country_code, timezone, default_currency, first_day_of_week,
           cycle_type, cycle_anchor, status, created_by, created_at, updated_at, version
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'active', $9, $10, $10, 1)`,
        [
          householdId,
          input.configuration.name,
          input.configuration.countryCode,
          input.configuration.timezone,
          input.configuration.currency,
          input.configuration.firstDayOfWeek,
          input.configuration.cycleType,
          input.configuration.cycleAnchor,
          input.userId,
          input.occurredAt,
        ],
      );
      await transaction.query(
        `INSERT INTO household_memberships (
           id, household_id, user_id, role, status, joined_at
         ) VALUES ($1, $2, $3, 'owner', 'active', $4)`,
        [membershipId, householdId, input.userId, input.occurredAt],
      );
      await writeHouseholdEvents(transaction, {
        userId: input.userId,
        householdId,
        action: 'household.created',
        eventType: 'household.created.v1',
        payload: household,
        occurredAt: input.occurredAt,
      });

      return { status: 'created', household };
    });
  }

  async listForUser(userId: string): Promise<readonly HouseholdSummary[]> {
    const rows = await this.database.query<HouseholdRow>(
      `${householdSelect()}
       WHERE m.user_id = $1 AND m.status = 'active' AND h.status = 'active'
       ORDER BY h.created_at, h.id`,
      [userId],
    );
    return rows.map(mapHousehold);
  }

  async findForUser(userId: string, householdId: string): Promise<HouseholdSummary | null> {
    const rows = await this.database.query<HouseholdRow>(
      `${householdSelect()}
       WHERE m.user_id = $1 AND h.id = $2 AND m.status = 'active' AND h.status = 'active'
       LIMIT 1`,
      [userId, householdId],
    );
    return rows[0] === undefined ? null : mapHousehold(rows[0]);
  }

  async updateHousehold(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly expectedVersion: number;
    readonly configuration: HouseholdConfiguration;
    readonly occurredAt: string;
  }): Promise<HouseholdUpdateResult> {
    return this.database.transaction(async (transaction) => {
      const rows = await transaction.query<HouseholdRow>(
        `${householdSelect()}
         WHERE m.user_id = $1 AND h.id = $2 AND m.status = 'active' AND h.status = 'active'
         LIMIT 1
         FOR UPDATE`,
        [input.userId, input.householdId],
      );
      const current = rows[0];
      if (current === undefined) {
        return { status: 'not_found' };
      }
      if (current.role !== 'owner' && current.role !== 'admin') {
        return { status: 'forbidden' };
      }
      if (current.version !== input.expectedVersion) {
        return { status: 'version_conflict' };
      }

      const updatedRows = await transaction.query<HouseholdRow>(
        `UPDATE households
         SET name = $3,
             country_code = $4,
             timezone = $5,
             default_currency = $6,
             first_day_of_week = $7,
             cycle_type = $8,
             cycle_anchor = $9,
             version = version + 1,
             updated_at = $10
         WHERE id = $1 AND version = $2
         RETURNING
           id, name, country_code, timezone, default_currency, first_day_of_week,
           cycle_type, cycle_anchor, status, version, created_at, updated_at,
           $11::text AS role`,
        [
          input.householdId,
          input.expectedVersion,
          input.configuration.name,
          input.configuration.countryCode,
          input.configuration.timezone,
          input.configuration.currency,
          input.configuration.firstDayOfWeek,
          input.configuration.cycleType,
          input.configuration.cycleAnchor,
          input.occurredAt,
          current.role,
        ],
      );
      const updated = updatedRows[0];
      if (updated === undefined) {
        return { status: 'version_conflict' };
      }
      const household = mapHousehold(updated);
      await writeHouseholdEvents(transaction, {
        userId: input.userId,
        householdId: input.householdId,
        action: 'household.configuration_updated',
        eventType: 'household.configuration_updated.v1',
        payload: household,
        occurredAt: input.occurredAt,
      });
      return { status: 'updated', household };
    });
  }
}

function householdSelect(): string {
  return `SELECT
    h.id,
    h.name,
    h.country_code,
    h.timezone,
    h.default_currency,
    h.first_day_of_week,
    h.cycle_type,
    h.cycle_anchor,
    h.status,
    h.version,
    h.created_at,
    h.updated_at,
    m.role
  FROM households h
  JOIN household_memberships m ON m.household_id = h.id`;
}

function mapHousehold(row: HouseholdRow): HouseholdSummary {
  return {
    id: row.id,
    name: row.name,
    countryCode: row.country_code,
    timezone: row.timezone,
    currency: row.default_currency,
    firstDayOfWeek: row.first_day_of_week,
    cycleType: row.cycle_type,
    cycleAnchor: toLocalDate(row.cycle_anchor),
    role: row.role,
    status: row.status,
    version: row.version,
    createdAt: toInstant(row.created_at),
    updatedAt: toInstant(row.updated_at),
  };
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function toLocalDate(value: Date | string): string {
  if (typeof value === 'string') {
    return value.slice(0, 10);
  }
  return value.toISOString().slice(0, 10);
}

function readHouseholdResponse(value: unknown): HouseholdSummary {
  if (typeof value === 'string') {
    return JSON.parse(value) as HouseholdSummary;
  }
  return value as HouseholdSummary;
}

async function writeHouseholdEvents(
  transaction: SqlExecutor,
  input: {
    readonly userId: string;
    readonly householdId: string;
    readonly action: string;
    readonly eventType: string;
    readonly payload: HouseholdSummary;
    readonly occurredAt: string;
  },
): Promise<void> {
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, occurred_at
     ) VALUES ($1, $2, $3, $4, 'household', $3, 'success', $5)`,
    [newUuidV7(), input.userId, input.householdId, input.action, input.occurredAt],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id, payload, occurred_at
     ) VALUES ($1, $2, 'household', $3, $3, $4, $5::jsonb, $6)`,
    [
      newUuidV7(),
      input.eventType,
      input.householdId,
      input.userId,
      JSON.stringify(input.payload),
      input.occurredAt,
    ],
  );
}
