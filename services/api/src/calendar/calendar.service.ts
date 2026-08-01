import { Injectable } from '@nestjs/common';
import type { CalendarEventConfiguration, CalendarEventSummary } from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { CalendarRepository } from './calendar.repository.js';

@Injectable()
export class CalendarService {
  constructor(private readonly repository: CalendarRepository) {}

  async list(
    userId: string,
    householdId: string,
    from: string,
    to: string,
  ): Promise<readonly CalendarEventSummary[]> {
    const result = await this.repository.list({ userId, householdId, from, to });
    if (result.status === 'not_found') {
      throw householdNotFound();
    }
    return result.events;
  }

  async create(
    userId: string,
    householdId: string,
    configuration: CalendarEventConfiguration,
    idempotencyKey: string,
  ): Promise<CalendarEventSummary> {
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
    if (result.status === 'created' || result.status === 'replayed') {
      return result.event;
    }
    if (result.status === 'not_found') {
      throw householdNotFound();
    }
    if (result.status === 'forbidden') {
      throw writeForbidden();
    }
    throw new ApiProblemException({
      status: 409,
      code: 'IDEMPOTENCY_KEY_REUSED',
      title: 'This idempotency key was already used for another request.',
    });
  }

  async update(
    userId: string,
    householdId: string,
    eventId: string,
    expectedVersion: number,
    configuration: CalendarEventConfiguration,
  ): Promise<CalendarEventSummary> {
    const result = await this.repository.update({
      userId,
      householdId,
      eventId,
      expectedVersion,
      configuration,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'updated') {
      return result.event;
    }
    if (result.status === 'not_found') {
      throw eventNotFound();
    }
    if (result.status === 'forbidden') {
      throw writeForbidden();
    }
    throw versionConflict();
  }

  async delete(
    userId: string,
    householdId: string,
    eventId: string,
    expectedVersion: number,
  ): Promise<void> {
    const result = await this.repository.delete({
      userId,
      householdId,
      eventId,
      expectedVersion,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'not_found') {
      throw eventNotFound();
    }
    if (result.status === 'forbidden') {
      throw writeForbidden();
    }
    if (result.status === 'version_conflict') {
      throw versionConflict();
    }
  }
}

function householdNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'HOUSEHOLD_NOT_FOUND',
    title: 'The household was not found.',
  });
}

function eventNotFound(): ApiProblemException {
  return new ApiProblemException({
    status: 404,
    code: 'CALENDAR_EVENT_NOT_FOUND',
    title: 'The calendar event was not found.',
  });
}

function writeForbidden(): ApiProblemException {
  return new ApiProblemException({
    status: 403,
    code: 'CALENDAR_EVENT_WRITE_FORBIDDEN',
    title: 'Your household role cannot change this calendar event.',
  });
}

function versionConflict(): ApiProblemException {
  return new ApiProblemException({
    status: 412,
    code: 'CALENDAR_EVENT_VERSION_CONFLICT',
    title: 'The calendar event changed. Reload it before saving again.',
  });
}
