import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { createHash } from 'node:crypto';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { AppModule } from '../src/app.module.js';
import { DatabaseService } from '../src/database/database.service.js';

const VALID_PASSWORD = 'A long local passphrase for 2026!';

describe('household invitations', () => {
  let app: INestApplication;
  let server: Server;
  let database: DatabaseService;
  let ownerToken: string;
  let memberToken: string;
  let outsiderToken: string;
  let householdId: string;

  beforeAll(async () => {
    const module = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = module.createNestApplication();
    await app.init();
    server = app.getHttpServer() as Server;
    database = app.get(DatabaseService);
    ownerToken = await registerAndVerify(server, 'invite-owner@example.test', 'Invite Owner');
    memberToken = await registerAndVerify(server, 'invite-member@example.test', 'Invite Member');
    outsiderToken = await registerAndVerify(
      server,
      'invite-outsider@example.test',
      'Invite Outsider',
    );
    const household = await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'invitation-household-create-0001')
      .send(householdBody())
      .expect(201);
    householdId = readStringProperty(household.body, 'id');
  });

  afterAll(async () => {
    await app.close();
  });

  it('creates, previews and accepts a single-use email-restricted invitation', async () => {
    const created = await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ role: 'member', email: 'INVITE-MEMBER@example.test' })
      .expect(201);
    const token = readStringProperty(created.body, 'token');
    const invitationId = readStringProperty(created.body, 'id');
    expect(token).toMatch(/^sh_inv_[A-Za-z0-9_-]{43}$/u);
    expect(created.body).toMatchObject({
      householdId,
      role: 'member',
      email: 'invite-member@example.test',
      status: 'pending',
    });

    const stored = await database.query<{ readonly token_hash: string }>(
      'SELECT token_hash FROM household_invitations WHERE id = $1',
      [invitationId],
    );
    expect(stored[0]?.token_hash).toBe(createHash('sha256').update(token).digest('hex'));
    expect(stored[0]?.token_hash).not.toContain(token);

    const listedBefore = await request(server)
      .get(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    expect(listedBefore.body).toHaveLength(1);
    expect(JSON.stringify(listedBefore.body)).not.toContain(token);

    await request(server)
      .get(`/v1/invitations/${token}`)
      .expect(200)
      .expect((response) => {
        expect(response.body).toMatchObject({
          householdName: 'Invitation home',
          role: 'member',
          emailRestricted: true,
          status: 'pending',
        });
      });

    const mismatch = await request(server)
      .post(`/v1/invitations/${token}/accept`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .expect(403);
    expect(mismatch.body).toMatchObject({ code: 'INVITATION_EMAIL_MISMATCH' });

    const accepted = await request(server)
      .post(`/v1/invitations/${token}/accept`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(accepted.body).toMatchObject({
      household: { id: householdId, role: 'member', status: 'active' },
    });
    const acceptedBody = accepted.body as { household: unknown };

    const replayed = await request(server)
      .post(`/v1/invitations/${token}/accept`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(replayed.body).toEqual(accepted.body);

    const listedHouseholds = await request(server)
      .get('/v1/households')
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(listedHouseholds.body).toEqual([acceptedBody.household]);

    const revokeAccepted = await request(server)
      .delete(`/v1/households/${householdId}/invitations/${invitationId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(409);
    expect(revokeAccepted.body).toMatchObject({ code: 'INVITATION_ALREADY_ACCEPTED' });
  });

  it('revokes pending invitations idempotently and hides tenant management', async () => {
    const created = await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ role: 'read_only' })
      .expect(201);
    const token = readStringProperty(created.body, 'token');
    const invitationId = readStringProperty(created.body, 'id');

    await request(server)
      .get(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .expect(404);

    await request(server)
      .delete(`/v1/households/${householdId}/invitations/${invitationId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(204);
    await request(server)
      .delete(`/v1/households/${householdId}/invitations/${invitationId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(204);

    await request(server)
      .get(`/v1/invitations/${token}`)
      .expect(200)
      .expect((response) => {
        expect(response.body).toMatchObject({ status: 'unavailable' });
      });
    const unavailable = await request(server)
      .post(`/v1/invitations/${token}/accept`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .expect(410);
    expect(unavailable.body).toMatchObject({ code: 'INVITATION_UNAVAILABLE' });
  });

  it('prevents an administrator from delegating administrator access', async () => {
    const adminInvite = await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ role: 'admin', email: 'invite-outsider@example.test' })
      .expect(201);
    await request(server)
      .post(`/v1/invitations/${readStringProperty(adminInvite.body, 'token')}/accept`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .expect(200);

    const denied = await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .send({ role: 'admin' })
      .expect(403);
    expect(denied.body).toMatchObject({ code: 'INVITATION_ROLE_DELEGATION_FORBIDDEN' });
  });

  it('rejects malformed tokens and unsupported invitation roles', async () => {
    await request(server).get('/v1/invitations/not-a-secret').expect(400);
    await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ role: 'owner' })
      .expect(400);
  });
});

async function registerAndVerify(
  server: Server,
  email: string,
  displayName: string,
): Promise<string> {
  const registration = await request(server)
    .post('/v1/auth/register')
    .send({
      email,
      password: VALID_PASSWORD,
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
      code: readStringProperty(registration.body, 'developmentVerificationCode'),
      deviceName: 'Invitation API tests',
    })
    .expect(200);
  return readStringProperty(verification.body, 'accessToken');
}

function householdBody(): object {
  return {
    name: 'Invitation home',
    countryCode: 'GB',
    timezone: 'Europe/London',
    currency: 'GBP',
    firstDayOfWeek: 1,
    cycleType: 'calendar_month',
    cycleAnchor: '2026-08-01',
  };
}

function readStringProperty(value: unknown, property: string): string {
  if (
    typeof value !== 'object' ||
    value === null ||
    !(property in value) ||
    typeof value[property as keyof typeof value] !== 'string'
  ) {
    throw new Error(`Response is missing string property ${property}.`);
  }
  return value[property as keyof typeof value];
}
