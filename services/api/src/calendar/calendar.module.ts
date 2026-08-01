import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { CalendarController } from './calendar.controller.js';
import { CalendarRepository } from './calendar.repository.js';
import { CalendarService } from './calendar.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [CalendarController],
  providers: [CalendarRepository, CalendarService],
})
export class CalendarModule {}
