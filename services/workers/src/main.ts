import { createWorkerHealth } from './health.js';

const health = createWorkerHealth(new Date());

console.log(JSON.stringify({ event: 'worker.started', ...health }));
