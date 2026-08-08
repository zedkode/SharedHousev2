import { Injectable } from '@nestjs/common';
import type {
  AccountExport,
  AccountExportConsentRecord,
  AccountExportInvitation,
  AccountExportSession,
  AccountSummary,
  CalendarEventSummary,
  HouseholdSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { DatabaseService, type SqlExecutor } from '../database/database.service.js';
import type {
  AuthenticatedPrincipal,
  PendingVerificationAccount,
  RefreshRotationResult,
  RegistrationRecord,
  ReplacementVerificationRecord,
  SessionTokenRecord,
  UserCredentialRecord,
  VerificationResult,
} from './identity.types.js';

interface UserCredentialRow {
  readonly id: string;
  readonly email_normalized: string;
  readonly email_verified_at: Date | string | null;
  readonly status: UserCredentialRecord['status'];
  readonly preferred_locale: AccountSummary['preferredLocale'];
  readonly display_name: string;
  readonly algorithm: UserCredentialRecord['algorithm'];
  readonly salt_base64: string;
  readonly hash_base64: string;
}

interface PrincipalRow extends UserCredentialRow {
  readonly session_id: string;
  readonly family_id: string;
}

interface VerificationRow extends UserCredentialRow {
  readonly challenge_id: string;
  readonly expires_at: Date | string;
  readonly attempt_count: number;
}

interface SessionRow extends PrincipalRow {
  readonly refresh_expires_at: Date | string;
  readonly revoked_at: Date | string | null;
}

interface ConsumedRefreshRow {
  readonly session_id: string;
  readonly family_id: string;
}

interface PendingVerificationRow {
  readonly id: string;
  readonly preferred_locale: PendingVerificationAccount['preferredLocale'];
}

interface ChallengeCreatedAtRow {
  readonly created_at: Date | string;
}

interface OwnedHouseholdRow {
  readonly id: string;
  readonly active_member_count: number;
}

@Injectable()
export class IdentityRepository {
  constructor(private readonly database: DatabaseService) {}

  async createRegistration(record: RegistrationRecord): Promise<boolean> {
    return this.database.transaction(async (transaction) => {
      const inserted = await transaction.query<{ readonly id: string }>(
        `INSERT INTO users (
           id, email_normalized, status, preferred_locale, adult_confirmed_at, created_at, updated_at
         ) VALUES ($1, $2, 'pending_verification', $3, $4, $4, $4)
         ON CONFLICT (email_normalized) DO NOTHING
         RETURNING id`,
        [record.userId, record.email, record.preferredLocale, record.occurredAt],
      );

      if (inserted.length === 0) {
        return false;
      }

      await transaction.query(
        `INSERT INTO user_profiles (user_id, display_name)
         VALUES ($1, $2)`,
        [record.userId, record.displayName],
      );
      await transaction.query(
        `INSERT INTO password_credentials (
           user_id, algorithm, salt_base64, hash_base64, changed_at
         ) VALUES ($1, $2, $3, $4, $5)`,
        [
          record.userId,
          record.passwordAlgorithm,
          record.passwordSaltBase64,
          record.passwordHashBase64,
          record.occurredAt,
        ],
      );
      await transaction.query(
        `INSERT INTO email_verification_challenges (
           id, user_id, code_hash, expires_at, created_at
         ) VALUES ($1, $2, $3, $4, $5)`,
        [
          record.verificationChallengeId,
          record.userId,
          record.verificationCodeHash,
          record.verificationExpiresAt,
          record.occurredAt,
        ],
      );
      await insertVerificationEmail(transaction, record.verificationEmail);
      await transaction.query(
        `INSERT INTO consent_records (id, user_id, purpose, policy_version, granted, recorded_at)
         VALUES
           ($1, $2, 'terms_of_service', '2026-08-01', true, $3),
           ($4, $2, 'privacy_notice', '2026-08-01', true, $3),
           ($5, $2, 'marketing', '2026-08-01', $6, $3)`,
        [
          newUuidV7(),
          record.userId,
          record.occurredAt,
          newUuidV7(),
          newUuidV7(),
          record.marketingConsent,
        ],
      );
      await insertAudit(transaction, {
        actorUserId: record.userId,
        action: 'identity.account_registered',
        targetType: 'user',
        targetId: record.userId,
        occurredAt: record.occurredAt,
      });
      return true;
    });
  }

  async findPendingVerificationAccount(email: string): Promise<PendingVerificationAccount | null> {
    const rows = await this.database.query<PendingVerificationRow>(
      `SELECT id, preferred_locale
       FROM users
       WHERE email_normalized = $1 AND status = 'pending_verification'
       LIMIT 1`,
      [email],
    );
    const row = rows[0];
    return row === undefined ? null : { userId: row.id, preferredLocale: row.preferred_locale };
  }

  async replaceVerificationChallenge(record: ReplacementVerificationRecord): Promise<boolean> {
    return this.database.transaction(async (transaction) => {
      const users = await transaction.query<{ readonly id: string }>(
        `SELECT id
         FROM users
         WHERE id = $1 AND status = 'pending_verification'
         FOR UPDATE`,
        [record.userId],
      );
      if (users.length === 0) return false;

      const latest = await transaction.query<ChallengeCreatedAtRow>(
        `SELECT created_at
         FROM email_verification_challenges
         WHERE user_id = $1
         ORDER BY created_at DESC
         LIMIT 1`,
        [record.userId],
      );
      const latestCreatedAt = latest[0]?.created_at;
      if (
        latestCreatedAt !== undefined &&
        new Date(latestCreatedAt).getTime() > Date.parse(record.cooldownBefore)
      ) {
        return false;
      }

      await transaction.query(
        `UPDATE email_verification_challenges
         SET consumed_at = COALESCE(consumed_at, $2)
         WHERE user_id = $1 AND consumed_at IS NULL`,
        [record.userId, record.occurredAt],
      );
      await transaction.query(
        `INSERT INTO email_verification_challenges (
           id, user_id, code_hash, expires_at, created_at
         ) VALUES ($1, $2, $3, $4, $5)`,
        [
          record.verificationChallengeId,
          record.userId,
          record.verificationCodeHash,
          record.verificationExpiresAt,
          record.occurredAt,
        ],
      );
      await insertVerificationEmail(transaction, record.verificationEmail);
      await transaction.query(
        `INSERT INTO audit_events (
           id, actor_user_id, action, target_type, target_id, outcome, occurred_at
         ) VALUES ($1, NULL, 'identity.email_verification_resent', 'user', $2, 'success', $3)`,
        [newUuidV7(), record.userId, record.occurredAt],
      );
      return true;
    });
  }

  async findCredentialByEmail(email: string): Promise<UserCredentialRecord | null> {
    const rows = await this.database.query<UserCredentialRow>(
      credentialQuery('u.email_normalized = $1'),
      [email],
    );
    return rows[0] === undefined ? null : mapCredential(rows[0]);
  }

  async findCredentialByUserId(userId: string): Promise<UserCredentialRecord | null> {
    const rows = await this.database.query<UserCredentialRow>(credentialQuery('u.id = $1'), [
      userId,
    ]);
    return rows[0] === undefined ? null : mapCredential(rows[0]);
  }

  async exportAccount(userId: string, occurredAt: string): Promise<AccountExport> {
    return this.database.transaction(async (transaction) => {
      const credentials = await transaction.query<UserCredentialRow>(credentialQuery('u.id = $1'), [
        userId,
      ]);
      const credential = credentials[0];
      if (credential === undefined) throw new Error('Authenticated account disappeared.');
      const households = await transaction.query<ExportHouseholdRow>(
        `${exportHouseholdSelect()}
         WHERE m.user_id = $1 AND h.status = 'active'
         ORDER BY h.created_at, h.id`,
        [userId],
      );
      const events = await transaction.query<ExportCalendarRow>(
        `${exportCalendarSelect()}
         WHERE c.created_by_user_id = $1
         ORDER BY c.event_date, c.created_at, c.id`,
        [userId],
      );
      const consents = await transaction.query<ExportConsentRow>(
        `SELECT purpose, policy_version, granted, recorded_at
         FROM consent_records WHERE user_id = $1 ORDER BY recorded_at, id`,
        [userId],
      );
      const sessions = await transaction.query<ExportSessionRow>(
        `SELECT device_name, authenticated_at, last_seen_at, revoked_at
         FROM user_sessions WHERE user_id = $1 ORDER BY authenticated_at, id`,
        [userId],
      );
      const invitations = await transaction.query<ExportInvitationRow>(
        `SELECT id, household_id, role, email_normalized, status, expires_at, created_at
         FROM household_invitations
         WHERE invited_by = $1 OR accepted_by = $1
         ORDER BY created_at, id`,
        [userId],
      );
      const result: AccountExport = {
        formatVersion: '1',
        generatedAt: occurredAt,
        account: mapAccount(credential),
        households: households.map(mapExportHousehold),
        calendarEvents: events.map(mapExportCalendar),
        consentRecords: consents.map(mapExportConsent),
        sessions: sessions.map(mapExportSession),
        invitations: invitations.map(mapExportInvitation),
      };
      await transaction.query(
        `INSERT INTO privacy_requests (
           id, user_id, request_type, status, result_summary, requested_at, completed_at
         ) VALUES ($1, $2, 'export', 'completed', $3::jsonb, $4, $4)`,
        [
          newUuidV7(Date.parse(occurredAt)),
          userId,
          JSON.stringify({
            householdCount: households.length,
            calendarEventCount: events.length,
            consentRecordCount: consents.length,
            sessionCount: sessions.length,
            invitationCount: invitations.length,
          }),
          occurredAt,
        ],
      );
      await insertAudit(transaction, {
        actorUserId: userId,
        action: 'privacy.account_exported',
        targetType: 'user',
        targetId: userId,
        occurredAt,
      });
      return result;
    });
  }

  async deleteAccount(
    userId: string,
    occurredAt: string,
  ): Promise<
    | { readonly status: 'completed'; readonly closedHouseholdIds: readonly string[] }
    | { readonly status: 'owner_transfer_required'; readonly householdIds: readonly string[] }
  > {
    return this.database.transaction(async (transaction) => {
      const owned = await transaction.query<OwnedHouseholdRow>(
        `SELECT h.id,
           (SELECT count(*)::int FROM household_memberships active
            WHERE active.household_id = h.id AND active.status = 'active') AS active_member_count
         FROM households h
         JOIN household_memberships owner_membership ON owner_membership.household_id = h.id
         WHERE owner_membership.user_id = $1
           AND owner_membership.role = 'owner'
           AND owner_membership.status = 'active'
           AND h.status = 'active'
         ORDER BY h.id
         FOR UPDATE OF h`,
        [userId],
      );
      const blockedIds = owned.filter((row) => row.active_member_count > 1).map((row) => row.id);
      if (blockedIds.length > 0) {
        await transaction.query(
          `INSERT INTO privacy_requests (
             id, user_id, request_type, status, result_summary, requested_at
           ) VALUES ($1, $2, 'deletion', 'blocked', $3::jsonb, $4)`,
          [
            newUuidV7(Date.parse(occurredAt)),
            userId,
            JSON.stringify({ reason: 'owner_transfer_required' }),
            occurredAt,
          ],
        );
        return { status: 'owner_transfer_required', householdIds: blockedIds };
      }

      const closedHouseholdIds = owned.map((row) => row.id);
      if (closedHouseholdIds.length > 0) {
        await transaction.query(
          `UPDATE households SET status = 'closed', updated_at = $2, version = version + 1
           WHERE id = ANY($1::uuid[])`,
          [closedHouseholdIds, occurredAt],
        );
        await transaction.query(
          `UPDATE calendar_events
           SET status = 'deleted', deleted_at = $2, updated_at = $2, version = version + 1
           WHERE household_id = ANY($1::uuid[]) AND status = 'active'`,
          [closedHouseholdIds, occurredAt],
        );
        await transaction.query(
          `UPDATE household_invitations
           SET status = 'revoked', revoked_at = $2, updated_at = $2
           WHERE household_id = ANY($1::uuid[]) AND status = 'pending'`,
          [closedHouseholdIds, occurredAt],
        );
      }

      await transaction.query(
        `UPDATE household_memberships
         SET status = 'left', left_at = $2
         WHERE user_id = $1 AND status = 'active'`,
        [userId, occurredAt],
      );
      await transaction.query(
        `UPDATE user_sessions SET revoked_at = COALESCE(revoked_at, $2) WHERE user_id = $1`,
        [userId, occurredAt],
      );
      await transaction.query('DELETE FROM email_verification_challenges WHERE user_id = $1', [
        userId,
      ]);
      await transaction.query('DELETE FROM password_credentials WHERE user_id = $1', [userId]);
      await transaction.query('DELETE FROM idempotency_records WHERE user_id = $1', [userId]);
      await transaction.query(
        `UPDATE user_profiles SET display_name = 'Former member' WHERE user_id = $1`,
        [userId],
      );
      await transaction.query(
        `UPDATE users
         SET email_normalized = $2, email_verified_at = NULL, status = 'deleted', updated_at = $3
         WHERE id = $1`,
        [userId, `deleted-${userId}@deleted.sharedhouse.invalid`, occurredAt],
      );
      await transaction.query(
        `INSERT INTO privacy_requests (
           id, user_id, request_type, status, result_summary, requested_at, completed_at
         ) VALUES ($1, $2, 'deletion', 'completed', $3::jsonb, $4, $4)`,
        [
          newUuidV7(Date.parse(occurredAt)),
          userId,
          JSON.stringify({ closedHouseholdCount: closedHouseholdIds.length }),
          occurredAt,
        ],
      );
      await insertAudit(transaction, {
        actorUserId: userId,
        action: 'privacy.account_deleted',
        targetType: 'user',
        targetId: userId,
        occurredAt,
      });
      return { status: 'completed', closedHouseholdIds };
    });
  }

  async verifyEmail(
    email: string,
    codeHash: string,
    occurredAt: string,
  ): Promise<VerificationResult> {
    return this.database.transaction(async (transaction) => {
      const matches = await transaction.query<VerificationRow>(
        `${credentialSelect()},
           c.id AS challenge_id, c.expires_at, c.attempt_count
         FROM users u
         JOIN user_profiles p ON p.user_id = u.id
         JOIN password_credentials pc ON pc.user_id = u.id
         JOIN email_verification_challenges c ON c.user_id = u.id
         WHERE u.email_normalized = $1
           AND c.code_hash = $2
           AND c.consumed_at IS NULL
         ORDER BY c.created_at DESC
         LIMIT 1
         FOR UPDATE`,
        [email, codeHash],
      );
      const match = matches[0];

      if (match === undefined) {
        await transaction.query(
          `UPDATE email_verification_challenges
           SET attempt_count = LEAST(attempt_count + 1, 5),
               consumed_at = CASE WHEN attempt_count + 1 >= 5 THEN $2 ELSE consumed_at END
           WHERE id = (
             SELECT c.id
             FROM email_verification_challenges c
             JOIN users u ON u.id = c.user_id
             WHERE u.email_normalized = $1 AND c.consumed_at IS NULL
             ORDER BY c.created_at DESC
             LIMIT 1
           )`,
          [email, occurredAt],
        );
        return { status: 'invalid' };
      }

      if (
        match.attempt_count >= 5 ||
        new Date(match.expires_at).getTime() <= Date.parse(occurredAt)
      ) {
        await transaction.query(
          'UPDATE email_verification_challenges SET consumed_at = $2 WHERE id = $1',
          [match.challenge_id, occurredAt],
        );
        return { status: 'expired' };
      }

      await transaction.query(
        'UPDATE email_verification_challenges SET consumed_at = $2 WHERE id = $1',
        [match.challenge_id, occurredAt],
      );
      await transaction.query(
        `UPDATE users
         SET email_verified_at = COALESCE(email_verified_at, $2), status = 'active', updated_at = $2
         WHERE id = $1`,
        [match.id, occurredAt],
      );
      await insertAudit(transaction, {
        actorUserId: match.id,
        action: 'identity.email_verified',
        targetType: 'user',
        targetId: match.id,
        occurredAt,
      });

      return {
        status: 'verified',
        account: { ...mapAccount(match), emailVerified: true },
      };
    });
  }

  async createSession(record: SessionTokenRecord): Promise<void> {
    await this.database.query(
      `INSERT INTO user_sessions (
         id, family_id, user_id, device_name, access_token_hash, refresh_token_hash,
         access_expires_at, refresh_expires_at, authenticated_at, last_seen_at, created_at
       ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $9, $9)`,
      [
        record.sessionId,
        record.familyId,
        record.userId,
        record.deviceName,
        record.accessTokenHash,
        record.refreshTokenHash,
        record.accessExpiresAt,
        record.refreshExpiresAt,
        record.occurredAt,
      ],
    );
  }

  async findPrincipalByAccessTokenHash(
    accessTokenHash: string,
    occurredAt: string,
  ): Promise<AuthenticatedPrincipal | null> {
    const rows = await this.database.query<PrincipalRow>(
      `${principalSelect()}
       WHERE s.access_token_hash = $1
         AND s.revoked_at IS NULL
         AND s.access_expires_at > $2
         AND u.status = 'active'
         AND u.email_verified_at IS NOT NULL`,
      [accessTokenHash, occurredAt],
    );
    const row = rows[0];
    return row === undefined ? null : mapPrincipal(row);
  }

  async rotateRefreshToken(input: {
    readonly currentRefreshTokenHash: string;
    readonly nextAccessTokenHash: string;
    readonly nextRefreshTokenHash: string;
    readonly accessExpiresAt: string;
    readonly refreshExpiresAt: string;
    readonly occurredAt: string;
  }): Promise<RefreshRotationResult> {
    return this.database.transaction(async (transaction) => {
      const sessions = await transaction.query<SessionRow>(
        `${principalSelect('s.refresh_expires_at, s.revoked_at')}
         WHERE s.refresh_token_hash = $1
         LIMIT 1
         FOR UPDATE`,
        [input.currentRefreshTokenHash],
      );
      const session = sessions[0];

      if (session === undefined) {
        const consumed = await transaction.query<ConsumedRefreshRow>(
          `SELECT session_id, family_id
           FROM consumed_refresh_tokens
           WHERE token_hash = $1
           LIMIT 1`,
          [input.currentRefreshTokenHash],
        );
        const replay = consumed[0];
        if (replay !== undefined) {
          await transaction.query(
            `UPDATE user_sessions
             SET revoked_at = COALESCE(revoked_at, $2)
             WHERE family_id = $1`,
            [replay.family_id, input.occurredAt],
          );
          return { status: 'reused' };
        }
        return { status: 'invalid' };
      }

      if (
        session.revoked_at !== null ||
        new Date(session.refresh_expires_at).getTime() <= Date.parse(input.occurredAt) ||
        session.status !== 'active'
      ) {
        return { status: 'invalid' };
      }

      await transaction.query(
        `INSERT INTO consumed_refresh_tokens (token_hash, session_id, family_id, consumed_at)
         VALUES ($1, $2, $3, $4)`,
        [input.currentRefreshTokenHash, session.session_id, session.family_id, input.occurredAt],
      );
      await transaction.query(
        `UPDATE user_sessions
         SET access_token_hash = $2,
             refresh_token_hash = $3,
             access_expires_at = $4,
             refresh_expires_at = $5,
             last_seen_at = $6
         WHERE id = $1`,
        [
          session.session_id,
          input.nextAccessTokenHash,
          input.nextRefreshTokenHash,
          input.accessExpiresAt,
          input.refreshExpiresAt,
          input.occurredAt,
        ],
      );

      return { status: 'rotated', principal: mapPrincipal(session) };
    });
  }

  async revokeSession(accessTokenHash: string, occurredAt: string): Promise<void> {
    await this.database.query(
      `UPDATE user_sessions
       SET revoked_at = COALESCE(revoked_at, $2)
       WHERE access_token_hash = $1`,
      [accessTokenHash, occurredAt],
    );
  }
}

interface ExportHouseholdRow {
  readonly id: string;
  readonly name: string;
  readonly country_code: string;
  readonly timezone: string;
  readonly default_currency: string;
  readonly first_day_of_week: 1 | 6 | 7;
  readonly cycle_type: HouseholdSummary['cycleType'];
  readonly cycle_anchor: Date | string;
  readonly status: 'active';
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
  readonly role: HouseholdSummary['role'];
}

interface ExportCalendarRow {
  readonly id: string;
  readonly household_id: string;
  readonly title: string;
  readonly description: string | null;
  readonly event_type: CalendarEventSummary['type'];
  readonly event_date: Date | string;
  readonly start_time: string | null;
  readonly end_time: string | null;
  readonly reminder_minutes_before: number | null;
  readonly created_by_user_id: string;
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
}

interface ExportConsentRow {
  readonly purpose: string;
  readonly policy_version: string;
  readonly granted: boolean;
  readonly recorded_at: Date | string;
}

interface ExportSessionRow {
  readonly device_name: string;
  readonly authenticated_at: Date | string;
  readonly last_seen_at: Date | string;
  readonly revoked_at: Date | string | null;
}

interface ExportInvitationRow {
  readonly id: string;
  readonly household_id: string;
  readonly role: AccountExportInvitation['role'];
  readonly email_normalized: string | null;
  readonly status: AccountExportInvitation['status'];
  readonly expires_at: Date | string;
  readonly created_at: Date | string;
}

function exportHouseholdSelect(): string {
  return `SELECT h.id, h.name, h.country_code, h.timezone, h.default_currency,
    h.first_day_of_week, h.cycle_type, h.cycle_anchor, h.status, h.version,
    h.created_at, h.updated_at, m.role
    FROM households h JOIN household_memberships m ON m.household_id = h.id`;
}

function exportCalendarSelect(): string {
  return `SELECT c.id, c.household_id, c.title, c.description, c.event_type, c.event_date,
    c.start_time, c.end_time, c.reminder_minutes_before, c.created_by_user_id,
    c.version, c.created_at, c.updated_at FROM calendar_events c`;
}

function mapExportHousehold(row: ExportHouseholdRow): HouseholdSummary {
  return {
    id: row.id,
    name: row.name,
    countryCode: row.country_code,
    timezone: row.timezone,
    currency: row.default_currency,
    firstDayOfWeek: row.first_day_of_week,
    cycleType: row.cycle_type,
    cycleAnchor: toDate(row.cycle_anchor),
    role: row.role,
    status: row.status,
    version: row.version,
    createdAt: toInstant(row.created_at),
    updatedAt: toInstant(row.updated_at),
  };
}

function mapExportCalendar(row: ExportCalendarRow): CalendarEventSummary {
  return {
    id: row.id,
    householdId: row.household_id,
    title: row.title,
    description: row.description,
    type: row.event_type,
    date: toDate(row.event_date),
    startTime: row.start_time?.slice(0, 5) ?? null,
    endTime: row.end_time?.slice(0, 5) ?? null,
    reminderMinutesBefore: row.reminder_minutes_before,
    createdByUserId: row.created_by_user_id,
    version: row.version,
    createdAt: toInstant(row.created_at),
    updatedAt: toInstant(row.updated_at),
  };
}

function mapExportConsent(row: ExportConsentRow): AccountExportConsentRecord {
  return {
    purpose: row.purpose,
    policyVersion: row.policy_version,
    granted: row.granted,
    recordedAt: toInstant(row.recorded_at),
  };
}

function mapExportSession(row: ExportSessionRow): AccountExportSession {
  return {
    deviceName: row.device_name,
    authenticatedAt: toInstant(row.authenticated_at),
    lastSeenAt: toInstant(row.last_seen_at),
    revokedAt: row.revoked_at === null ? null : toInstant(row.revoked_at),
  };
}

function mapExportInvitation(row: ExportInvitationRow): AccountExportInvitation {
  return {
    id: row.id,
    householdId: row.household_id,
    role: row.role,
    email: row.email_normalized,
    status: row.status,
    expiresAt: toInstant(row.expires_at),
    createdAt: toInstant(row.created_at),
  };
}

function toInstant(value: Date | string): string {
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function toDate(value: Date | string): string {
  return value instanceof Date ? value.toISOString().slice(0, 10) : String(value).slice(0, 10);
}

function credentialSelect(): string {
  return `SELECT
    u.id,
    u.email_normalized,
    u.email_verified_at,
    u.status,
    u.preferred_locale,
    p.display_name,
    pc.algorithm,
    pc.salt_base64,
    pc.hash_base64`;
}

function credentialQuery(whereClause: string): string {
  return `${credentialSelect()}
    FROM users u
    JOIN user_profiles p ON p.user_id = u.id
    JOIN password_credentials pc ON pc.user_id = u.id
    WHERE ${whereClause}
    LIMIT 1`;
}

function principalSelect(additionalFields?: string): string {
  const suffix = additionalFields === undefined ? '' : `, ${additionalFields}`;
  return `${credentialSelect()}, s.id AS session_id, s.family_id${suffix}
    FROM user_sessions s
    JOIN users u ON u.id = s.user_id
    JOIN user_profiles p ON p.user_id = u.id
    JOIN password_credentials pc ON pc.user_id = u.id`;
}

function mapCredential(row: UserCredentialRow): UserCredentialRecord {
  return {
    account: mapAccount(row),
    status: row.status,
    algorithm: row.algorithm,
    saltBase64: row.salt_base64,
    hashBase64: row.hash_base64,
  };
}

function mapAccount(row: UserCredentialRow): AccountSummary {
  return {
    id: row.id,
    email: row.email_normalized,
    emailVerified: row.email_verified_at !== null,
    displayName: row.display_name,
    preferredLocale: row.preferred_locale,
  };
}

function mapPrincipal(row: PrincipalRow): AuthenticatedPrincipal {
  return {
    sessionId: row.session_id,
    familyId: row.family_id,
    userId: row.id,
    account: mapAccount(row),
  };
}

async function insertAudit(
  transaction: SqlExecutor,
  input: {
    readonly actorUserId: string;
    readonly action: string;
    readonly targetType: string;
    readonly targetId: string;
    readonly occurredAt: string;
  },
): Promise<void> {
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, action, target_type, target_id, outcome, occurred_at
     ) VALUES ($1, $2, $3, $4, $5, 'success', $6)`,
    [
      newUuidV7(),
      input.actorUserId,
      input.action,
      input.targetType,
      input.targetId,
      input.occurredAt,
    ],
  );
}

async function insertVerificationEmail(
  transaction: SqlExecutor,
  email: RegistrationRecord['verificationEmail'],
): Promise<void> {
  if (email === undefined) return;
  await transaction.query(
    `INSERT INTO verification_email_outbox (
       id, challenge_id, recipient_email, locale, code_ciphertext_base64,
       code_iv_base64, code_auth_tag_base64, expires_at, available_at,
       created_at, updated_at
     ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $9, $9)`,
    [
      email.outboxId,
      email.challengeId,
      email.recipientEmail,
      email.locale,
      email.codeCiphertextBase64,
      email.codeIvBase64,
      email.codeAuthTagBase64,
      email.expiresAt,
      email.occurredAt,
    ],
  );
}
