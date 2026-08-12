import type { INestApplication } from '@nestjs/common';
import type { HouseholdChatMessage, HouseholdChatPage } from '@sharedhouse/contracts';
import { Test } from '@nestjs/testing';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { AppModule } from '../src/app.module.js';

const PASSWORD = 'A long local passphrase for chat 2026!';

describe('tenant-scoped household chat', () => {
  let app: INestApplication;
  let server: Server;

  beforeAll(async () => {
    const module = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = module.createNestApplication();
    await app.init();
    server = app.getHttpServer() as Server;
  });

  afterAll(async () => app.close());

  it('appends, replays and incrementally lists messages without leaking another household', async () => {
    const ownerToken = await register(server, 'chat-owner@example.test', 'Chat Owner');
    const outsiderToken = await register(server, 'chat-outsider@example.test', 'Chat Outsider');
    const readOnlyToken = await register(server, 'chat-read-only@example.test', 'Chat Reader');
    const householdId = await createHousehold(
      server,
      ownerToken,
      'chat-household-create-0001',
      'Chat home',
    );
    const endpoint = `/v1/households/${householdId}/chat/messages`;

    const readOnlyInvitation = await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ role: 'read_only', email: 'chat-read-only@example.test' })
      .expect(201);
    await request(server)
      .post(`/v1/invitations/${readString(readOnlyInvitation.body as unknown, 'token')}/accept`)
      .set('Authorization', `Bearer ${readOnlyToken}`)
      .expect(200);

    await request(server).get(endpoint).set('Authorization', `Bearer ${outsiderToken}`).expect(404);
    await request(server).get(endpoint).set('Authorization', `Bearer ${readOnlyToken}`).expect(200);
    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${readOnlyToken}`)
      .set('Idempotency-Key', 'chat-read-only-message-0001')
      .send({ body: 'This must be denied.' })
      .expect(403);
    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ body: 'Hello' })
      .expect(400);
    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'chat-empty-message-0001')
      .send({ body: '   ' })
      .expect(400);

    const key = 'chat-first-message-0001';
    const created = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', key)
      .send({ body: '  I can clean the kitchen tonight.  ' })
      .expect(201);
    const createdBody = readChatMessage(created.body as unknown);
    expect(createdBody).toMatchObject({
      householdId,
      senderDisplayName: 'Chat Owner',
      isCurrentUser: true,
      body: 'I can clean the kitchen tonight.',
    });

    const replay = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', key)
      .send({ body: 'I can clean the kitchen tonight.' })
      .expect(201);
    expect(readChatMessage(replay.body as unknown)).toEqual(createdBody);

    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', key)
      .send({ body: 'A different message' })
      .expect(409);

    const firstPage = await request(server)
      .get(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    const firstPageBody = readChatPage(firstPage.body as unknown);
    expect(firstPageBody.messages).toEqual([createdBody]);
    expect(firstPageBody.nextCursor).toBe(createdBody.id);

    const second = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'chat-second-message-0001')
      .send({ body: 'Thank you.' })
      .expect(201);
    const secondBody = readChatMessage(second.body as unknown);
    const incremental = await request(server)
      .get(endpoint)
      .query({ after: createdBody.id, limit: 10 })
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    expect(readChatPage(incremental.body as unknown).messages).toEqual([secondBody]);
  });
});

async function register(server: Server, email: string, displayName: string): Promise<string> {
  const registration = await request(server)
    .post('/v1/auth/register')
    .send({
      email,
      password: PASSWORD,
      displayName,
      preferredLocale: 'en',
      ageConfirmed: true,
      termsAccepted: true,
      marketingConsent: false,
    })
    .expect(202);
  const verification = await request(server)
    .post('/v1/auth/verify-email')
    .send({
      email,
      code: readString(registration.body, 'developmentVerificationCode'),
      deviceName: 'Chat API tests',
    })
    .expect(200);
  return readString(verification.body, 'accessToken');
}

async function createHousehold(
  server: Server,
  accessToken: string,
  key: string,
  name: string,
): Promise<string> {
  const response = await request(server)
    .post('/v1/households')
    .set('Authorization', `Bearer ${accessToken}`)
    .set('Idempotency-Key', key)
    .send({
      name,
      countryCode: 'GB',
      timezone: 'Europe/London',
      currency: 'GBP',
      firstDayOfWeek: 1,
      cycleType: 'calendar_month',
      cycleAnchor: '2026-08-01',
    })
    .expect(201);
  return readString(response.body, 'id');
}

function readString(value: unknown, property: string): string {
  if (
    typeof value !== 'object' ||
    value === null ||
    !(property in value) ||
    typeof value[property as keyof typeof value] !== 'string'
  )
    throw new Error(`Missing ${property}.`);
  return value[property as keyof typeof value];
}

function readChatMessage(value: unknown): HouseholdChatMessage {
  if (typeof value !== 'object' || value === null)
    throw new Error('Missing household chat message.');
  const record = value as Record<string, unknown>;
  const isCurrentUser = record.isCurrentUser;
  if (typeof isCurrentUser !== 'boolean') throw new Error('Missing isCurrentUser.');
  return {
    id: readString(value, 'id'),
    householdId: readString(value, 'householdId'),
    senderMembershipId: readString(value, 'senderMembershipId'),
    senderUserId: readString(value, 'senderUserId'),
    senderDisplayName: readString(value, 'senderDisplayName'),
    isCurrentUser,
    body: readString(value, 'body'),
    createdAt: readString(value, 'createdAt'),
  };
}

function readChatPage(value: unknown): HouseholdChatPage {
  if (typeof value !== 'object' || value === null) throw new Error('Missing chat page.');
  const record = value as Record<string, unknown>;
  const messages = record.messages;
  const nextCursor = record.nextCursor;
  if (!Array.isArray(messages)) throw new Error('Missing chat messages.');
  if (nextCursor !== null && typeof nextCursor !== 'string')
    throw new Error('Invalid chat cursor.');
  return { messages: messages.map((message: unknown) => readChatMessage(message)), nextCursor };
}
