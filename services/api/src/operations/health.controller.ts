import { Controller, Get } from '@nestjs/common';
import { API_VERSION, type ServiceHealth } from '@sharedhouse/contracts';

import { DatabaseService } from '../database/database.service.js';

@Controller('v1/health')
export class HealthController {
  constructor(private readonly database: DatabaseService) {}

  @Get()
  getHealth(): ServiceHealth {
    return serviceHealth();
  }

  @Get('ready')
  async getReadiness(): Promise<ServiceHealth> {
    await this.database.query('SELECT 1');
    return serviceHealth();
  }
}

function serviceHealth(): ServiceHealth {
  return {
    status: 'ok',
    service: 'api',
    apiVersion: API_VERSION,
    checkedAt: new Date().toISOString(),
  };
}
