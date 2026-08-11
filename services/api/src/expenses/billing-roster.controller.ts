import {
  Body,
  Controller,
  Get,
  Headers,
  HttpCode,
  Param,
  Put,
  Res,
  UseGuards,
} from '@nestjs/common';
import type { BillingRosterSummary } from '@sharedhouse/contracts';
import type { Response } from 'express';

import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import { parseBillingRosterUpdate } from '../http/request-validation.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { BillingRosterService } from './billing-roster.service.js';

@Controller('v1/households/:householdId/billing-roster')
@UseGuards(AuthenticationGuard)
export class BillingRosterController {
  constructor(private readonly roster: BillingRosterService) {}

  @Get()
  async get(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Res({ passthrough: true }) response: Response,
  ): Promise<BillingRosterSummary> {
    const board = await this.roster.get(principal.userId, readUuid(householdId));
    response.setHeader('ETag', `"${String(board.version)}"`);
    return board;
  }

  @Put()
  @HttpCode(200)
  async replace(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<BillingRosterSummary> {
    const board = await this.roster.replace({
      userId: principal.userId,
      householdId: readUuid(householdId),
      expectedVersion: readExpectedVersion(ifMatch),
      idempotencyKey: readIdempotencyKey(idempotencyKey),
      couples: parseBillingRosterUpdate(body).couples,
    });
    response.setHeader('ETag', `"${String(board.version)}"`);
    return board;
  }
}

function readUuid(value: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value))
    throw validationProblem([{ field: 'householdId', message: 'Use a valid identifier.' }]);
  return value;
}

function readExpectedVersion(value: string | undefined): number {
  if (value === undefined)
    throw new ApiProblemException({
      status: 428,
      code: 'IF_MATCH_REQUIRED',
      title: 'Provide the current billing roster version in If-Match.',
    });
  const match = /^"([1-9][0-9]*)"$/u.exec(value);
  const version = match?.[1] === undefined ? Number.NaN : Number(match[1]);
  if (!Number.isSafeInteger(version))
    throw validationProblem([
      { field: 'If-Match', message: 'Use a quoted positive version, for example "1".' },
    ]);
  return version;
}

function readIdempotencyKey(value: string | undefined): string {
  if (value === undefined || value.length < 16 || value.length > 128)
    throw validationProblem([
      { field: 'Idempotency-Key', message: 'Provide an idempotency key of 16 to 128 characters.' },
    ]);
  return value;
}
