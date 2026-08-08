import { Injectable } from '@nestjs/common';
import type { AccountSummary } from '@sharedhouse/contracts';

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
