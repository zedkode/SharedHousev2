import { Injectable } from '@nestjs/common';
import type {
  AcceptHouseholdInvitationResponse,
  CreateHouseholdInvitationRequest,
  HouseholdInvitationCreated,
  HouseholdInvitationPreview,
  HouseholdInvitationSummary,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { ApiProblemException } from '../http/api-problem.exception.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { TokenService } from '../security/token.service.js';
import { InvitationsRepository } from './invitations.repository.js';

const INVITATION_LIFETIME_MS = 7 * 24 * 60 * 60 * 1000;

@Injectable()
export class InvitationsService {
  constructor(
    private readonly repository: InvitationsRepository,
    private readonly tokens: TokenService,
  ) {}

  async create(
    principal: AuthenticatedPrincipal,
    householdId: string,
    request: CreateHouseholdInvitationRequest,
  ): Promise<HouseholdInvitationCreated> {
    const occurredAt = new Date();
    const token = this.tokens.createInvitationToken();
    const result = await this.repository.create({
      invitationId: newUuidV7(occurredAt.getTime()),
      householdId,
      actorUserId: principal.userId,
      tokenHash: this.tokens.hash(token),
      token,
      email: request.email?.trim().toLowerCase() ?? null,
      role: request.role,
      expiresAt: new Date(occurredAt.getTime() + INVITATION_LIFETIME_MS).toISOString(),
      occurredAt: occurredAt.toISOString(),
    });
    if (result.status === 'created') return result.invitation;
    if (result.status === 'not_found') throw invitationHouseholdNotFound();
    if (result.status === 'delegation_forbidden') {
      throw new ApiProblemException({
        status: 403,
        code: 'INVITATION_ROLE_DELEGATION_FORBIDDEN',
        title: 'Your role cannot invite another household administrator.',
      });
    }
    throw invitationManageForbidden();
  }

  async list(
    principal: AuthenticatedPrincipal,
    householdId: string,
  ): Promise<readonly HouseholdInvitationSummary[]> {
    const result = await this.repository.list(
      principal.userId,
      householdId,
      new Date().toISOString(),
    );
    if (result.status === 'listed') return result.invitations;
    if (result.status === 'not_found') throw invitationHouseholdNotFound();
    throw invitationManageForbidden();
  }

  async preview(token: string): Promise<HouseholdInvitationPreview> {
    const preview = await this.repository.preview(
      this.tokens.hash(token),
      new Date().toISOString(),
    );
    if (preview === null) throw invitationNotFound();
    return preview;
  }

  async accept(
    principal: AuthenticatedPrincipal,
    token: string,
  ): Promise<AcceptHouseholdInvitationResponse> {
    const result = await this.repository.accept({
      tokenHash: this.tokens.hash(token),
      userId: principal.userId,
      accountEmail: principal.account.email,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'accepted') return { household: result.household };
    if (result.status === 'expired') {
      throw new ApiProblemException({
        status: 410,
        code: 'INVITATION_EXPIRED',
        title: 'This invitation has expired. Ask for a new invitation.',
      });
    }
    if (result.status === 'email_mismatch') {
      throw new ApiProblemException({
        status: 403,
        code: 'INVITATION_EMAIL_MISMATCH',
        title: 'This invitation is restricted to another email address.',
      });
    }
    if (result.status === 'household_unavailable') {
      throw new ApiProblemException({
        status: 410,
        code: 'INVITATION_HOUSEHOLD_UNAVAILABLE',
        title: 'The household is no longer available.',
      });
    }
    if (result.status === 'not_found') throw invitationNotFound();
    throw new ApiProblemException({
      status: 410,
      code: 'INVITATION_UNAVAILABLE',
      title: 'This invitation has already been used or revoked.',
    });
  }

  async revoke(
    principal: AuthenticatedPrincipal,
    householdId: string,
    invitationId: string,
  ): Promise<void> {
    const result = await this.repository.revoke({
      householdId,
      invitationId,
      actorUserId: principal.userId,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'revoked') return;
    if (result.status === 'not_found') throw invitationNotFound();
    if (result.status === 'already_accepted') {
      throw new ApiProblemException({
        status: 409,
        code: 'INVITATION_ALREADY_ACCEPTED',
        title: 'An accepted invitation cannot be revoked.',
      });
    }
    throw invitationManageForbidden();
  }
}

function invitationNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'INVITATION_NOT_FOUND',
    title: 'The invitation was not found.',
  });
}

function invitationHouseholdNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'HOUSEHOLD_NOT_FOUND',
    title: 'The household was not found.',
  });
}

function invitationManageForbidden(): ApiProblemException {
  return new ApiProblemException({
    status: 403,
    code: 'INVITATION_MANAGE_FORBIDDEN',
    title: 'Your household role cannot manage invitations.',
  });
}
