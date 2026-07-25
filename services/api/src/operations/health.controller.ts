import { Controller, Get } from '@nestjs/common';
import { API_VERSION, type ServiceHealth } from '@sharedhouse/contracts';

@Controller('v1/health')
export class HealthController {
  @Get()
  getHealth(): ServiceHealth {
    return {
      status: 'ok',
      service: 'api',
      apiVersion: API_VERSION,
      checkedAt: new Date().toISOString(),
    };
  }
}
