import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { AppModule } from '../src/app.module.js';
import { DatabaseService } from '../src/database/database.service.js';

const VALID_PASSWORD = 'A long local passphrase for 2026!';

describe('authentication and household vertical slice', () => {
  let app: INestApplication;
  let server: Server;

  beforeAll(async () => {
    ({ app, server } = await createTestApplication());
  });

  afterAll(async () => {
    await app.close();
  });

  it('registers, verifies, authenticates, and configures a household idempotently', async () => {
    const email = 'owner@example.test';
    const registration = await request(server)
      .post('/v1/auth/register')
      .send(registrationBody(email, 'Owner Example'))
      .expect('Cache-Control', 'no-store')
      .expect('Pragma', 'no-cache')
      .expect(202);
    expect(registration.body).toMatchObject({ verificationRequired: true });
    const verificationCode = readStringProperty(registration.body, 'developmentVerificationCode');

    const blockedSignIn = await request(server)
      .post('/v1/auth/sign-in')
      .send({ email, password: VALID_PASSWORD, deviceName: 'API tests' })
      .expect('Content-Type', /application\/problem\+json/u)
      .expect(403);
    expect(blockedSignIn.body).toMatchObject({ code: 'EMAIL_VERIFICATION_REQUIRED' });

    const verified = await request(server)
      .post('/v1/auth/verify-email')
      .send({ email, code: verificationCode, deviceName: 'API tests' })
      .expect('Cache-Control', 'no-store')
      .expect('Pragma', 'no-cache')
      .expect(200);
    const accessToken = readStringProperty(verified.body, 'accessToken');

    const account = await request(server)
      .get('/v1/account')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);
    expect(account.body).toMatchObject({
      email,
      emailVerified: true,
      displayName: 'Owner Example',
    });

    const configuration = householdBody('Example household');
    await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${accessToken}`)
      .send(configuration)
      .expect(400);

    const idempotencyKey = 'household-create-owner-0001';
    const created = await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${accessToken}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(configuration)
      .expect(201);
    expect(created.body).toMatchObject({ ...configuration, role: 'owner', version: 1 });
    const householdId = readStringProperty(created.body, 'id');

    const replayed = await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${accessToken}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(configuration)
      .expect(201);
    expect(replayed.body).toEqual(created.body);

    const conflict = await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${accessToken}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(householdBody('Different household'))
      .expect(409);
    expect(conflict.body).toMatchObject({ code: 'IDEMPOTENCY_KEY_REUSED' });

    const listed = await request(server)
      .get('/v1/households')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);
    expect(listed.body).toHaveLength(1);

    const fetched = await request(server)
      .get(`/v1/households/${householdId}`)
      .set('Authorization', `Bearer ${accessToken}`)
      .expect('ETag', '"1"')
      .expect(200);
    expect(fetched.body).toEqual(created.body);

    await request(server)
      .patch(`/v1/households/${householdId}`)
      .set('Authorization', `Bearer ${accessToken}`)
      .send(householdBody('Updated home'))
      .expect(428);

    await request(server)
      .patch(`/v1/households/${householdId}`)
      .set('Authorization', `Bearer ${accessToken}`)
      .set('If-Match', '"99"')
      .send(householdBody('Updated home'))
      .expect(412);

    const updated = await request(server)
      .patch(`/v1/households/${householdId}`)
      .set('Authorization', `Bearer ${accessToken}`)
      .set('If-Match', '"1"')
      .send(householdBody('Updated home'))
      .expect('ETag', '"2"')
      .expect(200);
    expect(updated.body).toMatchObject({ name: 'Updated home', version: 2 });
  });

  it('rotates an expired verification challenge and keeps resend responses generic', async () => {
    const email = 'resend@example.test';
    const registration = await request(server)
      .post('/v1/auth/register')
      .send(registrationBody(email, 'Resend Example'))
      .expect(202);
    const firstCode = readStringProperty(registration.body, 'developmentVerificationCode');
    const database = app.get(DatabaseService);
    await database.query(
      `UPDATE email_verification_challenges
       SET created_at = $2
       WHERE user_id = (SELECT id FROM users WHERE email_normalized = $1)`,
      [email, '2026-01-01T00:00:00.000Z'],
    );

    const resent = await request(server)
      .post('/v1/auth/resend-verification')
      .send({ email })
      .expect('Cache-Control', 'no-store')
      .expect(202);
    const replacementCode = readStringProperty(resent.body, 'developmentVerificationCode');
    expect(replacementCode).not.toBe(firstCode);

    await request(server)
      .post('/v1/auth/verify-email')
      .send({ email, code: firstCode, deviceName: 'API tests' })
      .expect(400);
    await request(server)
      .post('/v1/auth/verify-email')
      .send({ email, code: replacementCode, deviceName: 'API tests' })
      .expect(200);

    await request(server)
      .post('/v1/auth/resend-verification')
      .send({ email: 'missing@example.test' })
      .expect(202)
      .expect({ verificationRequired: true });
  });

  it('rotates refresh tokens and revokes the family when an old token is reused', async () => {
    const session = await registerAndVerify(server, 'rotation@example.test', 'Rotation Example');
    const refreshToken = readStringProperty(session, 'refreshToken');

    const rotated = await request(server)
      .post('/v1/auth/refresh')
      .send({ refreshToken })
      .expect('Cache-Control', 'no-store')
      .expect('Pragma', 'no-cache')
      .expect(200);
    const rotatedAccessToken = readStringProperty(rotated.body, 'accessToken');

    const reuse = await request(server).post('/v1/auth/refresh').send({ refreshToken }).expect(401);
    expect(reuse.body).toMatchObject({ code: 'REFRESH_TOKEN_REUSED' });

    await request(server)
      .get('/v1/account')
      .set('Authorization', `Bearer ${rotatedAccessToken}`)
      .expect(401);
  });

  it('hides a valid household identifier from another authenticated account', async () => {
    const ownerSession = await registerAndVerify(server, 'tenant-a@example.test', 'Tenant A');
    const otherSession = await registerAndVerify(server, 'tenant-b@example.test', 'Tenant B');
    const ownerAccessToken = readStringProperty(ownerSession, 'accessToken');
    const otherAccessToken = readStringProperty(otherSession, 'accessToken');
    const created = await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('Idempotency-Key', 'tenant-isolation-household-0001')
      .send(householdBody('Private household'))
      .expect(201);
    const householdId = readStringProperty(created.body, 'id');

    const hidden = await request(server)
      .get(`/v1/households/${householdId}`)
      .set('Authorization', `Bearer ${otherAccessToken}`)
      .expect(404);
    expect(hidden.body).toMatchObject({ code: 'HOUSEHOLD_NOT_FOUND' });
  });

  it('rejects unknown fields and weak passwords without reflecting secrets', async () => {
    const response = await request(server)
      .post('/v1/auth/register')
      .send({
        ...registrationBody('invalid@example.test', 'Invalid Example'),
        password: 'password',
        admin: true,
      })
      .expect(400);
    expect(response.body).toMatchObject({ code: 'VALIDATION_FAILED' });
    expect(JSON.stringify(response.body)).not.toContain('passwordpassword');
  });

  it('keeps duplicate registration generic when development codes are disabled', async () => {
    const previousSetting = process.env.AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE;
    process.env.AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE = 'false';
    const isolated = await createTestApplication();
    try {
      const email = 'duplicate@example.test';
      const initial = await request(isolated.server)
        .post('/v1/auth/register')
        .send(registrationBody(email, 'Duplicate Example'))
        .expect('Cache-Control', 'no-store')
        .expect('Pragma', 'no-cache')
        .expect(202);
      const duplicate = await request(isolated.server)
        .post('/v1/auth/register')
        .send(registrationBody(email, 'Different Display Name'))
        .expect('Cache-Control', 'no-store')
        .expect('Pragma', 'no-cache')
        .expect(202);

      expect(initial.body).toEqual({ verificationRequired: true });
      expect(duplicate.body).toEqual(initial.body);
    } finally {
      await isolated.app.close();
      if (previousSetting === undefined) {
        delete process.env.AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE;
      } else {
        process.env.AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE = previousSetting;
      }
    }
  });

  it('signs in without cacheable tokens and sign-out revokes the current session', async () => {
    const isolated = await createTestApplication();
    try {
      const email = 'sign-out@example.test';
      await registerAndVerify(isolated.server, email, 'Sign Out Example');

      const signedIn = await request(isolated.server)
        .post('/v1/auth/sign-in')
        .send({ email, password: VALID_PASSWORD, deviceName: 'Revocation test' })
        .expect('Cache-Control', 'no-store')
        .expect('Pragma', 'no-cache')
        .expect(200);
      const accessToken = readStringProperty(signedIn.body, 'accessToken');

      await request(isolated.server)
        .post('/v1/auth/sign-out')
        .set('Authorization', `Bearer ${accessToken}`)
        .expect(204);

      await request(isolated.server)
        .get('/v1/account')
        .set('Authorization', `Bearer ${accessToken}`)
        .expect(401);
    } finally {
      await isolated.app.close();
    }
  });

  it('re-authenticates, anonymises, revokes sessions, and closes a sole-member household', async () => {
    const session = await registerAndVerify(server, 'delete-owner@example.test', 'Delete Owner');
    const accessToken = readStringProperty(session, 'accessToken');
    const created = await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${accessToken}`)
      .set('Idempotency-Key', 'delete-owner-household-0001')
      .send(householdBody('Delete owner home'))
      .expect(201);
    const householdId = readStringProperty(created.body, 'id');

    await request(server)
      .post('/v1/account/export')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ password: 'wrong password' })
      .expect(401);
    const exported = await request(server)
      .post('/v1/account/export')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ password: VALID_PASSWORD })
      .expect('Cache-Control', 'no-store')
      .expect('Content-Disposition', /sharedhouse-account-export\.json/u)
      .expect(200);
    expect(exported.body).toMatchObject({
      formatVersion: '1',
      account: { email: 'delete-owner@example.test' },
    });
    expect(exported.body.households).toHaveLength(1);
    expect(exported.body.consentRecords).toHaveLength(3);
    expect(exported.body.sessions.length).toBeGreaterThan(0);

    await request(server)
      .delete('/v1/account')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ password: 'wrong password', confirmation: 'DELETE' })
      .expect(401);

    const deleted = await request(server)
      .delete('/v1/account')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ password: VALID_PASSWORD, confirmation: 'DELETE' })
      .expect('Cache-Control', 'no-store')
      .expect(200);
    expect(deleted.body).toEqual({ status: 'completed', closedHouseholdIds: [householdId] });

    await request(server)
      .get('/v1/account')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(401);
    await request(server)
      .post('/v1/auth/sign-in')
      .send({ email: 'delete-owner@example.test', password: VALID_PASSWORD })
      .expect(401);

    const database = app.get(DatabaseService);
    const users = await database.query<{
      readonly email_normalized: string;
      readonly status: string;
    }>(`SELECT email_normalized, status FROM users WHERE id = $1`, [
      readStringProperty(session, 'account', 'id'),
    ]);
    expect(users[0]?.status).toBe('deleted');
    expect(users[0]?.email_normalized).toContain('@deleted.sharedhouse.invalid');
  });

  it('blocks deletion while an owned household still has another active member', async () => {
    const isolated = await createTestApplication();
    try {
      const owner = await registerAndVerify(
        isolated.server,
        'blocked-owner@example.test',
        'Blocked Owner',
      );
      const member = await registerAndVerify(
        isolated.server,
        'blocked-member@example.test',
        'Blocked Member',
      );
      const ownerToken = readStringProperty(owner, 'accessToken');
      const created = await request(isolated.server)
        .post('/v1/households')
        .set('Authorization', `Bearer ${ownerToken}`)
        .set('Idempotency-Key', 'blocked-owner-household-0001')
        .send(householdBody('Shared active home'))
        .expect(201);
      const database = isolated.app.get(DatabaseService);
      await database.query(
        `INSERT INTO household_memberships (id, household_id, user_id, role, status, joined_at)
       VALUES ($1, $2, $3, 'member', 'active', $4)`,
        [
          '019f9c00-0000-7000-8000-000000000001',
          readStringProperty(created.body, 'id'),
          readStringProperty(member, 'account', 'id'),
          new Date().toISOString(),
        ],
      );

      const blocked = await request(isolated.server)
        .delete('/v1/account')
        .set('Authorization', `Bearer ${ownerToken}`)
        .send({ password: VALID_PASSWORD, confirmation: 'DELETE' })
        .expect(409);
      expect(blocked.body).toMatchObject({ code: 'ACCOUNT_DELETION_OWNER_TRANSFER_REQUIRED' });
      await request(isolated.server)
        .get('/v1/account')
        .set('Authorization', `Bearer ${ownerToken}`)
        .expect(200);
    } finally {
      await isolated.app.close();
    }
  });

  it('serves a public deletion form and accepts a credential-confirmed request', async () => {
    const isolated = await createTestApplication();
    try {
      await registerAndVerify(isolated.server, 'web-delete@example.test', 'Web Delete');
      await request(isolated.server)
        .get('/account-deletion')
        .expect('Content-Type', /text\/html/u)
        .expect(200)
        .expect(/Delete your account/u);
      await request(isolated.server)
        .post('/account-deletion')
        .type('form')
        .send({
          email: 'web-delete@example.test',
          password: VALID_PASSWORD,
          confirmation: 'DELETE',
        })
        .expect('Cache-Control', 'no-store')
        .expect(201)
        .expect(/account was deleted/u);
    } finally {
      await isolated.app.close();
    }
  });
});

async function createTestApplication(): Promise<{
  readonly app: INestApplication;
  readonly server: Server;
}> {
  const module = await Test.createTestingModule({ imports: [AppModule] }).compile();
  const app = module.createNestApplication();
  await app.init();
  const httpServer: unknown = app.getHttpServer();
  return { app, server: httpServer as Server };
}

async function registerAndVerify(
  server: Server,
  email: string,
  displayName: string,
): Promise<unknown> {
  const registration = await request(server)
    .post('/v1/auth/register')
    .send(registrationBody(email, displayName))
    .expect(202);
  const code = readStringProperty(registration.body, 'developmentVerificationCode');
  const verification = await request(server)
    .post('/v1/auth/verify-email')
    .send({ email, code, deviceName: 'API tests' })
    .expect(200);
  const body: unknown = verification.body;
  return body;
}

function registrationBody(email: string, displayName: string): object {
  return {
    email,
    password: VALID_PASSWORD,
    displayName,
    preferredLocale: 'en',
    ageConfirmed: true,
    termsAccepted: true,
    marketingConsent: false,
  };
}

function householdBody(name: string): object {
  return {
    name,
    countryCode: 'GB',
    timezone: 'Europe/London',
    currency: 'GBP',
    firstDayOfWeek: 1,
    cycleType: 'calendar_month',
    cycleAnchor: '2026-08-01',
  };
}

function readStringProperty(value: unknown, property: string, nestedProperty?: string): string {
  const candidate =
    nestedProperty === undefined
      ? value
      : typeof value === 'object' && value !== null && property in value
        ? value[property as keyof typeof value]
        : undefined;
  const targetProperty = nestedProperty ?? property;
  if (
    typeof candidate !== 'object' ||
    candidate === null ||
    !(targetProperty in candidate) ||
    typeof candidate[targetProperty as keyof typeof candidate] !== 'string'
  ) {
    throw new Error(`Response is missing string property ${targetProperty}.`);
  }
  return candidate[targetProperty as keyof typeof candidate];
}
