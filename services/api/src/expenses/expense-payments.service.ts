import { Injectable } from '@nestjs/common';
import type { ExpensePaymentDeclarationRequest, ExpenseSummary } from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { ExpensePaymentsRepository, type PaymentAction } from './expense-payments.repository.js';
import { ExpensesRepository } from './expenses.repository.js';

export interface ExpensePaymentMutation {
  readonly expense: ExpenseSummary;
  readonly paymentVersion: number;
}

@Injectable()
export class ExpensePaymentsService {
  constructor(
    private readonly payments: ExpensePaymentsRepository,
    private readonly expenses: ExpensesRepository,
  ) {}

  async declare(
    userId: string,
    householdId: string,
    expenseId: string,
    configuration: ExpensePaymentDeclarationRequest,
    idempotencyKey: string,
  ): Promise<ExpensePaymentMutation> {
    const now = new Date();
    if (Date.parse(configuration.paidAt) > now.getTime() + 5 * 60 * 1000) {
      throw new ApiProblemException({
        status: 400,
        code: 'PAYMENT_DATE_IN_FUTURE',
        title: 'Payment time cannot be more than five minutes in the future.',
      });
    }
    const requestHash = createHash('sha256')
      .update(JSON.stringify({ householdId, expenseId, configuration }), 'utf8')
      .digest('hex');
    const result = await this.payments.declare({
      userId,
      householdId,
      expenseId,
      configuration,
      idempotencyKey,
      requestHash,
      occurredAt: now.toISOString(),
    });
    if (result.status === 'created' || result.status === 'replayed') {
      return this.readMutation(userId, householdId, expenseId, result.paymentVersion);
    }
    if (result.status === 'not_found') throw paymentNotFound();
    if (result.status === 'forbidden') throw paymentForbidden();
    if (result.status === 'status_conflict') {
      throw new ApiProblemException({
        status: 409,
        code: 'PAYMENT_DECLARATION_CONFLICT',
        title: 'Only an outstanding share of an approved expense can be declared paid.',
      });
    }
    throw new ApiProblemException({
      status: 409,
      code: 'IDEMPOTENCY_KEY_REUSED',
      title: 'This idempotency key was already used for another payment declaration.',
    });
  }

  confirm(
    userId: string,
    householdId: string,
    expenseId: string,
    paymentId: string,
    expectedVersion: number,
  ): Promise<ExpensePaymentMutation> {
    return this.transition(
      userId,
      householdId,
      expenseId,
      paymentId,
      expectedVersion,
      'confirm',
      null,
    );
  }

  dispute(
    userId: string,
    householdId: string,
    expenseId: string,
    paymentId: string,
    expectedVersion: number,
    reason: string,
  ): Promise<ExpensePaymentMutation> {
    return this.transition(
      userId,
      householdId,
      expenseId,
      paymentId,
      expectedVersion,
      'dispute',
      reason,
    );
  }

  reverse(
    userId: string,
    householdId: string,
    expenseId: string,
    paymentId: string,
    expectedVersion: number,
    reason: string,
  ): Promise<ExpensePaymentMutation> {
    return this.transition(
      userId,
      householdId,
      expenseId,
      paymentId,
      expectedVersion,
      'reverse',
      reason,
    );
  }

  private async transition(
    userId: string,
    householdId: string,
    expenseId: string,
    paymentId: string,
    expectedVersion: number,
    action: PaymentAction,
    reason: string | null,
  ): Promise<ExpensePaymentMutation> {
    const result = await this.payments.transition({
      userId,
      householdId,
      expenseId,
      paymentId,
      expectedVersion,
      action,
      reason,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'updated') {
      return this.readMutation(userId, householdId, expenseId, result.paymentVersion);
    }
    if (result.status === 'not_found') throw paymentNotFound();
    if (result.status === 'forbidden') throw paymentForbidden();
    if (result.status === 'version_conflict') {
      throw new ApiProblemException({
        status: 412,
        code: 'PAYMENT_VERSION_CONFLICT',
        title: 'The payment declaration changed. Reload it before continuing.',
      });
    }
    throw new ApiProblemException({
      status: 409,
      code: 'PAYMENT_STATUS_CONFLICT',
      title: 'This payment declaration cannot make the requested status change.',
    });
  }

  private async readMutation(
    userId: string,
    householdId: string,
    expenseId: string,
    paymentVersion: number,
  ): Promise<ExpensePaymentMutation> {
    const result = await this.expenses.get({ userId, householdId, expenseId });
    if (result.status === 'not_found') throw paymentNotFound();
    return { expense: result.expense, paymentVersion };
  }
}

function paymentNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'PAYMENT_NOT_FOUND',
    title: 'The expense or payment declaration was not found.',
  });
}

function paymentForbidden(): ApiProblemException {
  return new ApiProblemException({
    status: 403,
    code: 'PAYMENT_WRITE_FORBIDDEN',
    title: 'Your household role cannot perform this payment action.',
  });
}
