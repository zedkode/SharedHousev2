import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { ChatController } from './chat.controller.js';
import { ChatRepository } from './chat.repository.js';
import { ChatService } from './chat.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [ChatController],
  providers: [ChatRepository, ChatService],
})
export class ChatModule {}
