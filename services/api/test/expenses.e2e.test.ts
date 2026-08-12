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
      sourceTemplateId: null,
      occurrenceDate: null,
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

  it('lets managers revise an unsettled expense without erasing the original', async () => {
    const endpoint = `/v1/households/${householdId}/expenses`;
    const created = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-expense-revision-source-01')
      .send({
        ...expenseBody(3600, 'GBP'),
        title: 'Electricity estimate',
        supplierName: 'Initial Energy Ltd',
      })
      .expect(201);
    const sourceId = readStringProperty(created.body, 'id');
    expect(created.body).toMatchObject({ supplierName: 'Initial Energy Ltd', canRevise: true });

    const revisionBody = {
      ...expenseBody(4200, 'GBP'),
      title: 'Electricity final invoice',
      supplierName: 'Correct Energy Ltd',
      reason: 'Final supplier invoice replaced the estimate',
    };
    await request(server)
      .post(`${endpoint}/${sourceId}/revise`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('If-Match', '"1"')
      .set('Idempotency-Key', 'money-expense-revision-member-01')
      .send(revisionBody)
      .expect(403);

    const revised = await request(server)
      .post(`${endpoint}/${sourceId}/revise`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .set('Idempotency-Key', 'money-expense-revision-owner-001')
      .send(revisionBody)
      .expect('ETag', '"1"')
      .expect(201);
    const revisedId = readStringProperty(revised.body, 'id');
    expect(revised.body).toMatchObject({
      title: 'Electricity final invoice',
      supplierName: 'Correct Energy Ltd',
      amount: { minorUnits: 4200, currency: 'GBP' },
      revisionOfExpenseId: sourceId,
      supersededByExpenseId: null,
      status: 'approved',
      canRevise: true,
    });

    const replay = await request(server)
      .post(`${endpoint}/${sourceId}/revise`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .set('Idempotency-Key', 'money-expense-revision-owner-001')
      .send(revisionBody)
      .expect(201);
    expect(replay.body).toEqual(revised.body);

    const source = await request(server)
      .get(`${endpoint}/${sourceId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    expect(source.body).toMatchObject({
      status: 'reversed',
      supersededByExpenseId: revisedId,
      canRevise: false,
    });
  });

  it('declares, confirms, disputes and reverses payments without pretending to move money', async () => {
    const endpoint = `/v1/households/${householdId}/expenses`;
    const created = await request(server)
      .post(endpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-payment-expense-0001')
      .send({ ...expenseBody(2400, 'GBP'), title: 'Shared electricity' })
      .expect(201);
    const expenseId = readStringProperty(created.body, 'id');

    const memberView = await request(server)
      .get(`${endpoint}/${expenseId}`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    const memberAllocation = readAllocations(memberView.body).find(
      (allocation) => allocation.isCurrentUser,
    );
    expect(memberAllocation).toMatchObject({
      amount: { minorUnits: 1200, currency: 'GBP' },
      status: 'outstanding',
      canDeclarePayment: true,
      paymentDeclarations: [],
    });

    const declarationBody = {
      method: 'bank_transfer',
      paidAt: new Date(Date.now() - 60_000).toISOString(),
      reference: 'BANK-REF-2048',
      note: 'Sent from personal bank account',
    };
    const declared = await request(server)
      .post(`${endpoint}/${expenseId}/payments`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('Idempotency-Key', 'money-payment-declare-0001')
      .send(declarationBody)
      .expect('ETag', '"1"')
      .expect(201);
    const declaration = readCurrentPayment(declared.body);
    expect(declaration).toMatchObject({
      method: 'bank_transfer',
      reference: 'BANK-REF-2048',
      status: 'declared',
      declaredByDisplayName: 'Money Member',
      version: 1,
      canConfirm: false,
      canDispute: false,
      canReverse: true,
    });
    const paymentId = readStringProperty(declaration, 'id');

    const replay = await request(server)
      .post(`${endpoint}/${expenseId}/payments`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('Idempotency-Key', 'money-payment-declare-0001')
      .send(declarationBody)
      .expect(201);
    expect(readCurrentPayment(replay.body)).toEqual(declaration);

    await request(server)
      .post(`${endpoint}/${expenseId}/payments/${paymentId}/confirm`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('If-Match', '"1"')
      .expect(403);
    await request(server)
      .post(`${endpoint}/${expenseId}/payments/${paymentId}/confirm`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .set('If-Match', '"1"')
      .expect(404);

    const confirmed = await request(server)
      .post(`${endpoint}/${expenseId}/payments/${paymentId}/confirm`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .expect('ETag', '"2"')
      .expect(201);
    expect(readPaymentById(confirmed.body, paymentId)).toMatchObject({
      status: 'confirmed',
      declaredByDisplayName: 'Money Member',
      confirmedByDisplayName: 'Money Owner',
      version: 2,
      canConfirm: false,
      canDispute: true,
    });
    expect(readAllocationWithPayment(confirmed.body, paymentId)).toMatchObject({ status: 'paid' });

    const disputed = await request(server)
      .post(`${endpoint}/${expenseId}/payments/${paymentId}/dispute`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"2"')
      .send({ reason: 'Reference does not match the bank statement' })
      .expect('ETag', '"3"')
      .expect(201);
    expect(readPaymentById(disputed.body, paymentId)).toMatchObject({
      status: 'disputed',
      declaredByDisplayName: 'Money Member',
      disputedByDisplayName: 'Money Owner',
      disputeReason: 'Reference does not match the bank statement',
      version: 3,
    });
    expect(readAllocationWithPayment(disputed.body, paymentId)).toMatchObject({
      status: 'disputed',
    });

    await request(server)
      .post(`${endpoint}/${expenseId}/reverse`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .send({ reason: 'Invoice cancelled' })
      .expect(409)
      .expect((response) =>
        expect(response.body).toMatchObject({ code: 'EXPENSE_ACTIVE_PAYMENT_CONFLICT' }),
      );

    const corrected = await request(server)
      .post(`${endpoint}/${expenseId}/payments/${paymentId}/reverse`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('If-Match', '"3"')
      .send({ reason: 'Transfer was returned by the bank' })
      .expect('ETag', '"4"')
      .expect(201);
    expect(readPaymentById(corrected.body, paymentId)).toMatchObject({
      status: 'reversed',
      declaredByDisplayName: 'Money Member',
      reversedByDisplayName: 'Money Member',
      reversalReason: 'Transfer was returned by the bank',
      version: 4,
      canReverse: false,
    });
    expect(readAllocationWithPayment(corrected.body, paymentId)).toMatchObject({
      status: 'outstanding',
      canDeclarePayment: true,
    });

    const ownerDeclared = await request(server)
      .post(`${endpoint}/${expenseId}/payments`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-payment-owner-000001')
      .send({ ...declarationBody, reference: 'OWNER-BANK-4096' })
      .expect(201);
    const ownerPaymentId = readStringProperty(readCurrentPayment(ownerDeclared.body), 'id');
    const memberCanReview = await request(server)
      .get(`${endpoint}/${expenseId}`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(readPaymentById(memberCanReview.body, ownerPaymentId)).toMatchObject({
      canConfirm: true,
      canDispute: true,
    });
    await request(server)
      .post(`${endpoint}/${expenseId}/payments/${ownerPaymentId}/confirm`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('If-Match', '"1"')
      .expect(201)
      .expect((response) =>
        expect(readPaymentById(response.body, ownerPaymentId)).toMatchObject({
          status: 'confirmed',
          declaredByDisplayName: 'Money Owner',
          confirmedByDisplayName: 'Money Member',
          version: 2,
        }),
      );

    const exported = await request(server)
      .post('/v1/account/export')
      .set('Authorization', `Bearer ${memberToken}`)
      .send({ password: VALID_PASSWORD })
      .expect(200);
    const exportedExpense = readArrayProperty(exported.body, 'expenses').find(
      (expense) => readStringProperty(expense, 'id') === expenseId,
    );
    expect(exportedExpense).toBeDefined();
    expect(readPaymentById(exportedExpense, paymentId)).toMatchObject({
      status: 'reversed',
      declaredByDisplayName: 'Money Member',
      reversedByDisplayName: 'Money Member',
      canConfirm: false,
      canDispute: false,
      canReverse: false,
    });
  });

  it('lets owners manage reusable standard and custom household costs', async () => {
    const endpoint = `/v1/households/${householdId}/expense-templates`;
    const customRent = {
      title: 'Garden studio rent',
      category: 'custom',
      customCategoryName: 'Studio rent',
      amount: { minorUnits: 145_000, currency: 'GBP' },
      cadence: 'fortnightly',
      nextDueDate: '2026-09-01',
      endsOn: '2026-12-31',
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
      cadence: 'fortnightly',
      endsOn: '2026-12-31',
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

  it('splits rent by real residents and supports couples with or without a second app account', async () => {
    const rosterEndpoint = `/v1/households/${householdId}/billing-roster`;
    const initial = await request(server)
      .get(rosterEndpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect('ETag', '"1"')
      .expect(200);
    const rosterMembers = readArrayProperty(initial.body, 'members');
    const ownerMembershipId = readStringProperty(
      rosterMembers.find((member) => readStringProperty(member, 'displayName') === 'Money Owner'),
      'membershipId',
    );
    const memberMembershipId = readStringProperty(
      rosterMembers.find((member) => readStringProperty(member, 'displayName') === 'Money Member'),
      'membershipId',
    );
    const guestCouple = {
      couples: [
        {
          primaryMembershipId: ownerMembershipId,
          partnerDisplayName: 'Taylor without app access',
        },
      ],
    };

    await request(server)
      .put(rosterEndpoint)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('If-Match', '"1"')
      .set('Idempotency-Key', 'money-roster-member-denied-01')
      .send(guestCouple)
      .expect(403);

    const guestRoster = await request(server)
      .put(rosterEndpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .set('Idempotency-Key', 'money-roster-guest-couple-01')
      .send(guestCouple)
      .expect('ETag', '"2"')
      .expect(200);
    expect(guestRoster.body).toMatchObject({
      residentCount: 3,
      billingUnitCount: 2,
      canManage: true,
      version: 2,
      couples: [
        {
          primaryMembershipId: ownerMembershipId,
          partnerMembershipId: null,
          partnerDisplayName: 'Taylor without app access',
        },
      ],
    });

    const replay = await request(server)
      .put(rosterEndpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"1"')
      .set('Idempotency-Key', 'money-roster-guest-couple-01')
      .send(guestCouple)
      .expect(200);
    expect(replay.body).toEqual(guestRoster.body);

    const guestRent = await request(server)
      .post(`/v1/households/${householdId}/expenses`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-rent-875-guest-couple')
      .send({
        title: 'Monthly rent',
        category: 'rent',
        amount: { minorUnits: 87_500, currency: 'GBP' },
        dueDate: '2026-09-01',
      })
      .expect(201);
    const guestAllocations = readAllocations(guestRent.body);
    expect(guestAllocations).toHaveLength(2);
    expect(guestAllocations.reduce((sum, item) => sum + item.amount.minorUnits, 0)).toBe(87_500);
    const guestCoupleAllocation = guestAllocations.find((item) => item.participantCount === 2);
    expect(guestCoupleAllocation).toMatchObject({
      billingUnitType: 'couple',
      participantCount: 2,
      eligibleMembershipIds: [ownerMembershipId],
      isCurrentUser: true,
      canDeclarePayment: true,
    });
    expect(guestCoupleAllocation?.displayName).toContain('Taylor without app access');
    expect(guestCoupleAllocation?.amount.minorUnits).toBeGreaterThanOrEqual(58_332);
    expect(guestCoupleAllocation?.amount.minorUnits).toBeLessThanOrEqual(58_334);

    const accountCouple = {
      couples: [
        {
          primaryMembershipId: ownerMembershipId,
          partnerMembershipId: memberMembershipId,
        },
      ],
    };
    const accountRoster = await request(server)
      .put(rosterEndpoint)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('If-Match', '"2"')
      .set('Idempotency-Key', 'money-roster-account-couple-1')
      .send(accountCouple)
      .expect('ETag', '"3"')
      .expect(200);
    expect(accountRoster.body).toMatchObject({ residentCount: 2, billingUnitCount: 1, version: 3 });

    const accountRent = await request(server)
      .post(`/v1/households/${householdId}/expenses`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'money-rent-875-account-couple')
      .send({
        title: 'Monthly rent for couple',
        category: 'rent',
        amount: { minorUnits: 87_500, currency: 'GBP' },
        dueDate: '2026-10-01',
      })
      .expect(201);
    expect(readAllocations(accountRent.body)).toEqual([
      expect.objectContaining({
        amount: { minorUnits: 87_500, currency: 'GBP' },
        billingUnitType: 'couple',
        participantCount: 2,
        eligibleMembershipIds: [memberMembershipId, ownerMembershipId].sort(),
      }),
    ]);
    const memberView = await request(server)
      .get(`/v1/households/${householdId}/expenses/${readStringProperty(accountRent.body, 'id')}`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(readAllocations(memberView.body)[0]).toMatchObject({
      isCurrentUser: true,
      canDeclarePayment: true,
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
  displayName: string;
  billingUnitType: string;
  participantCount: number;
  eligibleMembershipIds: string[];
  amount: { minorUnits: number; currency: string };
  roundingAdjustmentMinor: number;
  status: string;
  canDeclarePayment: boolean;
  isCurrentUser: boolean;
  paymentDeclarations: PaymentBody[];
}

interface PaymentBody {
  id: string;
  status: string;
  version: number;
  [key: string]: unknown;
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

function readCurrentPayment(body: unknown): PaymentBody {
  const allocation = readAllocations(body).find((item) => item.isCurrentUser);
  const payment = allocation?.paymentDeclarations.find((item) => item.status !== 'reversed');
  if (payment === undefined) throw new Error('Expected a current payment declaration.');
  return payment;
}

function readPaymentById(body: unknown, paymentId: string): PaymentBody {
  for (const allocation of readAllocations(body)) {
    const payment = allocation.paymentDeclarations.find((item) => item.id === paymentId);
    if (payment !== undefined) return payment;
  }
  throw new Error('Expected payment declaration in expense response.');
}

function readAllocationWithPayment(body: unknown, paymentId: string): AllocationBody {
  const allocation = readAllocations(body).find((item) =>
    item.paymentDeclarations.some((payment) => payment.id === paymentId),
  );
  if (allocation === undefined) throw new Error('Expected allocation for payment declaration.');
  return allocation;
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
