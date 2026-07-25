import { API_VERSION, type ServiceHealth } from '@sharedhouse/contracts';

export function createWorkerHealth(checkedAt: Date): ServiceHealth {
  return {
    status: 'ok',
    service: 'workers',
    apiVersion: API_VERSION,
    checkedAt: checkedAt.toISOString(),
  };
}
