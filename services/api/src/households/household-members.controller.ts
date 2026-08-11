import {
  Body,
  Controller,
  Get,
  Headers,
  HttpCode,
  Param,
  Post,
  Res,
  UseGuards,
} from '@nestjs/common';
import type { HouseholdMemberBoard, HouseholdMemberSummary } from '@sharedhouse/contracts';
import type { Response } from 'express';
import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import { parseHouseholdMemberAction } from '../http/request-validation.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { HouseholdMembersService } from './household-members.service.js';

@Controller('v1/households/:householdId/members')
@UseGuards(AuthenticationGuard)
export class HouseholdMembersController {
  constructor(private readonly members: HouseholdMembersService) {}

  @Get()
  list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
  ): Promise<HouseholdMemberBoard> {
    return this.members.list(principal.userId, readUuid(householdId, 'householdId'));
  }

  @Post(':membershipId/actions')
  @HttpCode(200)
  async action(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('membershipId') membershipId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<HouseholdMemberSummary> {
    const member = await this.members.action({
      userId: principal.userId,
      householdId: readUuid(householdId, 'householdId'),
      membershipId: readUuid(membershipId, 'membershipId'),
      expectedVersion: readExpectedVersion(ifMatch),
      idempotencyKey: readIdempotencyKey(idempotencyKey),
      action: parseHouseholdMemberAction(body),
    });
    response.setHeader('ETag', `"${String(member.version)}"`);
    return member;
  }
}

function readIdempotencyKey(value: string | undefined): string {
  if (value === undefined || value.length < 16 || value.length > 128) {
    throw validationProblem([
      { field: 'Idempotency-Key', message: 'Provide an idempotency key of 16 to 128 characters.' },
    ]);
  }
  return value;
}
function readExpectedVersion(value: string | undefined): number {
  if (value === undefined)
    throw new ApiProblemException({
      status: 428,
      code: 'IF_MATCH_REQUIRED',
      title: 'Provide the current member version in If-Match.',
    });
  const match = /^"([1-9][0-9]*)"$/u.exec(value);
  const version = match?.[1] === undefined ? Number.NaN : Number(match[1]);
  if (!Number.isSafeInteger(version))
    throw validationProblem([
      { field: 'If-Match', message: 'Use a quoted positive version, for example "1".' },
    ]);
  return version;
}
function readUuid(value: string, field: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value)) {
    throw validationProblem([{ field, message: 'Use a valid identifier.' }]);
  }
  return value;
}
