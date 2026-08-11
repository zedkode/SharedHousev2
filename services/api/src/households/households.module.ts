import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { HouseholdsController } from './households.controller.js';
import { HouseholdsRepository } from './households.repository.js';
import { HouseholdsService } from './households.service.js';
import { HouseholdMembersController } from './household-members.controller.js';
import { HouseholdMembersRepository } from './household-members.repository.js';
import { HouseholdMembersService } from './household-members.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [HouseholdsController, HouseholdMembersController],
  providers: [
    HouseholdsRepository,
    HouseholdsService,
    HouseholdMembersRepository,
    HouseholdMembersService,
  ],
})
export class HouseholdsModule {}
