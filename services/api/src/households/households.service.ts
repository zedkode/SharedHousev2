import { Injectable } from '@nestjs/common';
import type { HouseholdConfiguration, HouseholdSummary } from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { HouseholdsRepository } from './households.repository.js';

@Injectable()
export class HouseholdsService {
  constructor(private readonly repository: HouseholdsRepository) {}

  async create(
    userId: string,
    configuration: HouseholdConfiguration,
    idempotencyKey: string,
  ): Promise<HouseholdSummary> {
    const requestHash = createHash('sha256')
      .update(JSON.stringify(configuration), 'utf8')
      .digest('hex');
    const result = await this.repository.createHousehold({
      userId,
      configuration,
      idempotencyKey,
      requestHash,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'idempotency_conflict') {
      throw new ApiProblemException({
        status: 409,
        code: 'IDEMPOTENCY_KEY_REUSED',
        title: 'This idempotency key was already used for another request.',
      });
    }
    return result.household;
  }

  async list(userId: string): Promise<readonly HouseholdSummary[]> {
    return this.repository.listForUser(userId);
  }

  async get(userId: string, householdId: string): Promise<HouseholdSummary> {
    const household = await this.repository.findForUser(userId, householdId);
    if (household === null) {
      throw householdNotFound();
    }
    return household;
  }

  async update(
    userId: string,
    householdId: string,
    expectedVersion: number,
    configuration: HouseholdConfiguration,
  ): Promise<HouseholdSummary> {
    const result = await this.repository.updateHousehold({
      userId,
      householdId,
      expectedVersion,
      configuration,
      occurredAt: new Date().toISOString(),
    });
    if (result.status !== 'updated') {
      if (result.status === 'not_found') {
        throw householdNotFound();
      }
      if (result.status === 'forbidden') {
        throw new ApiProblemException({
          status: 403,
          code: 'HOUSEHOLD_UPDATE_FORBIDDEN',
          title: 'Your household role cannot change these settings.',
        });
      }
      throw new ApiProblemException({
        status: 412,
        code: 'HOUSEHOLD_VERSION_CONFLICT',
        title: 'The household changed. Reload it before saving again.',
      });
    }
    return result.household;
  }
}

function householdNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'HOUSEHOLD_NOT_FOUND',
    title: 'The household was not found.',
  });
}
