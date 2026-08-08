import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { HouseholdInvitationsController, InvitationsController } from './invitations.controller.js';
import { InvitationsRepository } from './invitations.repository.js';
import { InvitationsService } from './invitations.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [HouseholdInvitationsController, InvitationsController],
  providers: [InvitationsRepository, InvitationsService],
})
export class InvitationsModule {}
