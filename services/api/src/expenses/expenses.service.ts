import { Injectable } from '@nestjs/common';
import type { ExpenseConfiguration, ExpenseSummary } from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { ExpensesRepository } from './expenses.repository.js';

@Injectable()
export class ExpensesService {
  constructor(private readonly repository: ExpensesRepository) {}

  async list(userId: string, householdId: string): Promise<readonly ExpenseSummary[]> {
    const result = await this.repository.list({ userId, householdId });
    if (result.status === 'not_found') throw householdNotFound();
    return result.expenses;
  }

  async get(userId: string, householdId: string, expenseId: string): Promise<ExpenseSummary> {
    const result = await this.repository.get({ userId, householdId, expenseId });
    if (result.status === 'not_found') throw expenseNotFound();
    return result.expense;
  }

  async create(
    userId: string,
    householdId: string,
    configuration: ExpenseConfiguration,
    idempotencyKey: string,
  ): Promise<ExpenseSummary> {
    const requestHash = createHash('sha256')
      .update(JSON.stringify({ householdId, configuration }), 'utf8')
      .digest('hex');
    const result = await this.repository.create({
      userId,
      householdId,
      configuration,
      idempotencyKey,
      requestHash,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'created' || result.status === 'replayed') return result.expense;
    if (result.status === 'not_found') throw householdNotFound();
    if (result.status === 'forbidden') throw writeForbidden();
    if (result.status === 'currency_mismatch') {
      throw new ApiProblemException({
        status: 409,
        code: 'EXPENSE_CURRENCY_MISMATCH',
        title: 'Use the household settlement currency for this expense.',
      });
    }
    throw new ApiProblemException({
      status: 409,
      code: 'IDEMPOTENCY_KEY_REUSED',
      title: 'This idempotency key was already used for another request.',
    });
  }

  approve(
    userId: string,
    householdId: string,
    expenseId: string,
    expectedVersion: number,
  ): Promise<ExpenseSummary> {
    return this.transition(userId, householdId, expenseId, expectedVersion, 'approve', null);
  }

  reverse(
    userId: string,
    householdId: string,
    expenseId: string,
    expectedVersion: number,
    reason: string,
  ): Promise<ExpenseSummary> {
    return this.transition(userId, householdId, expenseId, expectedVersion, 'reverse', reason);
  }

  private async transition(
    userId: string,
    householdId: string,
    expenseId: string,
    expectedVersion: number,
    action: 'approve' | 'reverse',
    reason: string | null,
  ): Promise<ExpenseSummary> {
    const result = await this.repository.transition({
      userId,
      householdId,
      expenseId,
      expectedVersion,
      action,
      reason,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'updated') return result.expense;
    if (result.status === 'not_found') throw expenseNotFound();
    if (result.status === 'forbidden') throw writeForbidden();
    if (result.status === 'version_conflict') {
      throw new ApiProblemException({
        status: 412,
        code: 'EXPENSE_VERSION_CONFLICT',
        title: 'The expense changed. Reload it before continuing.',
      });
    }
    throw new ApiProblemException({
      status: 409,
      code: 'EXPENSE_STATUS_CONFLICT',
      title: 'This expense cannot make the requested status change.',
    });
  }
}

function householdNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'HOUSEHOLD_NOT_FOUND',
    title: 'The household was not found.',
  });
}

function expenseNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'EXPENSE_NOT_FOUND',
    title: 'The expense was not found.',
  });
}

function writeForbidden(): ApiProblemException {
  return new ApiProblemException({
    status: 403,
    code: 'EXPENSE_WRITE_FORBIDDEN',
    title: 'Your household role cannot perform this expense action.',
  });
}
