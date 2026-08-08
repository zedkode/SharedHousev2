import type {
  HouseholdInvitationCreated,
  HouseholdInvitationRole,
  HouseholdInvitationSummary,
  HouseholdSummary,
} from '@sharedhouse/contracts';

export interface CreateInvitationRecord {
  readonly invitationId: string;
  readonly householdId: string;
  readonly actorUserId: string;
  readonly tokenHash: string;
  readonly token: string;
  readonly email: string | null;
  readonly role: HouseholdInvitationRole;
  readonly expiresAt: string;
  readonly occurredAt: string;
}

export type CreateInvitationResult =
  | { readonly status: 'created'; readonly invitation: HouseholdInvitationCreated }
  | { readonly status: 'not_found' | 'forbidden' | 'delegation_forbidden' };

export type ListInvitationsResult =
  | { readonly status: 'listed'; readonly invitations: readonly HouseholdInvitationSummary[] }
  | { readonly status: 'not_found' | 'forbidden' };

export type AcceptInvitationResult =
  | { readonly status: 'accepted'; readonly household: HouseholdSummary }
  | {
      readonly status:
        'not_found' | 'expired' | 'unavailable' | 'email_mismatch' | 'household_unavailable';
    };

export type RevokeInvitationResult =
  | { readonly status: 'revoked' }
  | { readonly status: 'not_found' | 'forbidden' | 'already_accepted' };
