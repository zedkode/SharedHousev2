import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { ExpensesController } from './expenses.controller.js';
import { ExpenseTemplatesController } from './expense-templates.controller.js';
import { ExpenseTemplatesRepository } from './expense-templates.repository.js';
import { ExpenseTemplatesService } from './expense-templates.service.js';
import { ExpensesRepository } from './expenses.repository.js';
import { ExpensesService } from './expenses.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [ExpensesController, ExpenseTemplatesController],
  providers: [
    ExpensesRepository,
    ExpensesService,
    ExpenseTemplatesRepository,
    ExpenseTemplatesService,
  ],
})
export class ExpensesModule {}
