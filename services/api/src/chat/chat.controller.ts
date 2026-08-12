import {
  Body,
  Controller,
  Get,
  Headers,
  type MessageEvent,
  Param,
  Post,
  Query,
  Res,
  Sse,
  UseGuards,
} from '@nestjs/common';
import type { HouseholdChatMessage, HouseholdChatPage } from '@sharedhouse/contracts';
import type { Response } from 'express';
import type { Observable } from 'rxjs';

import { validationProblem } from '../http/api-problem.exception.js';
import type { AuthenticatedPrincipal } from '../identity/identity.types.js';
import { AuthenticationGuard } from '../security/authentication.guard.js';
import { CurrentPrincipal } from '../security/current-principal.decorator.js';
import { ChatService } from './chat.service.js';

@Controller('v1/households/:householdId/chat/messages')
@UseGuards(AuthenticationGuard)
export class ChatController {
  constructor(private readonly chat: ChatService) {}

  @Get()
  list(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Query('after') after: string | undefined,
    @Query('limit') limit: string | undefined,
  ): Promise<HouseholdChatPage> {
    return this.chat.list(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readOptionalUuid(after, 'after'),
      readLimit(limit),
    );
  }

  @Sse('stream')
  stream(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Query('after') after: string | undefined,
  ): Observable<MessageEvent> {
    return this.chat.stream(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readOptionalUuid(after, 'after'),
    );
  }

  @Post()
  async create(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Headers('idempotency-key') idempotencyKey: string | undefined,
    @Body() body: unknown,
    @Res({ passthrough: true }) response: Response,
  ): Promise<HouseholdChatMessage> {
    const message = await this.chat.create(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readBody(body),
      readIdempotencyKey(idempotencyKey),
    );
    response.setHeader(
      'Location',
      `/v1/households/${householdId}/chat/messages?after=${message.id}`,
    );
    return message;
  }
}

function readBody(value: unknown): string {
  if (
    typeof value !== 'object' ||
    value === null ||
    !('body' in value) ||
    typeof value.body !== 'string'
  ) {
    throw validationProblem([{ field: 'body', message: 'Provide a chat message.' }]);
  }
  const body = value.body.trim();
  if (body.length < 1 || body.length > 2000 || body.includes('\u0000')) {
    throw validationProblem([
      { field: 'body', message: 'Use between 1 and 2000 visible characters.' },
    ]);
  }
  return body;
}

function readIdempotencyKey(value: string | undefined): string {
  if (value === undefined || value.length < 16 || value.length > 128)
    throw validationProblem([
      { field: 'Idempotency-Key', message: 'Provide an idempotency key of 16 to 128 characters.' },
    ]);
  return value;
}

function readLimit(value: string | undefined): number {
  if (value === undefined) return 100;
  const limit = Number(value);
  if (!Number.isSafeInteger(limit) || limit < 1 || limit > 100)
    throw validationProblem([{ field: 'limit', message: 'Use an integer from 1 to 100.' }]);
  return limit;
}

function readOptionalUuid(value: string | undefined, field: string): string | null {
  return value === undefined || value === '' ? null : readUuid(value, field);
}

function readUuid(value: string, field: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value))
    throw validationProblem([{ field, message: 'Use a valid identifier.' }]);
  return value;
}
