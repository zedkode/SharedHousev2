import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { HouseholdsController } from './households.controller.js';
import { HouseholdsRepository } from './households.repository.js';
import { HouseholdsService } from './households.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [HouseholdsController],
  providers: [HouseholdsRepository, HouseholdsService],
})
export class HouseholdsModule {}
