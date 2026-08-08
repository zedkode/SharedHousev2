import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { ExpensesController } from './expenses.controller.js';
import { ExpensesRepository } from './expenses.repository.js';
import { ExpensesService } from './expenses.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [ExpensesController],
  providers: [ExpensesRepository, ExpensesService],
})
export class ExpensesModule {}
