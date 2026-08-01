import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { AppModule } from '../src/app.module.js';

const VALID_PASSWORD = 'A long local passphrase for 2026!';

describe('tenant-scoped one-off calendar events', () => {
  let app: INestApplication;
  let server: Server;

  beforeAll(async () => {
    const module = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = module.createNestApplication();
    await app.init();
    const httpServer: unknown = app.getHttpServer();
    server = httpServer as Server;
  });

  afterAll(async () => {
    await app.close();
  });

  it('creates, lists, updates and safely deletes an event with tenant isolation', async () => {
    const ownerAccessToken = await registerAndVerify(
      server,
      'calendar-owner@example.test',
      'Calendar Owner',
    );
    const outsiderAccessToken = await registerAndVerify(
      server,
      'calendar-outsider@example.test',
      'Calendar Outsider',
    );
    const householdId = await createHousehold(
      server,
      ownerAccessToken,
      'calendar-household-owner-0001',
      'Calendar home',
    );
    const outsiderHouseholdId = await createHousehold(
      server,
      outsiderAccessToken,
      'calendar-household-outsider-01',
      'Other home',
    );
    const endpoint = `/v1/households/${householdId}/calendar-events`;
    const event = calendarEventBody('Boiler service');

    await request(server)
      .get(endpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .expect(400);
    await request(server)
      .get(endpoint)
      .query({ from: '2026-01-01', to: '2027-01-06' })
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .expect(400);

    const unsupportedRecurrence = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('Idempotency-Key', 'calendar-invalid-event-0001')
      .send({ ...event, endTime: '08:00', recurrence: 'weekly' })
      .expect(400);
    expect(unsupportedRecurrence.body).toMatchObject({ code: 'VALIDATION_FAILED' });
    expect(readViolations(unsupportedRecurrence.body)).toEqual(
      expect.arrayContaining([expect.objectContaining({ field: 'recurrence' })]),
    );

    const invalidTime = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('Idempotency-Key', 'calendar-invalid-time-00001')
      .send({ ...event, endTime: '08:00' })
      .expect(400);
    expect(readViolations(invalidTime.body)).toEqual(
      expect.arrayContaining([expect.objectContaining({ field: 'endTime' })]),
    );

    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .send(event)
      .expect(400);

    const idempotencyKey = 'calendar-create-event-0001';
    const created = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(event)
      .expect('ETag', '"1"')
      .expect(201);
    expect(created.body).toMatchObject({
      householdId,
      ...event,
      version: 1,
    });
    const eventId = readStringProperty(created.body, 'id');

    const replayed = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(event)
      .expect(201);
    expect(replayed.body).toEqual(created.body);

    const idempotencyConflict = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(calendarEventBody('Different event'))
      .expect(409);
    expect(idempotencyConflict.body).toMatchObject({ code: 'IDEMPOTENCY_KEY_REUSED' });

    const listed = await request(server)
      .get(endpoint)
      .query({ from: '2026-08-01', to: '2027-07-31' })
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .expect(200);
    expect(listed.body).toEqual([created.body]);

    const hiddenList = await request(server)
      .get(endpoint)
      .query({ from: '2026-08-01', to: '2026-08-31' })
      .set('Authorization', `Bearer ${outsiderAccessToken}`)
      .expect(404);
    expect(hiddenList.body).toMatchObject({ code: 'HOUSEHOLD_NOT_FOUND' });

    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${outsiderAccessToken}`)
      .set('Idempotency-Key', 'calendar-cross-tenant-0001')
      .send(event)
      .expect(404);

    const eventEndpoint = `${endpoint}/${eventId}`;
    await request(server)
      .patch(eventEndpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .send(calendarEventBody('Updated service'))
      .expect(428);
    await request(server)
      .patch(eventEndpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('If-Match', '"99"')
      .send(calendarEventBody('Updated service'))
      .expect(412);

    const updated = await request(server)
      .patch(eventEndpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('If-Match', '"1"')
      .send(calendarEventBody('Updated service'))
      .expect('ETag', '"2"')
      .expect(200);
    expect(updated.body).toMatchObject({ title: 'Updated service', version: 2 });

    await request(server)
      .patch(`/v1/households/${outsiderHouseholdId}/calendar-events/${eventId}`)
      .set('Authorization', `Bearer ${outsiderAccessToken}`)
      .set('If-Match', '"2"')
      .send(calendarEventBody('Cross-household update'))
      .expect(404);
    await request(server)
      .delete(eventEndpoint)
      .set('Authorization', `Bearer ${outsiderAccessToken}`)
      .set('If-Match', '"2"')
      .expect(404);

    await request(server)
      .delete(eventEndpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('If-Match', '"1"')
      .expect(412);
    await request(server)
      .delete(eventEndpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('If-Match', '"2"')
      .expect(204);
    await request(server)
      .delete(eventEndpoint)
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .set('If-Match', '"2"')
      .expect(204);

    const afterDelete = await request(server)
      .get(endpoint)
      .query({ from: '2026-08-01', to: '2026-08-31' })
      .set('Authorization', `Bearer ${ownerAccessToken}`)
      .expect(200);
    expect(afterDelete.body).toEqual([]);
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
  const code = readStringProperty(registration.body, 'developmentVerificationCode');
  const verification = await request(server)
    .post('/v1/auth/verify-email')
    .send({ email, code, deviceName: 'Calendar API tests' })
    .expect(200);
  return readStringProperty(verification.body, 'accessToken');
}

async function createHousehold(
  server: Server,
  accessToken: string,
  idempotencyKey: string,
  name: string,
): Promise<string> {
  const response = await request(server)
    .post('/v1/households')
    .set('Authorization', `Bearer ${accessToken}`)
    .set('Idempotency-Key', idempotencyKey)
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
  return readStringProperty(response.body, 'id');
}

function calendarEventBody(title: string): object {
  return {
    title,
    description: 'Annual safety visit',
    type: 'maintenance',
    date: '2026-08-14',
    startTime: '09:30',
    endTime: '10:30',
    reminderMinutesBefore: 60,
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

function readViolations(value: unknown): unknown {
  if (typeof value !== 'object' || value === null || !('violations' in value)) {
    throw new Error('Response is missing validation violations.');
  }
  return value.violations;
}
