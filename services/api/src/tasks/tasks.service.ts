import { Injectable } from '@nestjs/common';
import type {
  HouseholdTaskActionRequest,
  HouseholdTaskBoard,
  HouseholdTaskConfiguration,
  HouseholdTaskSummary,
} from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';
import { ApiProblemException } from '../http/api-problem.exception.js';
import { TasksRepository } from './tasks.repository.js';

@Injectable()
export class TasksService {
  constructor(private readonly repository: TasksRepository) {}
  async list(userId: string, householdId: string): Promise<HouseholdTaskBoard> {
    const result = await this.repository.list(userId, householdId);
    if (result === null) throw problem(404, 'HOUSEHOLD_NOT_FOUND', 'The household was not found.');
    return result;
  }
  async create(
    userId: string,
    householdId: string,
    configuration: HouseholdTaskConfiguration,
    idempotencyKey: string,
  ): Promise<HouseholdTaskSummary> {
    const result = await this.repository.create({
      userId,
      householdId,
      configuration,
      idempotencyKey,
      requestHash: hash({ householdId, configuration }),
      occurredAt: new Date().toISOString(),
    });
    return unwrap(result);
  }
  async action(
    userId: string,
    householdId: string,
    taskId: string,
    expectedVersion: number,
    action: HouseholdTaskActionRequest,
    idempotencyKey: string,
  ): Promise<HouseholdTaskSummary> {
    const result = await this.repository.action({
      userId,
      householdId,
      taskId,
      expectedVersion,
      action,
      idempotencyKey,
      requestHash: hash({ householdId, taskId, expectedVersion, action }),
      occurredAt: new Date().toISOString(),
    });
    return unwrap(result);
  }
}

function hash(value: unknown): string {
  return createHash('sha256').update(JSON.stringify(value), 'utf8').digest('hex');
}
function unwrap(result: {
  readonly status: string;
  readonly task?: HouseholdTaskSummary;
}): HouseholdTaskSummary {
  if ((result.status === 'ok' || result.status === 'replayed') && result.task !== undefined)
    return result.task;
  if (result.status === 'not_found')
    throw problem(404, 'HOUSEHOLD_TASK_NOT_FOUND', 'The household task was not found.');
  if (result.status === 'forbidden')
    throw problem(
      403,
      'HOUSEHOLD_TASK_ACTION_FORBIDDEN',
      'Your household role cannot perform this task action.',
    );
  if (result.status === 'version_conflict')
    throw problem(
      412,
      'HOUSEHOLD_TASK_VERSION_CONFLICT',
      'The task changed. Reload it before trying again.',
    );
  if (result.status === 'invalid_transition')
    throw problem(
      409,
      'HOUSEHOLD_TASK_INVALID_TRANSITION',
      'This action is not available in the task’s current state.',
    );
  if (result.status === 'request_conflict')
    throw problem(
      409,
      'HOUSEHOLD_TASK_REQUEST_CONFLICT',
      'A matching request is already pending or is no longer pending.',
    );
  if (result.status === 'invalid_member')
    throw problem(
      422,
      'HOUSEHOLD_TASK_INVALID_ASSIGNEE',
      'Choose an active writable household member.',
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
