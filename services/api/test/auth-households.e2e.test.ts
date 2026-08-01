import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { AppModule } from '../src/app.module.js';

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
