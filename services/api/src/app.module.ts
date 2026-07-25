import { Module } from '@nestjs/common';

import { HealthModule } from './operations/health.module.js';

@Module({
  imports: [HealthModule],
})
export class AppModule {}
