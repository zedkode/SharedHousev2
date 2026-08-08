import { Body, Controller, Delete, Get, HttpCode, Param, Post, UseGuards } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import type {
  AcceptHouseholdInvitationResponse,
  HouseholdInvitationCreated,
  HouseholdInvitationPreview,
  HouseholdInvitationSummary,
} from '@sharedhouse/contracts';

import { validationProblem } from '../http/api-problem.exception.js';
import { parseCreateHouseholdInvitation } from '../http/request-validation.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { InvitationsService } from './invitations.service.js';

@Controller('v1/households/:householdId/invitations')
@UseGuards(AuthenticationGuard)
export class HouseholdInvitationsController {
  constructor(private readonly invitations: InvitationsService) {}

  @Get()
  list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
  ): Promise<readonly HouseholdInvitationSummary[]> {
    return this.invitations.list(principal, readUuid(householdId, 'householdId'));
  }

  @Post()
  @Throttle({ default: { limit: 20, ttl: 60 * 60_000 } })
  create(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Body() body: unknown,
  ): Promise<HouseholdInvitationCreated> {
    return this.invitations.create(
      principal,
      readUuid(householdId, 'householdId'),
      parseCreateHouseholdInvitation(body),
    );
  }

  @Delete(':invitationId')
  @HttpCode(204)
  revoke(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('invitationId') invitationId: string,
  ): Promise<void> {
    return this.invitations.revoke(
      principal,
      readUuid(householdId, 'householdId'),
      readUuid(invitationId, 'invitationId'),
    );
  }
}

@Controller('v1/invitations')
export class InvitationsController {
  constructor(private readonly invitations: InvitationsService) {}

  @Get(':token')
  @Throttle({ default: { limit: 30, ttl: 15 * 60_000 } })
  preview(@Param('token') token: string): Promise<HouseholdInvitationPreview> {
    return this.invitations.preview(readInvitationToken(token));
  }

  @Post(':token/accept')
  @HttpCode(200)
  @UseGuards(AuthenticationGuard)
  @Throttle({ default: { limit: 10, ttl: 15 * 60_000 } })
  accept(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('token') token: string,
  ): Promise<AcceptHouseholdInvitationResponse> {
    return this.invitations.accept(principal, readInvitationToken(token));
  }
}

function readUuid(value: string, field: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value)) {
    throw validationProblem([{ field, message: 'Use a valid identifier.' }]);
  }
  return value;
}

function readInvitationToken(value: string): string {
  if (!/^sh_inv_[A-Za-z0-9_-]{43}$/u.test(value)) {
    throw validationProblem([
      { field: 'token', message: 'Use the complete SharedHouse invitation code.' },
    ]);
  }
  return value;
}
