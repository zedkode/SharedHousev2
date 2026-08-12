import { createWorkerHealth } from './health.js';
import { PostgresWorkerDatabase } from './database.js';
import { readWorkerEnvironment } from './environment.js';
import { generateDueExpenseOccurrences } from './occurrence-generator.js';
import { generateUpcomingTaskOccurrences } from './task-occurrence-generator.js';

async function main(): Promise<void> {
  const environment = readWorkerEnvironment(process.env);
  const database = new PostgresWorkerDatabase(environment);
  const stopController = new AbortController();
  const requestStop = (): void => {
    stopController.abort();
  };
  process.once('SIGINT', requestStop);
  process.once('SIGTERM', requestStop);

  console.log(JSON.stringify({ event: 'worker.started', ...createWorkerHealth(new Date()) }));
  try {
    while (!stopController.signal.aborted) {
      const startedAt = new Date();
      try {
        const summary = await generateDueExpenseOccurrences(
          database,
          startedAt,
          environment.batchSize,
        );
        const taskSummary = await generateUpcomingTaskOccurrences(
          database,
          startedAt,
          environment.batchSize,
        );
        console.log(
          JSON.stringify({
            event: 'worker.recurring_expenses.completed',
            checkedAt: startedAt.toISOString(),
            ...summary,
            recurringTasks: taskSummary,
          }),
        );
      } catch (error: unknown) {
        console.error(
          JSON.stringify({
            event: 'worker.recurring_expenses.failed',
            checkedAt: startedAt.toISOString(),
            message: error instanceof Error ? error.message : 'Unknown worker failure',
          }),
        );
      }
      await delay(environment.pollIntervalMs, stopController.signal);
    }
  } finally {
    await database.close();
    console.log(JSON.stringify({ event: 'worker.stopped', checkedAt: new Date().toISOString() }));
  }
}

function delay(durationMs: number, signal: AbortSignal): Promise<void> {
  if (signal.aborted) return Promise.resolve();
  return new Promise((resolve) => {
    const finish = (): void => {
      signal.removeEventListener('abort', onAbort);
      resolve();
    };
    const timeout = setTimeout(finish, durationMs);
    const onAbort = (): void => {
      clearTimeout(timeout);
      finish();
    };
    signal.addEventListener('abort', onAbort, { once: true });
  });
}

void main();
