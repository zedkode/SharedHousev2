import { Module } from '@nestjs/common';
import { APP_FILTER, APP_GUARD } from '@nestjs/core';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';

import { DatabaseModule } from './database/database.module.js';
import { CalendarModule } from './calendar/calendar.module.js';
import { EmailModule } from './email/email.module.js';
import { ExpensesModule } from './expenses/expenses.module.js';
import { HouseholdsModule } from './households/households.module.js';
import { ProblemDetailsFilter } from './http/problem-details.filter.js';
import { IdentityModule } from './identity/identity.module.js';
import { InvitationsModule } from './invitations/invitations.module.js';
import { HealthModule } from './operations/health.module.js';

@Module({
  imports: [
    DatabaseModule,
    CalendarModule,
    EmailModule,
    ExpensesModule,
    ThrottlerModule.forRoot([{ name: 'default', ttl: 60_000, limit: 120 }]),
    HealthModule,
    IdentityModule,
    InvitationsModule,
    HouseholdsModule,
  ],
  providers: [
    { provide: APP_GUARD, useClass: ThrottlerGuard },
    { provide: APP_FILTER, useClass: ProblemDetailsFilter },
  ],
})
export class AppModule {}
