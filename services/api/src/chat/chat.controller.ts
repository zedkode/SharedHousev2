import {
  Body,
  Controller,
  Get,
  Headers,
  type MessageEvent,
  Param,
  Patch,
  Post,
  Query,
  Res,
  Sse,
  UseGuards,
} from '@nestjs/common';
import type {
  HouseholdChatAttachment,
  HouseholdChatMessage,
  HouseholdChatPage,
} from '@sharedhouse/contracts';
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
      readCreateRequest(body),
      readIdempotencyKey(idempotencyKey),
    );
    response.setHeader(
      'Location',
      `/v1/households/${householdId}/chat/messages?after=${message.id}`,
    );
    return message;
  }

  @Post('attachments')
  async uploadAttachment(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Body() body: unknown,
  ): Promise<HouseholdChatAttachment> {
    return this.chat.uploadAttachment(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readAttachment(body),
    );
  }

  @Get('attachments/:attachmentId')
  async downloadAttachment(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('attachmentId') attachmentId: string,
    @Res() response: Response,
  ): Promise<void> {
    const attachment = await this.chat.downloadAttachment(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(attachmentId, 'attachmentId'),
    );
    response.setHeader('Content-Type', attachment.mediaType);
    response.setHeader('Content-Length', String(attachment.content.length));
    response.setHeader('Cache-Control', 'private, max-age=86400');
    response.send(attachment.content);
  }

  @Patch(':messageId/pin')
  pin(
    @CurrentPrincipal() principal: AuthenticatedPrincipal,
    @Param('householdId') householdId: string,
    @Param('messageId') messageId: string,
    @Body() body: unknown,
  ): Promise<HouseholdChatMessage> {
    return this.chat.setPinned(
      principal.userId,
      readUuid(householdId, 'householdId'),
      readUuid(messageId, 'messageId'),
      readPinned(body),
    );
  }
}

function readCreateRequest(value: unknown): {
  readonly body: string;
  readonly attachmentIds: readonly string[];
  readonly mentionedUserIds: readonly string[];
  readonly mentionAll: boolean;
  readonly location: { readonly latitude: number; readonly longitude: number } | null;
} {
  if (
    typeof value !== 'object' ||
    value === null ||
    !('body' in value) ||
    typeof value.body !== 'string'
  ) {
    throw validationProblem([{ field: 'body', message: 'Provide a chat message.' }]);
  }
  const body = value.body.trim();
  if (body.includes('\u0000'))
    throw validationProblem([{ field: 'body', message: 'Remove unsupported null characters.' }]);
  const attachmentIds = readUuidArray(value, 'attachmentIds', 8);
  const mentionedUserIds = readUuidArray(value, 'mentionedUserIds', 64);
  const mentionAll = 'mentionAll' in value && value.mentionAll === true;
  const location = readLocation(value);
  if (body.length === 0 && attachmentIds.length === 0 && location === null)
    throw validationProblem([{ field: 'body', message: 'Provide text, a photo, or a location.' }]);
  return { body, attachmentIds, mentionedUserIds, mentionAll, location };
}

function readUuidArray(value: object, field: string, maximum: number): readonly string[] {
  const record = value as Record<string, unknown>;
  if (!(field in record) || record[field] === undefined) return [];
  const candidate = record[field];
  if (!Array.isArray(candidate) || candidate.length > maximum)
    throw validationProblem([
      { field, message: `Provide at most ${String(maximum)} identifiers.` },
    ]);
  return [
    ...new Set(
      candidate.map((item) => {
        if (typeof item !== 'string')
          throw validationProblem([{ field, message: 'Use valid identifiers.' }]);
        return readUuid(item, field);
      }),
    ),
  ];
}

function readLocation(
  value: object,
): { readonly latitude: number; readonly longitude: number } | null {
  if (!('location' in value) || value.location === undefined || value.location === null)
    return null;
  const location = value.location;
  if (typeof location !== 'object' || !('latitude' in location) || !('longitude' in location))
    throw validationProblem([{ field: 'location', message: 'Provide latitude and longitude.' }]);
  const latitude = location.latitude;
  const longitude = location.longitude;
  if (
    typeof latitude !== 'number' ||
    !Number.isFinite(latitude) ||
    latitude < -90 ||
    latitude > 90 ||
    typeof longitude !== 'number' ||
    !Number.isFinite(longitude) ||
    longitude < -180 ||
    longitude > 180
  )
    throw validationProblem([{ field: 'location', message: 'Provide valid coordinates.' }]);
  return { latitude, longitude };
}

function readAttachment(value: unknown): {
  readonly mediaType: HouseholdChatAttachment['mediaType'];
  readonly width: number;
  readonly height: number;
  readonly content: Buffer;
} {
  if (
    typeof value !== 'object' ||
    value === null ||
    !('mediaType' in value) ||
    !('contentBase64' in value) ||
    !('width' in value) ||
    !('height' in value)
  )
    throw validationProblem([
      { field: 'attachment', message: 'Provide compressed image metadata and content.' },
    ]);
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(String(value.mediaType)))
    throw validationProblem([{ field: 'mediaType', message: 'Use JPEG, PNG, or WebP.' }]);
  if (
    typeof value.contentBase64 !== 'string' ||
    !/^[A-Za-z0-9+/]+={0,2}$/u.test(value.contentBase64)
  )
    throw validationProblem([
      { field: 'contentBase64', message: 'Provide valid base64 image content.' },
    ]);
  const content = Buffer.from(value.contentBase64, 'base64');
  if (content.length < 1 || content.length > 2_621_440)
    throw validationProblem([
      { field: 'contentBase64', message: 'Compressed photos must be at most 2.5 MB.' },
    ]);
  const width = value.width;
  const height = value.height;
  if (
    !Number.isSafeInteger(width) ||
    !Number.isSafeInteger(height) ||
    Number(width) < 1 ||
    Number(width) > 8192 ||
    Number(height) < 1 ||
    Number(height) > 8192
  )
    throw validationProblem([{ field: 'dimensions', message: 'Provide valid image dimensions.' }]);
  return {
    mediaType: value.mediaType as HouseholdChatAttachment['mediaType'],
    width: Number(width),
    height: Number(height),
    content,
  };
}

function readPinned(value: unknown): boolean {
  if (
    typeof value !== 'object' ||
    value === null ||
    !('pinned' in value) ||
    typeof value.pinned !== 'boolean'
  )
    throw validationProblem([
      { field: 'pinned', message: 'Choose whether the message is pinned.' },
    ]);
  return value.pinned;
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
