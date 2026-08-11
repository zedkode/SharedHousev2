import { Injectable } from '@nestjs/common';
import type { BillingCoupleConfiguration, BillingRosterSummary } from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { BillingRosterRepository } from './billing-roster.repository.js';

@Injectable()
export class BillingRosterService {
  constructor(private readonly repository: BillingRosterRepository) {}

  async get(userId: string, householdId: string): Promise<BillingRosterSummary> {
    const board = await this.repository.get(userId, householdId);
    if (board === null) throw problem(404, 'HOUSEHOLD_NOT_FOUND', 'The household was not found.');
    return board;
  }

  async replace(input: {
    readonly userId: string;
    readonly householdId: string;
    readonly expectedVersion: number;
    readonly idempotencyKey: string;
    readonly couples: readonly BillingCoupleConfiguration[];
  }): Promise<BillingRosterSummary> {
    const requestHash = createHash('sha256')
      .update(JSON.stringify({ householdId: input.householdId, couples: input.couples }), 'utf8')
      .digest('hex');
    const result = await this.repository.replace({
      ...input,
      requestHash,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'updated' || result.status === 'replayed') return result.roster;
    if (result.status === 'not_found')
      throw problem(404, 'HOUSEHOLD_NOT_FOUND', 'The household was not found.');
    if (result.status === 'forbidden')
      throw problem(
        403,
        'BILLING_ROSTER_FORBIDDEN',
        'Only a household owner or administrator can configure billing participants.',
      );
    if (result.status === 'version_conflict')
      throw problem(
        412,
        'BILLING_ROSTER_VERSION_CONFLICT',
        'The billing roster changed. Reload it before saving.',
      );
    if (result.status === 'invalid_roster')
      throw problem(
        409,
        'BILLING_ROSTER_INVALID',
        'Every active member can belong to at most one couple.',
      );
    throw problem(
      409,
      'IDEMPOTENCY_KEY_REUSED',
      'This idempotency key was already used for another request.',
    );
  }
}

function problem(status: number, code: string, title: string): ApiProblemException {
  return new ApiProblemException({ status, code, title });
}
