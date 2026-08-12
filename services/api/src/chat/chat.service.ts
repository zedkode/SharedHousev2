import { Injectable, type MessageEvent } from '@nestjs/common';
import type { HouseholdChatMessage, HouseholdChatPage } from '@sharedhouse/contracts';
import { createHash } from 'node:crypto';
import { Observable } from 'rxjs';

import { ApiProblemException } from '../http/api-problem.exception.js';
import { ChatRepository } from './chat.repository.js';

@Injectable()
export class ChatService {
  constructor(private readonly repository: ChatRepository) {}

  async list(
    userId: string,
    householdId: string,
    after: string | null,
    limit: number,
  ): Promise<HouseholdChatPage> {
    const result = await this.repository.list({ userId, householdId, after, limit });
    if (result.status === 'not_found') throw householdNotFound();
    return result.page;
  }

  async create(
    userId: string,
    householdId: string,
    body: string,
    idempotencyKey: string,
  ): Promise<HouseholdChatMessage> {
    const requestHash = createHash('sha256')
      .update(JSON.stringify({ householdId, body }), 'utf8')
      .digest('hex');
    const result = await this.repository.create({
      userId,
      householdId,
      body,
      idempotencyKey,
      requestHash,
      occurredAt: new Date().toISOString(),
    });
    if (result.status === 'created' || result.status === 'replayed') return result.message;
    if (result.status === 'not_found') throw householdNotFound();
    if (result.status === 'forbidden')
      throw new ApiProblemException({
        status: 403,
        code: 'HOUSEHOLD_CHAT_WRITE_FORBIDDEN',
        title: 'Your household role cannot send chat messages.',
      });
    throw new ApiProblemException({
      status: 409,
      code: 'IDEMPOTENCY_KEY_REUSED',
      title: 'This idempotency key was already used for another request.',
    });
  }

  stream(userId: string, householdId: string, after: string | null): Observable<MessageEvent> {
    return new Observable<MessageEvent>((subscriber) => {
      let cursor = after;
      let polling = false;
      let stopped = false;
      let heartbeatCount = 0;
      const poll = async (): Promise<void> => {
        if (polling || stopped) return;
        polling = true;
        try {
          const page = await this.list(userId, householdId, cursor, 100);
          for (const message of page.messages) {
            subscriber.next({ id: message.id, type: 'message', data: message });
          }
          cursor = page.nextCursor;
          heartbeatCount += 1;
          if (page.messages.length === 0 && heartbeatCount % 5 === 0) {
            subscriber.next({
              type: 'heartbeat',
              data: { checkedAt: new Date().toISOString() },
            });
          }
        } catch (error: unknown) {
          subscriber.error(error);
        } finally {
          polling = false;
        }
      };
      void poll();
      const timer = setInterval(() => void poll(), 1_500);
      return () => {
        stopped = true;
        clearInterval(timer);
      };
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
