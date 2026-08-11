import { Injectable } from '@nestjs/common';
import type {
  HouseholdMemberActionRequest,
  HouseholdMemberBoard,
  HouseholdMemberSummary,
} from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';
import { ApiProblemException } from '../http/api-problem.exception.js';
import {
  HouseholdMembersRepository,
  type MemberMutationResult,
} from './household-members.repository.js';

@Injectable()
export class HouseholdMembersService {
  constructor(private readonly repository: HouseholdMembersRepository) {}

  async list(userId: string, householdId: string): Promise<HouseholdMemberBoard> {
    const board = await this.repository.list(userId, householdId);
    if (board === null) throw problem(404, 'HOUSEHOLD_NOT_FOUND', 'The household was not found.');
    return board;
  }

  async action(input: {
    userId: string;
    householdId: string;
    membershipId: string;
    expectedVersion: number;
    idempotencyKey: string;
    action: HouseholdMemberActionRequest;
  }): Promise<HouseholdMemberSummary> {
    const requestHash = createHash('sha256').update(JSON.stringify(input), 'utf8').digest('hex');
    const result = await this.repository.action({
      ...input,
      requestHash,
      occurredAt: new Date().toISOString(),
    });
    return unwrap(result);
  }
}

function unwrap(result: MemberMutationResult): HouseholdMemberSummary {
  if (result.status === 'ok' || result.status === 'replayed') return result.member;
  if (result.status === 'not_found')
    throw problem(404, 'HOUSEHOLD_MEMBER_NOT_FOUND', 'The household member was not found.');
  if (result.status === 'forbidden')
    throw problem(
      403,
      'HOUSEHOLD_MEMBER_ACTION_FORBIDDEN',
      'Your household role cannot perform this member action.',
    );
  if (result.status === 'version_conflict')
    throw problem(
      412,
      'HOUSEHOLD_MEMBER_VERSION_CONFLICT',
      'The member changed. Reload the household before trying again.',
    );
  if (result.status === 'invalid_transition')
    throw problem(
      409,
      'HOUSEHOLD_MEMBER_INVALID_TRANSITION',
      'This action is not available for the member in their current state.',
    );
  throw problem(
    409,
    'IDEMPOTENCY_KEY_REUSED',
    'This idempotency key was already used for another request.',
  );
}
function problem(status: number, code: string, title: string): ApiProblemException {
  return new ApiProblemException({ status, code, title });
}
