import { Body, Controller, Get, Headers, Param, Patch, Post, Res, UseGuards } from '@nestjs/common';
import type { ExpenseTemplateSummary } from '@sharedhouse/contracts';
import type { Response } from 'express';

import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import {
  parseArchiveExpenseTemplateRequest,
  parseExpenseTemplateConfiguration,
} from '../http/request-validation.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { ExpenseTemplatesService } from './expense-templates.service.js';

@Controller('v1/households/:householdId/expense-templates')
@UseGuards(AuthenticationGuard)
export class ExpenseTemplatesController {
  constructor(private readonly templates: ExpenseTemplatesService) {}

  @Get()
  list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
  ): Promise<readonly ExpenseTemplateSummary[]> {
    return this.templates.list(principal.userId, readUuid(householdId, 'householdId'));
  }

  @Post()
  async create(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<ExpenseTemplateSummary> {
    const template = await this.templates.create(
      principal.userId,
      readUuid(householdId, 'householdId'),
      parseExpenseTemplateConfiguration(body),
      readIdempotencyKey(idempotencyKey),
    );
    response.setHeader('ETag', `"${String(template.version)}"`);
    return template;
  }

  @Patch(':templateId')
  async update(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('templateId') templateId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<ExpenseTemplateSummary> {
    const template = await this.templates.update(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(templateId, 'templateId'),
      readVersion(ifMatch),
      parseExpenseTemplateConfiguration(body),
    );
    response.setHeader('ETag', `"${String(template.version)}"`);
    return template;
  }

  @Post(':templateId/archive')
  async archive(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('templateId') templateId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<ExpenseTemplateSummary> {
    const template = await this.templates.archive(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(templateId, 'templateId'),
      readVersion(ifMatch),
      parseArchiveExpenseTemplateRequest(body).reason,
    );
    response.setHeader('ETag', `"${String(template.version)}"`);
    return template;
  }
}

function readIdempotencyKey(value: string | undefined): string {
  if (value === undefined || value.length < 16 || value.length > 128)
    throw validationProblem([
      { field: 'Idempotency-Key', message: 'Provide an idempotency key of 16 to 128 characters.' },
    ]);
  return value;
}

function readVersion(value: string | undefined): number {
  if (value === undefined) {
    throw new ApiProblemException({
      status: 428,
      code: 'IF_MATCH_REQUIRED',
      title: 'Provide the current household cost version in If-Match.',
    });
  }
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
