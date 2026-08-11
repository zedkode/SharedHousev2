import { Module } from '@nestjs/common';

import { IdentityModule } from '../identity/identity.module.js';
import { ExpensesController } from './expenses.controller.js';
import { ExpensePaymentsController } from './expense-payments.controller.js';
import { ExpensePaymentsRepository } from './expense-payments.repository.js';
import { ExpensePaymentsService } from './expense-payments.service.js';
import { ExpenseTemplatesController } from './expense-templates.controller.js';
import { ExpenseTemplatesRepository } from './expense-templates.repository.js';
import { ExpenseTemplatesService } from './expense-templates.service.js';
import { ExpensesRepository } from './expenses.repository.js';
import { ExpensesService } from './expenses.service.js';

@Module({
  imports: [IdentityModule],
  controllers: [ExpensesController, ExpensePaymentsController, ExpenseTemplatesController],
  providers: [
    ExpensesRepository,
    ExpensesService,
    ExpensePaymentsRepository,
    ExpensePaymentsService,
    ExpenseTemplatesRepository,
    ExpenseTemplatesService,
  ],
})
export class ExpensesModule {}
