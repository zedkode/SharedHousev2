import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import type { Server } from 'node:http';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { AppModule } from '../src/app.module.js';

const PASSWORD = 'A long local passphrase for 2026!';

describe('household member administration', () => {
  let app: INestApplication;
  let server: Server;
  let ownerToken: string;
  let adminToken: string;
  let memberToken: string;
  let readOnlyToken: string;
  let outsiderToken: string;
  let householdId: string;

  beforeAll(async () => {
    const module = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = module.createNestApplication();
    await app.init();
    server = app.getHttpServer() as Server;
    ownerToken = await register(server, 'members-owner@example.test', 'Owner Person');
    adminToken = await register(server, 'members-admin@example.test', 'Admin Person');
    memberToken = await register(server, 'members-member@example.test', 'Member Person');
    readOnlyToken = await register(server, 'members-reader@example.test', 'Reader Person');
    outsiderToken = await register(server, 'members-outsider@example.test', 'Outsider Person');
    householdId = readString(
      (
        await request(server)
          .post('/v1/households')
          .set('Authorization', `Bearer ${ownerToken}`)
          .set('Idempotency-Key', 'members-household-create-0001')
          .send(householdBody())
          .expect(201)
      ).body,
      'id',
    );
    await inviteAndAccept(
      server,
      ownerToken,
      adminToken,
      householdId,
      'admin',
      'members-admin@example.test',
    );
    await inviteAndAccept(
      server,
      ownerToken,
      memberToken,
      householdId,
      'member',
      'members-member@example.test',
    );
    await inviteAndAccept(
      server,
      ownerToken,
      readOnlyToken,
      householdId,
      'read_only',
      'members-reader@example.test',
    );
  });

  afterAll(async () => app.close());

  it('exposes privacy-safe membership capabilities and isolates other households', async () => {
    const ownerBoard = await board(server, ownerToken, householdId, 200);
    expect(ownerBoard.body).toMatchObject({ canInvite: true, canEditHousehold: true });
    const members = readMembers(ownerBoard.body);
    expect(members).toHaveLength(4);
    expect(JSON.stringify(ownerBoard.body)).not.toContain('@example.test');
    expect(members.find((member) => member.displayName === 'Owner Person')).toMatchObject({
      role: 'owner',
      isCurrentUser: true,
      canRemove: false,
    });
    expect(members.find((member) => member.displayName === 'Admin Person')).toMatchObject({
      role: 'admin',
      canChangeRole: true,
      canTransferOwnership: true,
    });

    const memberBoard = await board(server, memberToken, householdId, 200);
    expect(memberBoard.body).toMatchObject({ canInvite: false, canEditHousehold: false });
    expect(readMembers(memberBoard.body).every((member) => !member.canRemove)).toBe(true);
    await board(server, outsiderToken, householdId, 404);
  });

  it('enforces delegation, optimistic locking, suspension, reactivation and removal', async () => {
    const initial = readMembers((await board(server, ownerToken, householdId, 200)).body);
    const admin = requiredMember(initial, 'Admin Person');
    const member = requiredMember(initial, 'Member Person');
    const reader = requiredMember(initial, 'Reader Person');

    await act(
      server,
      adminToken,
      householdId,
      admin.membershipId,
      admin.version,
      'members-admin-self-denied-01',
      { action: 'change_role', role: 'member' },
      403,
    );
    await act(
      server,
      adminToken,
      householdId,
      member.membershipId,
      member.version,
      'members-admin-escalate-0001',
      { action: 'change_role', role: 'admin' },
      403,
    );
    const promoted = await act(
      server,
      ownerToken,
      householdId,
      member.membershipId,
      member.version,
      'members-owner-promote-0001',
      { action: 'change_role', role: 'admin' },
      200,
    );
    expect(promoted.body).toMatchObject({ role: 'admin', version: member.version + 1 });
    await act(
      server,
      ownerToken,
      householdId,
      member.membershipId,
      member.version,
      'members-stale-change-00001',
      { action: 'change_role', role: 'member' },
      412,
    );

    const suspended = await act(
      server,
      ownerToken,
      householdId,
      reader.membershipId,
      reader.version,
      'members-suspend-reader-001',
      { action: 'suspend' },
      200,
    );
    expect(suspended.body).toMatchObject({ status: 'suspended', version: reader.version + 1 });
    await board(server, readOnlyToken, householdId, 404);
    const replay = await act(
      server,
      ownerToken,
      householdId,
      reader.membershipId,
      reader.version,
      'members-suspend-reader-001',
      { action: 'suspend' },
      200,
    );
    expect(replay.body).toEqual(suspended.body);
    const reactivated = await act(
      server,
      ownerToken,
      householdId,
      reader.membershipId,
      reader.version + 1,
      'members-reactivate-reader1',
      { action: 'reactivate' },
      200,
    );
    expect(reactivated.body).toMatchObject({ status: 'active', version: reader.version + 2 });
    await board(server, readOnlyToken, householdId, 200);
    const removed = await act(
      server,
      ownerToken,
      householdId,
      reader.membershipId,
      reader.version + 2,
      'members-remove-reader-0001',
      { action: 'remove' },
      200,
    );
    expect(removed.body).toMatchObject({ status: 'removed', version: reader.version + 3 });
    await board(server, readOnlyToken, householdId, 404);
  });

  it('transfers exactly one active owner atomically and changes authority immediately', async () => {
    const members = readMembers((await board(server, ownerToken, householdId, 200)).body);
    const admin = requiredMember(members, 'Admin Person');
    const transferred = await act(
      server,
      ownerToken,
      householdId,
      admin.membershipId,
      admin.version,
      'members-transfer-owner-0001',
      { action: 'transfer_ownership' },
      200,
    );
    expect(transferred.body).toMatchObject({ role: 'owner', status: 'active' });

    const newOwnerView = readMembers((await board(server, adminToken, householdId, 200)).body);
    expect(
      newOwnerView.filter((member) => member.role === 'owner' && member.status === 'active'),
    ).toHaveLength(1);
    expect(requiredMember(newOwnerView, 'Owner Person')).toMatchObject({ role: 'admin' });
    expect(requiredMember(newOwnerView, 'Admin Person')).toMatchObject({
      role: 'owner',
      isCurrentUser: true,
    });
    await act(
      server,
      ownerToken,
      householdId,
      admin.membershipId,
      admin.version + 1,
      'members-old-owner-denied-01',
      { action: 'transfer_ownership' },
      403,
    );
  });
});

async function board(server: Server, token: string, householdId: string, status: number) {
  return request(server)
    .get(`/v1/households/${householdId}/members`)
    .set('Authorization', `Bearer ${token}`)
    .expect(status);
}
async function act(
  server: Server,
  token: string,
  householdId: string,
  membershipId: string,
  version: number,
  key: string,
  body: object,
  status: number,
) {
  return request(server)
    .post(`/v1/households/${householdId}/members/${membershipId}/actions`)
    .set('Authorization', `Bearer ${token}`)
    .set('If-Match', `"${String(version)}"`)
    .set('Idempotency-Key', key)
    .send(body)
    .expect(status);
}
async function inviteAndAccept(
  server: Server,
  ownerToken: string,
  token: string,
  householdId: string,
  role: string,
  email: string,
): Promise<void> {
  const invitation = await request(server)
    .post(`/v1/households/${householdId}/invitations`)
    .set('Authorization', `Bearer ${ownerToken}`)
    .send({ role, email })
    .expect(201);
  await request(server)
    .post(`/v1/invitations/${readString(invitation.body, 'token')}/accept`)
    .set('Authorization', `Bearer ${token}`)
    .expect(200);
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
      deviceName: 'Member admin tests',
    })
    .expect(200);
  return readString(verified.body, 'accessToken');
}
function householdBody(): object {
  return {
    name: 'Member admin home',
    countryCode: 'GB',
    timezone: 'Europe/London',
    currency: 'GBP',
    firstDayOfWeek: 1,
    cycleType: 'calendar_month',
    cycleAnchor: '2026-08-01',
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
interface Member {
  membershipId: string;
  displayName: string;
  role: string;
  status: string;
  version: number;
  canRemove: boolean;
}
function readMembers(value: unknown): Member[] {
  if (
    typeof value !== 'object' ||
    value === null ||
    !('members' in value) ||
    !Array.isArray(value.members)
  )
    throw new Error('Missing members.');
  return value.members as Member[];
}
function requiredMember(members: Member[], displayName: string): Member {
  const member = members.find((value) => value.displayName === displayName);
  if (member === undefined) throw new Error(`Missing ${displayName}.`);
  return member;
}
