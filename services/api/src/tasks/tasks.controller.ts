import { Body, Controller, Get, Headers, Param, Post, Res, UseGuards } from '@nestjs/common';
import type { HouseholdTaskBoard, HouseholdTaskSummary } from '@sharedhouse/contracts';
import type { Response } from 'express';
import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import {
  parseHouseholdTaskAction,
  parseHouseholdTaskConfiguration,
} from '../http/request-validation.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { TasksService } from './tasks.service.js';

@Controller('v1/households/:householdId/tasks')
@UseGuards(AuthenticationGuard)
export class TasksController {
  constructor(private readonly tasks: TasksService) {}

  @Get()
  list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
  ): Promise<HouseholdTaskBoard> {
    return this.tasks.list(principal.userId, readUuid(householdId, 'householdId'));
  }

  @Post()
  async create(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<HouseholdTaskSummary> {
    const task = await this.tasks.create(
      principal.userId,
      readUuid(householdId, 'householdId'),
      parseHouseholdTaskConfiguration(body),
      readIdempotencyKey(idempotencyKey),
    );
    response.setHeader('ETag', `"${String(task.version)}"`);
    return task;
  }

  @Post(':taskId/actions')
  async action(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('taskId') taskId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<HouseholdTaskSummary> {
    const task = await this.tasks.action(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(taskId, 'taskId'),
      readExpectedVersion(ifMatch),
      parseHouseholdTaskAction(body),
      readIdempotencyKey(idempotencyKey),
    );
    response.setHeader('ETag', `"${String(task.version)}"`);
    return task;
  }
}

function readIdempotencyKey(value: string | undefined): string {
  if (value === undefined || value.length < 16 || value.length > 128)
    throw validationProblem([
      { field: 'Idempotency-Key', message: 'Provide an idempotency key of 16 to 128 characters.' },
    ]);
  return value;
}
function readExpectedVersion(value: string | undefined): number {
  if (value === undefined)
    throw new ApiProblemException({
      status: 428,
      code: 'IF_MATCH_REQUIRED',
      title: 'Provide the current task version in If-Match.',
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
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value))
    throw validationProblem([{ field, message: 'Use a valid identifier.' }]);
  return value;
}
