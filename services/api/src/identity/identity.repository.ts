import { Injectable } from '@nestjs/common';
import type {
  AccountExport,
  AccountExportConsentRecord,
  AccountExportInvitation,
  AccountExportSession,
  AccountSummary,
  CalendarEventSummary,
  ExpenseSummary,
  ExpensePaymentSummary,
  ExpenseTemplateSummary,
  HouseholdSummary,
  HouseholdTaskRecurrenceCadence,
  HouseholdTaskSummary,
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
  readonly successor_user_id: string | null;
  readonly successor_membership_id: string | null;
  readonly successor_previous_role: 'admin' | 'member' | null;
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
      const taskRows = await transaction.query<ExportTaskRow>(
        `SELECT t.id,t.household_id,t.assignee_membership_id,assignee_profile.display_name AS assignee_display_name,
           t.title,t.instructions,t.zone,t.priority,t.due_date,t.due_time,t.estimated_minutes,
           t.recurrence_cadence,t.recurrence_ends_on,t.series_id,t.occurrence_date,
           t.recurrence_completed,t.status,
           t.completion_note,t.completed_by_user_id,t.completed_at,t.version,t.created_at,t.updated_at,
           r.id AS request_id,r.request_type,r.status AS request_status,r.reason AS request_reason,
           r.requested_assignee_membership_id,r.requested_due_date,r.requested_due_time,
           r.created_by_membership_id,requester_profile.display_name AS request_created_by_display_name,
           r.resolved_by_user_id,resolver_profile.display_name AS resolved_by_display_name,
           r.resolution_note,r.resolved_at,r.created_at AS request_created_at
         FROM household_tasks t
         JOIN household_memberships assignee ON assignee.id=t.assignee_membership_id
         JOIN user_profiles assignee_profile ON assignee_profile.user_id=assignee.user_id
         LEFT JOIN household_task_requests r ON r.task_id=t.id
         LEFT JOIN household_memberships requester ON requester.id=r.created_by_membership_id
         LEFT JOIN user_profiles requester_profile ON requester_profile.user_id=requester.user_id
         LEFT JOIN user_profiles resolver_profile ON resolver_profile.user_id=r.resolved_by_user_id
         WHERE t.created_by_user_id=$1 OR assignee.user_id=$1
         ORDER BY t.due_date,t.id,r.created_at,r.id`,
        [userId],
      );
      const expenseRows = await transaction.query<ExportExpenseRow>(
        `SELECT e.id, e.household_id, creator.user_id AS created_by_user_id, e.title, e.supplier_name,
           e.category, e.custom_category_name, e.amount_minor, e.currency, e.due_date, e.notes,
           e.source_template_id, e.occurrence_date, e.revision_of_expense_id,
           e.superseded_by_expense_id, e.split_method,
            e.status, e.version, e.created_at, e.updated_at, a.id AS allocation_id,
            a.membership_id, a.billing_unit_label AS display_name,
            a.billing_unit_type, a.participant_count,
            eligible.membership_ids AS allocation_eligible_membership_ids,
            eligible.user_ids AS allocation_eligible_user_ids,
            a.amount_minor AS allocation_amount_minor, a.rounding_adjustment_minor,
           p.id AS payment_id, p.amount_minor AS payment_amount_minor, p.method AS payment_method,
           p.payment_reference, p.note AS payment_note, p.paid_at AS payment_paid_at,
           p.status AS payment_status, declared.user_id AS payment_declared_by_user_id,
           declared_profile.display_name AS payment_declared_by_display_name,
           confirmed.user_id AS payment_confirmed_by_user_id,
           confirmed_profile.display_name AS payment_confirmed_by_display_name,
           p.confirmed_at AS payment_confirmed_at,
           disputed.user_id AS payment_disputed_by_user_id,
           disputed_profile.display_name AS payment_disputed_by_display_name,
           p.disputed_at AS payment_disputed_at,
           p.dispute_reason AS payment_dispute_reason,
           reversed.user_id AS payment_reversed_by_user_id,
           reversed_profile.display_name AS payment_reversed_by_display_name,
           p.reversed_at AS payment_reversed_at,
           p.reversal_reason AS payment_reversal_reason, p.version AS payment_version,
           p.created_at AS payment_created_at, p.updated_at AS payment_updated_at
         FROM expenses e
         JOIN household_memberships creator ON creator.id = e.created_by_membership_id
         JOIN expense_allocations a ON a.expense_id = e.id
          JOIN LATERAL (
            SELECT array_agg(allocation_member.membership_id ORDER BY allocation_member.membership_id) AS membership_ids,
              array_agg(member.user_id ORDER BY allocation_member.membership_id) AS user_ids
            FROM expense_allocation_members allocation_member
            JOIN household_memberships member ON member.id = allocation_member.membership_id
            WHERE allocation_member.allocation_id = a.id
          ) eligible ON true
         LEFT JOIN expense_payment_declarations p ON p.allocation_id = a.id
         LEFT JOIN household_memberships declared ON declared.id = p.declared_by_membership_id
         LEFT JOIN user_profiles declared_profile ON declared_profile.user_id = declared.user_id
         LEFT JOIN household_memberships confirmed ON confirmed.id = p.confirmed_by_membership_id
         LEFT JOIN user_profiles confirmed_profile ON confirmed_profile.user_id = confirmed.user_id
         LEFT JOIN household_memberships disputed ON disputed.id = p.disputed_by_membership_id
         LEFT JOIN user_profiles disputed_profile ON disputed_profile.user_id = disputed.user_id
         LEFT JOIN household_memberships reversed ON reversed.id = p.reversed_by_membership_id
         LEFT JOIN user_profiles reversed_profile ON reversed_profile.user_id = reversed.user_id
         WHERE creator.user_id = $1 OR EXISTS (
            SELECT 1 FROM expense_allocations mine
            JOIN expense_allocation_members mine_link ON mine_link.allocation_id = mine.id
            JOIN household_memberships my_membership ON my_membership.id = mine_link.membership_id
            WHERE mine.expense_id = e.id AND my_membership.user_id = $1
         )
          ORDER BY e.due_date, e.id, a.id, p.created_at, p.id`,
        [userId],
      );
      const expenseTemplates = await transaction.query<ExportExpenseTemplateRow>(
        `SELECT t.id, t.household_id, t.title, t.category, t.custom_category_name,
           t.amount_minor, t.currency, t.cadence, t.next_due_date, t.schedule_ends_on,
           t.notes, t.status,
           t.version, t.created_at, t.updated_at
         FROM expense_templates t
         WHERE EXISTS (
           SELECT 1 FROM household_memberships m
           WHERE m.household_id = t.household_id AND m.user_id = $1 AND m.status = 'active'
         )
         ORDER BY t.household_id, t.next_due_date, t.id`,
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
        householdTasks: mapExportTasks(taskRows),
        expenses: mapExportExpenses(expenseRows, userId),
        expenseTemplates: expenseTemplates.map(mapExportExpenseTemplate),
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
            householdTaskCount: new Set(taskRows.map((row) => row.id)).size,
            expenseCount: new Set(expenseRows.map((row) => row.id)).size,
            expenseTemplateCount: expenseTemplates.length,
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
    | {
        readonly status: 'completed';
        readonly closedHouseholdIds: readonly string[];
        readonly transferredHouseholdIds: readonly string[];
      }
    | { readonly status: 'owner_transfer_required'; readonly householdIds: readonly string[] }
  > {
    return this.database.transaction(async (transaction) => {
      const owned = await transaction.query<OwnedHouseholdRow>(
        `SELECT h.id,
           (SELECT count(*)::int FROM household_memberships active
            WHERE active.household_id = h.id AND active.status = 'active') AS active_member_count,
           successor.user_id AS successor_user_id,
           successor.id AS successor_membership_id,
           successor.role AS successor_previous_role
         FROM households h
         JOIN household_memberships owner_membership ON owner_membership.household_id = h.id
         LEFT JOIN LATERAL (
           SELECT candidate.id, candidate.user_id, candidate.role
           FROM household_memberships candidate
           WHERE candidate.household_id = h.id
             AND candidate.user_id <> $1
             AND candidate.status = 'active'
             AND candidate.role IN ('admin', 'member')
           ORDER BY CASE candidate.role WHEN 'admin' THEN 0 ELSE 1 END,
                    candidate.joined_at, candidate.user_id
           LIMIT 1
         ) successor ON true
         WHERE owner_membership.user_id = $1
           AND owner_membership.role = 'owner'
           AND owner_membership.status = 'active'
           AND h.status = 'active'
         ORDER BY h.id
         FOR UPDATE OF h`,
        [userId],
      );
      const blockedIds = owned
        .filter((row) => row.active_member_count > 1 && row.successor_user_id === null)
        .map((row) => row.id);
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

      const transferred = owned.filter((row) => row.active_member_count > 1);
      const transferredHouseholdIds = transferred.map((row) => row.id);
      for (const household of transferred) {
        const previousOwnerRows = await transaction.query<{ readonly id: string }>(
          `UPDATE household_memberships
           SET status = 'left', left_at = $3, version = version + 1, updated_at = $3
           WHERE household_id = $1 AND user_id = $2 AND role = 'owner' AND status = 'active'
           RETURNING id`,
          [household.id, userId, occurredAt],
        );
        const previousOwner = previousOwnerRows[0];
        if (previousOwner === undefined) {
          throw new Error('Active owner membership changed during account deletion.');
        }
        await transaction.query(
          `INSERT INTO household_membership_role_changes (
             id, membership_id, household_id, user_id, previous_role, next_role, changed_by, occurred_at
           ) VALUES ($1, $2, $3, $4, $5, 'owner', $6, $7)`,
          [
            newUuidV7(),
            household.successor_membership_id,
            household.id,
            household.successor_user_id,
            household.successor_previous_role,
            userId,
            occurredAt,
          ],
        );
        await transaction.query(
          `UPDATE household_memberships
           SET role = 'owner', version = version + 1, updated_at = $2
           WHERE id = $1 AND status = 'active'`,
          [household.successor_membership_id, occurredAt],
        );
        await transaction.query(
          `INSERT INTO household_membership_history
           (id, household_id, membership_id, actor_user_id, action, previous_role, new_role,
            previous_status, new_status, reason, occurred_at)
           VALUES
           ($1,$2,$3,$4,'ownership_transferred_from','owner','owner','active','left',
            'Account deletion',$5),
           ($6,$2,$7,$4,'ownership_transferred_to',$8,'owner','active','active',
            'Account deletion',$5)`,
          [
            newUuidV7(),
            household.id,
            previousOwner.id,
            userId,
            occurredAt,
            newUuidV7(),
            household.successor_membership_id,
            household.successor_previous_role,
          ],
        );
        await transaction.query(
          `INSERT INTO audit_events (
             id, actor_user_id, household_id, action, target_type, target_id, outcome,
             safe_details, occurred_at
           ) VALUES ($1, $2, $3, 'household.ownership_transferred_on_account_deletion',
             'user', $4, 'success', $5::jsonb, $6)`,
          [
            newUuidV7(),
            userId,
            household.id,
            household.successor_user_id,
            JSON.stringify({ previousRole: household.successor_previous_role }),
            occurredAt,
          ],
        );
        await transaction.query(
          `INSERT INTO outbox_events (
             id, event_type, aggregate_type, aggregate_id, household_id, actor_user_id,
             payload, occurred_at
           ) VALUES ($1, 'household.ownership_transferred.v1', 'household', $2, $2, $3, $4::jsonb, $5)`,
          [
            newUuidV7(),
            household.id,
            userId,
            JSON.stringify({ successorUserId: household.successor_user_id }),
            occurredAt,
          ],
        );
      }
      const solelyOwnedHouseholdIds = owned
        .filter((row) => row.active_member_count === 1)
        .map((row) => row.id);
      if (solelyOwnedHouseholdIds.length > 0) {
        await transaction.query(
          `UPDATE households SET status = 'closed', updated_at = $2, version = version + 1
           WHERE id = ANY($1::uuid[])`,
          [solelyOwnedHouseholdIds, occurredAt],
        );
        await transaction.query(
          `UPDATE calendar_events
           SET status = 'deleted', deleted_at = $2, updated_at = $2, version = version + 1
           WHERE household_id = ANY($1::uuid[]) AND status = 'active'`,
          [solelyOwnedHouseholdIds, occurredAt],
        );
        await transaction.query(
          `UPDATE household_invitations
           SET status = 'revoked', revoked_at = $2, updated_at = $2
           WHERE household_id = ANY($1::uuid[]) AND status = 'pending'`,
          [solelyOwnedHouseholdIds, occurredAt],
        );
      }

      await transaction.query(
        `UPDATE household_memberships
         SET status = 'left', left_at = $2, version = version + 1, updated_at = $2
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
          JSON.stringify({
            closedHouseholdCount: solelyOwnedHouseholdIds.length,
            transferredHouseholdCount: transferredHouseholdIds.length,
          }),
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
      return {
        status: 'completed',
        closedHouseholdIds: solelyOwnedHouseholdIds,
        transferredHouseholdIds,
      };
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

interface ExportTaskRow {
  readonly id: string;
  readonly household_id: string;
  readonly assignee_membership_id: string;
  readonly assignee_display_name: string;
  readonly title: string;
  readonly instructions: string | null;
  readonly zone: string | null;
  readonly priority: HouseholdTaskSummary['priority'];
  readonly due_date: Date | string;
  readonly due_time: string | null;
  readonly estimated_minutes: number | null;
  readonly recurrence_cadence: HouseholdTaskRecurrenceCadence | null;
  readonly recurrence_ends_on: Date | string | null;
  readonly series_id: string | null;
  readonly occurrence_date: Date | string | null;
  readonly recurrence_completed: boolean;
  readonly status: HouseholdTaskSummary['status'];
  readonly completion_note: string | null;
  readonly completed_by_user_id: string | null;
  readonly completed_at: Date | string | null;
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
  readonly request_id: string | null;
  readonly request_type: HouseholdTaskSummary['requests'][number]['type'] | null;
  readonly request_status: HouseholdTaskSummary['requests'][number]['status'] | null;
  readonly request_reason: string | null;
  readonly requested_assignee_membership_id: string | null;
  readonly requested_due_date: Date | string | null;
  readonly requested_due_time: string | null;
  readonly created_by_membership_id: string | null;
  readonly request_created_by_display_name: string | null;
  readonly resolved_by_user_id: string | null;
  readonly resolved_by_display_name: string | null;
  readonly resolution_note: string | null;
  readonly resolved_at: Date | string | null;
  readonly request_created_at: Date | string | null;
}

interface ExportExpenseRow {
  readonly id: string;
  readonly household_id: string;
  readonly created_by_user_id: string;
  readonly title: string;
  readonly supplier_name: string | null;
  readonly category: ExpenseSummary['category'];
  readonly custom_category_name: string | null;
  readonly amount_minor: number | string | bigint;
  readonly currency: string;
  readonly due_date: Date | string;
  readonly notes: string | null;
  readonly source_template_id: string | null;
  readonly occurrence_date: Date | string | null;
  readonly revision_of_expense_id: string | null;
  readonly superseded_by_expense_id: string | null;
  readonly split_method: 'equal';
  readonly status: ExpenseSummary['status'];
  readonly version: number;
  readonly created_at: Date | string;
  readonly updated_at: Date | string;
  readonly allocation_id: string;
  readonly membership_id: string;
  readonly display_name: string;
  readonly billing_unit_type: 'individual' | 'couple';
  readonly participant_count: 1 | 2;
  readonly allocation_eligible_membership_ids: readonly string[];
  readonly allocation_eligible_user_ids: readonly string[];
  readonly allocation_amount_minor: number | string | bigint;
  readonly rounding_adjustment_minor: number;
  readonly payment_id: string | null;
  readonly payment_amount_minor: number | string | bigint | null;
  readonly payment_method: ExpensePaymentSummary['method'] | null;
  readonly payment_reference: string | null;
  readonly payment_note: string | null;
  readonly payment_paid_at: Date | string | null;
  readonly payment_status: ExpensePaymentSummary['status'] | null;
  readonly payment_declared_by_user_id: string | null;
  readonly payment_declared_by_display_name: string | null;
  readonly payment_confirmed_by_user_id: string | null;
  readonly payment_confirmed_by_display_name: string | null;
  readonly payment_confirmed_at: Date | string | null;
  readonly payment_disputed_by_user_id: string | null;
  readonly payment_disputed_by_display_name: string | null;
  readonly payment_disputed_at: Date | string | null;
  readonly payment_dispute_reason: string | null;
  readonly payment_reversed_by_user_id: string | null;
  readonly payment_reversed_by_display_name: string | null;
  readonly payment_reversed_at: Date | string | null;
  readonly payment_reversal_reason: string | null;
  readonly payment_version: number | null;
  readonly payment_created_at: Date | string | null;
  readonly payment_updated_at: Date | string | null;
}

interface ExportExpenseTemplateRow {
  readonly id: string;
  readonly household_id: string;
  readonly title: string;
  readonly category: ExpenseTemplateSummary['category'];
  readonly custom_category_name: string | null;
  readonly amount_minor: number | string | bigint;
  readonly currency: string;
  readonly cadence: ExpenseTemplateSummary['cadence'];
  readonly next_due_date: Date | string;
  readonly schedule_ends_on: Date | string | null;
  readonly notes: string | null;
  readonly status: ExpenseTemplateSummary['status'];
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

function mapExportTasks(rows: readonly ExportTaskRow[]): HouseholdTaskSummary[] {
  const groups = new Map<string, ExportTaskRow[]>();
  for (const row of rows) groups.set(row.id, [...(groups.get(row.id) ?? []), row]);
  return [...groups.values()].map((group) => {
    const row = group[0];
    if (row === undefined) throw new Error('Task export row group is empty.');
    return {
      id: row.id,
      householdId: row.household_id,
      title: row.title,
      instructions: row.instructions,
      zone: row.zone,
      priority: row.priority,
      dueDate: toDate(row.due_date),
      dueTime: row.due_time?.slice(0, 5) ?? null,
      estimatedMinutes: row.estimated_minutes,
      assigneeMembershipId: row.assignee_membership_id,
      assigneeDisplayName: row.assignee_display_name,
      recurrenceCadence: row.recurrence_cadence,
      recurrenceEndsOn: row.recurrence_ends_on === null ? null : toDate(row.recurrence_ends_on),
      seriesId: row.series_id,
      occurrenceDate: row.occurrence_date === null ? null : toDate(row.occurrence_date),
      recurrenceActive: row.recurrence_cadence !== null && !row.recurrence_completed,
      status: row.status,
      completionNote: row.completion_note,
      completedByUserId: row.completed_by_user_id,
      completedAt: row.completed_at === null ? null : toInstant(row.completed_at),
      requests: group.flatMap((request) => {
        if (
          request.request_id === null ||
          request.request_type === null ||
          request.request_status === null ||
          request.request_reason === null ||
          request.created_by_membership_id === null ||
          request.request_created_by_display_name === null ||
          request.request_created_at === null
        )
          return [];
        return [
          {
            id: request.request_id,
            type: request.request_type,
            status: request.request_status,
            reason: request.request_reason,
            requestedAssigneeMembershipId: request.requested_assignee_membership_id,
            requestedDueDate:
              request.requested_due_date === null ? null : toDate(request.requested_due_date),
            requestedDueTime: request.requested_due_time?.slice(0, 5) ?? null,
            createdByMembershipId: request.created_by_membership_id,
            createdByDisplayName: request.request_created_by_display_name,
            resolvedByUserId: request.resolved_by_user_id,
            resolvedByDisplayName: request.resolved_by_display_name,
            resolutionNote: request.resolution_note,
            resolvedAt: request.resolved_at === null ? null : toInstant(request.resolved_at),
            createdAt: toInstant(request.request_created_at),
          },
        ];
      }),
      canManage: false,
      canStart: false,
      canComplete: false,
      canRequest: false,
      version: row.version,
      createdAt: toInstant(row.created_at),
      updatedAt: toInstant(row.updated_at),
    };
  });
}

function mapExportExpenses(rows: readonly ExportExpenseRow[], userId: string): ExpenseSummary[] {
  const grouped = new Map<string, ExportExpenseRow[]>();
  for (const row of rows) grouped.set(row.id, [...(grouped.get(row.id) ?? []), row]);
  return [...grouped.values()].map((group) => {
    const row = group[0];
    if (row === undefined) throw new Error('Expense export row group is empty.');
    const allocationGroups = new Map<string, ExportExpenseRow[]>();
    for (const allocationRow of group) {
      allocationGroups.set(allocationRow.allocation_id, [
        ...(allocationGroups.get(allocationRow.allocation_id) ?? []),
        allocationRow,
      ]);
    }
    const allocations = [...allocationGroups.values()].map((allocationRows) => {
      const allocation = allocationRows[0];
      if (allocation === undefined) throw new Error('Expense export allocation is empty.');
      const paymentDeclarations = allocationRows.flatMap((paymentRow) => {
        const payment = mapExportPayment(paymentRow, row.currency);
        return payment === null ? [] : [payment];
      });
      const activePayment = [...paymentDeclarations]
        .reverse()
        .find((payment) => payment.status !== 'reversed');
      return {
        membershipId: allocation.membership_id,
        displayName: allocation.display_name,
        billingUnitType: allocation.billing_unit_type,
        participantCount: allocation.participant_count,
        eligibleMembershipIds: allocation.allocation_eligible_membership_ids,
        amount: {
          minorUnits: toSafeInteger(allocation.allocation_amount_minor),
          currency: row.currency,
        },
        roundingAdjustmentMinor: allocation.rounding_adjustment_minor,
        status: paymentAllocationStatus(activePayment?.status),
        paymentDeclarations,
        canDeclarePayment: false,
        isCurrentUser: allocation.allocation_eligible_user_ids.includes(userId),
      };
    });
    return {
      id: row.id,
      householdId: row.household_id,
      title: row.title,
      supplierName: row.supplier_name,
      category: row.category,
      customCategoryName: row.custom_category_name,
      amount: { minorUnits: toSafeInteger(row.amount_minor), currency: row.currency },
      dueDate: toDate(row.due_date),
      notes: row.notes,
      sourceTemplateId: row.source_template_id,
      occurrenceDate: row.occurrence_date === null ? null : toDate(row.occurrence_date),
      revisionOfExpenseId: row.revision_of_expense_id,
      supersededByExpenseId: row.superseded_by_expense_id,
      splitMethod: row.split_method,
      status: row.status,
      allocations,
      currentUserShare: allocations.find((allocation) => allocation.isCurrentUser)?.amount ?? {
        minorUnits: 0,
        currency: row.currency,
      },
      createdByUserId: row.created_by_user_id,
      canApprove: false,
      canReverse: false,
      canRevise: false,
      version: row.version,
      createdAt: toInstant(row.created_at),
      updatedAt: toInstant(row.updated_at),
    };
  });
}

function mapExportPayment(
  payment: ExportExpenseRow,
  currency: string,
): ExpensePaymentSummary | null {
  if (
    payment.payment_id === null ||
    payment.payment_amount_minor === null ||
    payment.payment_method === null ||
    payment.payment_paid_at === null ||
    payment.payment_status === null ||
    payment.payment_declared_by_user_id === null ||
    payment.payment_declared_by_display_name === null ||
    payment.payment_version === null ||
    payment.payment_created_at === null ||
    payment.payment_updated_at === null
  ) {
    return null;
  }
  return {
    id: payment.payment_id,
    expenseId: payment.id,
    allocationMembershipId: payment.membership_id,
    payerDisplayName: payment.display_name,
    amount: {
      minorUnits: toSafeInteger(payment.payment_amount_minor),
      currency,
    },
    method: payment.payment_method,
    reference: payment.payment_reference,
    note: payment.payment_note,
    paidAt: toInstant(payment.payment_paid_at),
    status: payment.payment_status,
    declaredByUserId: payment.payment_declared_by_user_id,
    declaredByDisplayName: payment.payment_declared_by_display_name,
    confirmedByUserId: payment.payment_confirmed_by_user_id,
    confirmedByDisplayName: payment.payment_confirmed_by_display_name,
    confirmedAt:
      payment.payment_confirmed_at === null ? null : toInstant(payment.payment_confirmed_at),
    disputedByUserId: payment.payment_disputed_by_user_id,
    disputedByDisplayName: payment.payment_disputed_by_display_name,
    disputedAt:
      payment.payment_disputed_at === null ? null : toInstant(payment.payment_disputed_at),
    disputeReason: payment.payment_dispute_reason,
    reversedByUserId: payment.payment_reversed_by_user_id,
    reversedByDisplayName: payment.payment_reversed_by_display_name,
    reversedAt:
      payment.payment_reversed_at === null ? null : toInstant(payment.payment_reversed_at),
    reversalReason: payment.payment_reversal_reason,
    canConfirm: false,
    canDispute: false,
    canReverse: false,
    version: payment.payment_version,
    createdAt: toInstant(payment.payment_created_at),
    updatedAt: toInstant(payment.payment_updated_at),
  };
}

function paymentAllocationStatus(
  status: ExpensePaymentSummary['status'] | undefined,
): 'outstanding' | 'declared' | 'paid' | 'disputed' {
  if (status === 'confirmed') return 'paid';
  if (status === 'declared' || status === 'disputed') return status;
  return 'outstanding';
}

function mapExportExpenseTemplate(row: ExportExpenseTemplateRow): ExpenseTemplateSummary {
  return {
    id: row.id,
    householdId: row.household_id,
    title: row.title,
    category: row.category,
    customCategoryName: row.custom_category_name,
    amount: { minorUnits: toSafeInteger(row.amount_minor), currency: row.currency },
    cadence: row.cadence,
    nextDueDate: toDate(row.next_due_date),
    endsOn: row.schedule_ends_on === null ? null : toDate(row.schedule_ends_on),
    notes: row.notes,
    status: row.status,
    canManage: false,
    version: row.version,
    createdAt: toInstant(row.created_at),
    updatedAt: toInstant(row.updated_at),
  };
}

function toSafeInteger(value: number | string | bigint): number {
  const result = Number(value);
  if (!Number.isSafeInteger(result)) throw new Error('Stored money exceeds export range.');
  return result;
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
  return value instanceof Date ? value.toISOString().slice(0, 10) : value.slice(0, 10);
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
