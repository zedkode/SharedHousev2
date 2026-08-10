import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';

import { AppModule } from '../src/app.module.js';

const VALID_PASSWORD = 'A long local passphrase for 2026!';

describe('tenant-scoped household expenses', () => {
  let app: INestApplication;
  let server: Server;
  let ownerToken: string;
  let memberToken: string;
  let outsiderToken: string;
  let householdId: string;

  beforeAll(async () => {
    const module = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = module.createNestApplication();
    await app.init();
    server = app.getHttpServer() as Server;
    ownerToken = await registerAndVerify(server, 'money-owner@example.test', 'Money Owner');
    memberToken = await registerAndVerify(server, 'money-member@example.test', 'Money Member');
    outsiderToken = await registerAndVerify(
      server,
      'money-outsider@example.test',
      'Money Outsider',
    );
    const household = await request(server)
      .post('/v1/households')
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-household-create-0001')
      .send(householdBody('Money home'))
      .expect(201);
    householdId = readStringProperty(household.body, 'id');
    const invitation = await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ role: 'member', email: 'money-member@example.test' })
      .expect(201);
    await request(server)
      .post(`/v1/invitations/${readStringProperty(invitation.body, 'token')}/accept`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
  });

  afterAll(async () => {
    await app.close();
  });

  it('proposes, deterministically splits, approves and reverses without deleting history', async () => {
    const endpoint = `/v1/households/${householdId}/expenses`;
    const body = expenseBody(1001, 'GBP');
    const created = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('Idempotency-Key', 'money-expense-create-000001')
      .send(body)
      .expect('ETag', '"1"')
      .expect(201);
    expect(created.body).toMatchObject({
      householdId,
      title: 'Internet bill',
      status: 'proposed',
      splitMethod: 'equal',
      amount: { minorUnits: 1001, currency: 'GBP' },
      currentUserShare: { currency: 'GBP' },
      version: 1,
    });
    const expenseId = readStringProperty(created.body, 'id');
    const allocations = readAllocations(created.body);
    expect(allocations).toHaveLength(2);
    expect(allocations.map((allocation) => allocation.membershipId)).toEqual(
      [...allocations.map((allocation) => allocation.membershipId)].sort(),
    );
    expect(allocations.map((allocation) => allocation.amount.minorUnits)).toEqual([501, 500]);
    expect(allocations.reduce((sum, allocation) => sum + allocation.amount.minorUnits, 0)).toBe(
      1001,
    );
    expect(allocations.map((allocation) => allocation.roundingAdjustmentMinor)).toEqual([1, 0]);

    const replay = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('Idempotency-Key', 'money-expense-create-000001')
      .send(body)
      .expect(201);
    expect(replay.body).toEqual(created.body);

    await request(server)
      .post(`${endpoint}/${expenseId}/approve`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('If-Match', '"1"')
      .expect(403);

    const ownerList = await request(server)
      .get(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    const ownerExpenses = readArrayProperty(ownerList.body, 'root');
    expect(ownerExpenses).toHaveLength(1);
    expect(ownerExpenses[0]).toMatchObject({ id: expenseId, canApprove: true });

    const approved = await request(server)
      .post(`${endpoint}/${expenseId}/approve`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .expect('ETag', '"2"')
      .expect(201);
    expect(approved.body).toMatchObject({ status: 'approved', version: 2, canApprove: false });

    await request(server)
      .post(`${endpoint}/${expenseId}/reverse`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('If-Match', '"2"')
      .send({ reason: 'Incorrect supplier invoice' })
      .expect(403);

    const reversed = await request(server)
      .post(`${endpoint}/${expenseId}/reverse`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"2"')
      .send({ reason: 'Incorrect supplier invoice' })
      .expect('ETag', '"3"')
      .expect(201);
    expect(reversed.body).toMatchObject({ status: 'reversed', version: 3, canReverse: false });

    const after = await request(server)
      .get(`${endpoint}/${expenseId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    expect(after.body).toEqual(reversed.body);

    const exported = await request(server)
      .post('/v1/account/export')
      .set('Authorization', `Bearer ${memberToken}`)
      .send({ password: VALID_PASSWORD })
      .expect(200);
    const exportedExpenses = readArrayProperty(exported.body, 'expenses');
    expect(exportedExpenses).toHaveLength(1);
    expect(exportedExpenses[0]).toMatchObject({
      id: expenseId,
      status: 'reversed',
      canApprove: false,
      canReverse: false,
    });
  });

  it('enforces currency, validation and tenant boundaries', async () => {
    const endpoint = `/v1/households/${householdId}/expenses`;
    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-expense-invalid-00001')
      .send(expenseBody(1000, 'EUR'))
      .expect(409)
      .expect((response) =>
        expect(response.body).toMatchObject({ code: 'EXPENSE_CURRENCY_MISMATCH' }),
      );
    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-expense-invalid-00002')
      .send({ ...expenseBody(0, 'GBP'), splitMethod: 'fixed' })
      .expect(400);
    await request(server).get(endpoint).set('Authorization', `Bearer ${outsiderToken}`).expect(404);
  });

  it('lets owners manage reusable standard and custom household costs', async () => {
    const endpoint = `/v1/households/${householdId}/expense-templates`;
    const customRent = {
      title: 'Garden studio rent',
      category: 'custom',
      customCategoryName: 'Studio rent',
      amount: { minorUnits: 145_000, currency: 'GBP' },
      cadence: 'monthly',
      nextDueDate: '2026-09-01',
      notes: 'Reusable household cost',
    };

    await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('Idempotency-Key', 'money-template-member-0001')
      .send(customRent)
      .expect(403);

    const created = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-template-create-0001')
      .send(customRent)
      .expect('ETag', '"1"')
      .expect(201);
    expect(created.body).toMatchObject({
      title: 'Garden studio rent',
      category: 'custom',
      customCategoryName: 'Studio rent',
      cadence: 'monthly',
      status: 'active',
      canManage: true,
      version: 1,
    });
    const templateId = readStringProperty(created.body, 'id');

    const visibleToMember = await request(server)
      .get(endpoint)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(readArrayProperty(visibleToMember.body, 'root')[0]).toMatchObject({
      id: templateId,
      canManage: false,
    });

    const updated = await request(server)
      .patch(`${endpoint}/${templateId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .send({ ...customRent, amount: { minorUnits: 150_000, currency: 'GBP' } })
      .expect('ETag', '"2"')
      .expect(200);
    expect(updated.body).toMatchObject({ amount: { minorUnits: 150_000 }, version: 2 });

    await request(server)
      .patch(`${endpoint}/${templateId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .send(customRent)
      .expect(412);

    await request(server)
      .post(`${endpoint}/${templateId}/archive`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"2"')
      .send({ reason: 'Lease ended' })
      .expect('ETag', '"3"')
      .expect(201)
      .expect((response) =>
        expect(response.body).toMatchObject({ status: 'archived', version: 3 }),
      );

    const memberAfterArchive = await request(server)
      .get(endpoint)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(readArrayProperty(memberAfterArchive.body, 'root')).toHaveLength(0);

    const customExpense = await request(server)
      .post(`/v1/households/${householdId}/expenses`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-custom-expense-0001')
      .send({
        title: customRent.title,
        category: customRent.category,
        customCategoryName: customRent.customCategoryName,
        amount: customRent.amount,
        dueDate: customRent.nextDueDate,
      })
      .expect(201);
    expect(customExpense.body).toMatchObject({
      category: 'custom',
      customCategoryName: 'Studio rent',
    });

    await request(server)
      .post(`/v1/households/${householdId}/expenses`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-custom-invalid-0001')
      .send({ ...expenseBody(1000, 'GBP'), category: 'custom' })
      .expect(400);

    const exported = await request(server)
      .post('/v1/account/export')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ password: VALID_PASSWORD })
      .expect(200);
    expect(readArrayProperty(exported.body, 'expenseTemplates')[0]).toMatchObject({
      id: templateId,
      customCategoryName: 'Studio rent',
      status: 'archived',
      canManage: false,
    });
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
      deviceName: 'Money API tests',
    })
    .expect(200);
  return readStringProperty(verification.body, 'accessToken');
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

function expenseBody(minorUnits: number, currency: string): object {
  return {
    title: 'Internet bill',
    category: 'internet',
    amount: { minorUnits, currency },
    dueDate: '2026-08-20',
    notes: 'August broadband service',
  };
}

interface AllocationBody {
  membershipId: string;
  amount: { minorUnits: number; currency: string };
  roundingAdjustmentMinor: number;
}

function readAllocations(body: unknown): AllocationBody[] {
  if (
    typeof body !== 'object' ||
    body === null ||
    !Array.isArray((body as { allocations?: unknown }).allocations)
  ) {
    throw new Error('Expected expense allocations.');
  }
  return (body as { allocations: AllocationBody[] }).allocations;
}

function readArrayProperty(body: unknown, property: string): unknown[] {
  if (property === 'root') {
    if (!Array.isArray(body)) throw new Error('Expected an array response.');
    return body as unknown[];
  }
  if (typeof body !== 'object' || body === null) throw new Error('Expected an object response.');
  const value = (body as Record<string, unknown>)[property];
  if (!Array.isArray(value)) throw new Error(`Expected ${property} to be an array.`);
  return value as unknown[];
}

function readStringProperty(body: unknown, property: string): string {
  if (typeof body !== 'object' || body === null) throw new Error('Expected an object response.');
  const value = (body as Record<string, unknown>)[property];
  if (typeof value !== 'string') throw new Error(`Expected ${property} to be text.`);
  return value;
}
