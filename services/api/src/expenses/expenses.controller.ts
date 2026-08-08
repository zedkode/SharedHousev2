import {
  Body,
  Controller,
  Get,
  Headers,
  Param,
  Post,
  Res,
  UseGuards,
} from '@nestjs/common';
import type { ExpenseSummary } from '@sharedhouse/contracts';
import type { Response } from 'express';

import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import {
  parseExpenseConfiguration,
  parseReverseExpenseRequest,
} from '../http/request-validation.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { ExpensesService } from './expenses.service.js';

@Controller('v1/households/:householdId/expenses')
@UseGuards(AuthenticationGuard)
export class ExpensesController {
  constructor(private readonly expenses: ExpensesService) {}

  @Get()
  list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
  ): Promise<readonly ExpenseSummary[]> {
    return this.expenses.list(principal.userId, readUuid(householdId, 'householdId'));
  }

  @Get(':expenseId')
  get(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('expenseId') expenseId: string,
  ): Promise<ExpenseSummary> {
    return this.expenses.get(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(expenseId, 'expenseId'),
    );
  }

  @Post()
  async create(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<ExpenseSummary> {
    const expense = await this.expenses.create(
      principal.userId,
      readUuid(householdId, 'householdId'),
      parseExpenseConfiguration(body),
      readIdempotencyKey(idempotencyKey),
    );
    response.setHeader('ETag', `"${String(expense.version)}"`);
    return expense;
  }

  @Post(':expenseId/approve')
  async approve(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('expenseId') expenseId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Res({ passthrough: true }) response: Response,
  ): Promise<ExpenseSummary> {
    const expense = await this.expenses.approve(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(expenseId, 'expenseId'),
      readExpectedVersion(ifMatch),
    );
    response.setHeader('ETag', `"${String(expense.version)}"`);
    return expense;
  }

  @Post(':expenseId/reverse')
  async reverse(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('expenseId') expenseId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<ExpenseSummary> {
    const expense = await this.expenses.reverse(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(expenseId, 'expenseId'),
      readExpectedVersion(ifMatch),
      parseReverseExpenseRequest(body).reason,
    );
    response.setHeader('ETag', `"${String(expense.version)}"`);
    return expense;
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
      title: 'Provide the current expense version in If-Match.',
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

function readUuid(value: string, field: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value)) {
    throw validationProblem([{ field, message: 'Use a valid identifier.' }]);
  }
  return value;
}
