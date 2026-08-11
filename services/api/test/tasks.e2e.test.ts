import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { AppModule } from '../src/app.module.js';

const PASSWORD = 'A long local passphrase for 2026!';

describe('household task workflow', () => {
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
    ownerToken = await register(server, 'tasks-owner@example.test', 'Task Owner');
    memberToken = await register(server, 'tasks-member@example.test', 'Task Member');
    outsiderToken = await register(server, 'tasks-outsider@example.test', 'Task Outsider');
    householdId = readString(
      (
        await request(server)
          .post('/v1/households')
          .set('Authorization', `Bearer ${ownerToken}`)
          .set('Idempotency-Key', 'tasks-household-create-0001')
          .send(householdBody())
          .expect(201)
      ).body,
      'id',
    );
    const invitation = await request(server)
      .post(`/v1/households/${householdId}/invitations`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ role: 'member', email: 'tasks-member@example.test' })
      .expect(201);
    await request(server)
      .post(`/v1/invitations/${readString(invitation.body, 'token')}/accept`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
  });
  afterAll(async () => app.close());

  it('creates, assigns, starts and completes a task with versioned history', async () => {
    const board = await request(server)
      .get(`/v1/households/${householdId}/tasks`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    expect(board.body).toMatchObject({ canCreate: true, tasks: [] });
    const member = readMembers(board.body).find((value) => value.displayName === 'Task Member');
    if (member === undefined) throw new Error('Member option missing.');
    const created = await request(server)
      .post(`/v1/households/${householdId}/tasks`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'tasks-create-kitchen-0001')
      .send(taskBody(member.membershipId))
      .expect('ETag', '"1"')
      .expect(201);
    expect(created.body).toMatchObject({
      title: 'Clean the kitchen',
      assigneeDisplayName: 'Task Member',
      status: 'open',
      canManage: true,
      version: 1,
    });
    const taskId = readString(created.body, 'id');
    await request(server)
      .post(`/v1/households/${householdId}/tasks`)
      .set('Authorization', `Bearer ${memberToken}`)
      .set('Idempotency-Key', 'tasks-member-create-denied')
      .send(taskBody(member.membershipId))
      .expect(403);
    await request(server)
      .get(`/v1/households/${householdId}/tasks`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .expect(404);
    const memberBoard = await request(server)
      .get(`/v1/households/${householdId}/tasks`)
      .set('Authorization', `Bearer ${memberToken}`)
      .expect(200);
    expect(memberBoard.body).toMatchObject({
      canCreate: false,
      tasks: [{ id: taskId, canStart: true, canComplete: true, canRequest: true }],
    });
    const started = await act(
      server,
      memberToken,
      householdId,
      taskId,
      1,
      'tasks-start-00000001',
      { action: 'start' },
      201,
    );
    expect(started.body).toMatchObject({ status: 'in_progress', version: 2 });
    await act(
      server,
      memberToken,
      householdId,
      taskId,
      1,
      'tasks-stale-00000001',
      { action: 'complete', note: 'Everything is clean.' },
      412,
    );
    const completed = await act(
      server,
      memberToken,
      householdId,
      taskId,
      2,
      'tasks-complete-00001',
      { action: 'complete', note: 'Everything is clean.' },
      201,
    );
    expect(completed.body).toMatchObject({
      status: 'completed',
      completionNote: 'Everything is clean.',
      version: 3,
    });
    const reopened = await act(
      server,
      ownerToken,
      householdId,
      taskId,
      3,
      'tasks-reopen-000001',
      { action: 'reopen' },
      201,
    );
    expect(reopened.body).toMatchObject({ status: 'open', completionNote: null, version: 4 });
    const exported = await request(server)
      .post('/v1/account/export')
      .set('Authorization', `Bearer ${memberToken}`)
      .send({ password: PASSWORD })
      .expect(200);
    expect(exported.body).toMatchObject({
      householdTasks: [
        { id: taskId, title: 'Clean the kitchen', assigneeDisplayName: 'Task Member' },
      ],
    });
  });

  it('commits help, swap and postpone requests only after manager decisions', async () => {
    const ownerBoard = await request(server)
      .get(`/v1/households/${householdId}/tasks`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .expect(200);
    const owner = readMembers(ownerBoard.body).find((value) => value.displayName === 'Task Owner');
    const member = readMembers(ownerBoard.body).find(
      (value) => value.displayName === 'Task Member',
    );
    if (owner === undefined || member === undefined) throw new Error('Task members missing.');
    const created = await request(server)
      .post(`/v1/households/${householdId}/tasks`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .set('Idempotency-Key', 'tasks-create-bins-0000001')
      .send({ ...taskBody(member.membershipId), title: 'Put bins outside' })
      .expect(201);
    const taskId = readString(created.body, 'id');
    const help = await act(
      server,
      memberToken,
      householdId,
      taskId,
      1,
      'tasks-help-000000001',
      { action: 'request_help', note: 'The recycling is too heavy.' },
      201,
    );
    expect(help.body).toMatchObject({
      version: 2,
      requests: [{ type: 'help', status: 'pending' }],
    });
    const helpId = readString((help.body as { requests: unknown[] }).requests[0], 'id');
    const approvedHelp = await act(
      server,
      ownerToken,
      householdId,
      taskId,
      2,
      'tasks-help-approve-01',
      { action: 'approve_request', requestId: helpId, note: 'I will assist.' },
      201,
    );
    expect(approvedHelp.body).toMatchObject({
      version: 3,
      requests: [{ type: 'help', status: 'approved' }],
    });
    const swap = await act(
      server,
      memberToken,
      householdId,
      taskId,
      3,
      'tasks-swap-000000001',
      {
        action: 'request_swap',
        note: 'I am away.',
        requestedAssigneeMembershipId: owner.membershipId,
      },
      201,
    );
    const swapId = readString((swap.body as { requests: unknown[] }).requests[0], 'id');
    const approvedSwap = await act(
      server,
      ownerToken,
      householdId,
      taskId,
      4,
      'tasks-swap-approve-01',
      { action: 'approve_request', requestId: swapId, note: 'Accepted for this occurrence.' },
      201,
    );
    expect(approvedSwap.body).toMatchObject({ assigneeDisplayName: 'Task Owner', version: 5 });
    const postpone = await act(
      server,
      ownerToken,
      householdId,
      taskId,
      5,
      'tasks-postpone-00001',
      {
        action: 'request_postpone',
        note: 'Collection moved.',
        requestedDueDate: '2026-08-20',
        requestedDueTime: '20:00',
      },
      201,
    );
    const postponeId = readString((postpone.body as { requests: unknown[] }).requests[0], 'id');
    const approvedPostpone = await act(
      server,
      ownerToken,
      householdId,
      taskId,
      6,
      'tasks-postpone-ok-001',
      { action: 'approve_request', requestId: postponeId, note: 'New date confirmed.' },
      201,
    );
    expect(approvedPostpone.body).toMatchObject({
      dueDate: '2026-08-20',
      dueTime: '20:00',
      version: 7,
    });
  });
});

async function act(
  server: Server,
  token: string,
  householdId: string,
  taskId: string,
  version: number,
  key: string,
  body: object,
  expected: number,
) {
  return request(server)
    .post(`/v1/households/${householdId}/tasks/${taskId}/actions`)
    .set('Authorization', `Bearer ${token}`)
    .set('If-Match', `"${String(version)}"`)
    .set('Idempotency-Key', key)
    .send(body)
    .expect(expected);
}
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
  const verified = await request(server)
    .post('/v1/auth/verify-email')
    .send({
      email,
      code: readString(registration.body, 'developmentVerificationCode'),
      deviceName: 'Tasks API tests',
    })
    .expect(200);
  return readString(verified.body, 'accessToken');
}
function householdBody(): object {
  return {
    name: 'Tasks home',
    countryCode: 'GB',
    timezone: 'Europe/London',
    currency: 'GBP',
    firstDayOfWeek: 1,
    cycleType: 'calendar_month',
    cycleAnchor: '2026-08-01',
  };
}
function taskBody(assigneeMembershipId: string): object {
  return {
    title: 'Clean the kitchen',
    instructions: 'Wipe surfaces and mop the floor.',
    zone: 'Kitchen',
    priority: 'high',
    dueDate: '2026-08-15',
    dueTime: '18:30',
    estimatedMinutes: 35,
    assigneeMembershipId,
  };
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
function readMembers(value: unknown): { membershipId: string; displayName: string }[] {
  if (
    typeof value !== 'object' ||
    value === null ||
    !('members' in value) ||
    !Array.isArray(value.members)
  )
    throw new Error('Missing members.');
  return value.members as { membershipId: string; displayName: string }[];
}
