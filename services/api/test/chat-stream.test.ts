import type { HouseholdChatMessage } from '@sharedhouse/contracts';
import { firstValueFrom } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import type { ChatRepository } from '../src/chat/chat.repository.js';
import { ChatService } from '../src/chat/chat.service.js';

describe('ChatService stream', () => {
  it('emits the first authorised incremental message as an SSE message event', async () => {
    const message: HouseholdChatMessage = {
      id: '018f0000-0000-7000-8000-000000000077',
      householdId: '018f0000-0000-7000-8000-000000000002',
      senderMembershipId: '018f0000-0000-7000-8000-000000000003',
      senderUserId: '018f0000-0000-7000-8000-000000000001',
      senderDisplayName: 'Alex',
      isCurrentUser: true,
      body: 'Kitchen is finished.',
      createdAt: '2026-08-11T12:00:00Z',
    };
    const list = vi.fn().mockResolvedValue({
      status: 'ok',
      page: { messages: [message], nextCursor: message.id },
    });
    const repository = { list } as unknown as ChatRepository;
    const service = new ChatService(repository);

    const event = await firstValueFrom(
      service.stream('018f0000-0000-7000-8000-000000000001', message.householdId, null),
    );

    expect(event).toEqual({ id: message.id, type: 'message', data: message });
    expect(list).toHaveBeenCalledWith({
      userId: '018f0000-0000-7000-8000-000000000001',
      householdId: message.householdId,
      after: null,
      limit: 100,
    });
  });
});
