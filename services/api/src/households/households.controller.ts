import { Body, Controller, Get, Headers, Param, Patch, Post, Res, UseGuards } from '@nestjs/common';
import type { HouseholdSummary } from '@sharedhouse/contracts';
import type { Response } from 'express';

import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import { parseHouseholdConfiguration } from '../http/request-validation.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { HouseholdsService } from './households.service.js';

@Controller('v1/households')
@UseGuards(AuthenticationGuard)
export class HouseholdsController {
  constructor(private readonly households: HouseholdsService) {}

  @Get()
  async list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
  ): Promise<readonly HouseholdSummary[]> {
    return this.households.list(principal.userId);
  }

  @Post()
  async create(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
  ): Promise<HouseholdSummary> {
    return this.households.create(
      principal.userId,
      parseHouseholdConfiguration(body),
      readIdempotencyKey(idempotencyKey),
    );
  }

  @Get(':householdId')
  async get(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Res({ passthrough: true }) response: Response,
  ): Promise<HouseholdSummary> {
    const household = await this.households.get(principal.userId, readUuid(householdId));
    response.setHeader('ETag', `"${String(household.version)}"`);
    return household;
  }

  @Patch(':householdId')
  async update(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<HouseholdSummary> {
    const household = await this.households.update(
      principal.userId,
      readUuid(householdId),
      readExpectedVersion(ifMatch),
      parseHouseholdConfiguration(body),
    );
    response.setHeader('ETag', `"${String(household.version)}"`);
    return household;
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
  if (value === undefined) {
    throw new ApiProblemException({
      status: 428,
      code: 'IF_MATCH_REQUIRED',
      title: 'Provide the current household version in If-Match.',
    });
  }
  const match = /^"([1-9][0-9]*)"$/u.exec(value);
  const version = match?.[1] === undefined ? Number.NaN : Number(match[1]);
  if (!Number.isSafeInteger(version)) {
    throw validationProblem([
      { field: 'If-Match', message: 'Use a quoted positive version, for example "1".' },
    ]);
  }
  return version;
}

function readUuid(value: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value)) {
    throw validationProblem([
      { field: 'householdId', message: 'Use a valid household identifier.' },
    ]);
  }
  return value;
}
