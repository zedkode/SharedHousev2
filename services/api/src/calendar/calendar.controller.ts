import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  HttpCode,
  HttpStatus,
  Param,
  Patch,
  Post,
  Query,
  Res,
  UseGuards,
} from '@nestjs/common';
import type { CalendarEventSummary } from '@sharedhouse/contracts';
import type { Response } from 'express';

import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import {
  parseCalendarDateRange,
  parseCalendarEventConfiguration,
} from '../http/request-validation.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { CalendarService } from './calendar.service.js';

@Controller('v1/households/:householdId/calendar-events')
@UseGuards(AuthenticationGuard)
export class CalendarController {
  constructor(private readonly calendar: CalendarService) {}

  @Get()
  async list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Query('from') from: string | undefined,
    @Query('to') to: string | undefined,
  ): Promise<readonly CalendarEventSummary[]> {
    const range = parseCalendarDateRange(from, to);
    return this.calendar.list(
      principal.userId,
      readUuid(householdId, 'householdId'),
      range.from,
      range.to,
    );
  }

  @Post()
  async create(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<CalendarEventSummary> {
    const event = await this.calendar.create(
      principal.userId,
      readUuid(householdId, 'householdId'),
      parseCalendarEventConfiguration(body),
      readIdempotencyKey(idempotencyKey),
    );
    response.setHeader('ETag', `"${String(event.version)}"`);
    return event;
  }

  @Patch(':eventId')
  async update(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('eventId') eventId: string,
    @Headers('if-match') ifMatch: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<CalendarEventSummary> {
    const event = await this.calendar.update(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(eventId, 'eventId'),
      readExpectedVersion(ifMatch),
      parseCalendarEventConfiguration(body),
    );
    response.setHeader('ETag', `"${String(event.version)}"`);
    return event;
  }

  @Delete(':eventId')
  @HttpCode(HttpStatus.NO_CONTENT)
  async delete(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('eventId') eventId: string,
    @Headers('if-match') ifMatch: string | undefined,
  ): Promise<void> {
    await this.calendar.delete(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(eventId, 'eventId'),
      readExpectedVersion(ifMatch),
    );
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
      title: 'Provide the current calendar event version in If-Match.',
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
