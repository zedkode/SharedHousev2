import { Injectable } from '@nestjs/common';
import type { ExpenseTemplateConfiguration, ExpenseTemplateSummary } from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { ExpenseTemplatesRepository } from './expense-templates.repository.js';

@Injectable()
export class ExpenseTemplatesService {
  constructor(private readonly repository: ExpenseTemplatesRepository) {}

  async list(userId: string, householdId: string): Promise<readonly ExpenseTemplateSummary[]> {
    const result = await this.repository.list(userId, householdId);
    if (result.status === 'not_found')
      throw templateProblem(404, 'HOUSEHOLD_NOT_FOUND', 'The household was not found.');
    return result.templates;
  }

  async create(
    userId: string,
    householdId: string,
    configuration: ExpenseTemplateConfiguration,
    idempotencyKey: string,
  ): Promise<ExpenseTemplateSummary> {
    const requestHash = hashRequest(householdId, configuration);
    const result = await this.repository.create({
      userId,
      householdId,
      configuration,
      idempotencyKey,
      requestHash,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'created' || result.status === 'replayed') return result.template;
    return reject(result.status);
  }

  async update(
    userId: string,
    householdId: string,
    templateId: string,
    expectedVersion: number,
    configuration: ExpenseTemplateConfiguration,
  ): Promise<ExpenseTemplateSummary> {
    const result = await this.repository.update({
      userId,
      householdId,
      templateId,
      expectedVersion,
      configuration,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'updated') return result.template;
    return reject(result.status);
  }

  async archive(
    userId: string,
    householdId: string,
    templateId: string,
    expectedVersion: number,
    reason: string,
  ): Promise<ExpenseTemplateSummary> {
    const result = await this.repository.archive({
      userId,
      householdId,
      templateId,
      expectedVersion,
      reason,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'updated') return result.template;
    return reject(result.status);
  }
}

function hashRequest(householdId: string, configuration: ExpenseTemplateConfiguration): string {
  return createHash('sha256')
    .update(JSON.stringify({ householdId, configuration }), 'utf8')
    .digest('hex');
}

function reject(
  status:
    | 'not_found'
    | 'forbidden'
    | 'currency_mismatch'
    | 'idempotency_conflict'
    | 'version_conflict'
    | 'status_conflict',
): never {
  switch (status) {
    case 'not_found':
      throw templateProblem(404, 'EXPENSE_TEMPLATE_NOT_FOUND', 'The household cost was not found.');
    case 'forbidden':
      throw templateProblem(
        403,
        'EXPENSE_TEMPLATE_MANAGE_FORBIDDEN',
        'Only a household owner or administrator can manage household costs.',
      );
    case 'currency_mismatch':
      throw templateProblem(
        409,
        'EXPENSE_TEMPLATE_CURRENCY_MISMATCH',
        'Use the household settlement currency.',
      );
    case 'idempotency_conflict':
      throw templateProblem(
        409,
        'IDEMPOTENCY_KEY_REUSED',
        'This idempotency key was already used for another request.',
      );
    case 'version_conflict':
      throw templateProblem(
        412,
        'EXPENSE_TEMPLATE_VERSION_CONFLICT',
        'The household cost changed. Reload it before continuing.',
      );
    case 'status_conflict':
      throw templateProblem(
        409,
        'EXPENSE_TEMPLATE_STATUS_CONFLICT',
        'This archived household cost cannot be changed.',
      );
  }
}

function templateProblem(status: number, code: string, title: string): ApiProblemException {
  return new ApiProblemException({ status, code, title });
}
