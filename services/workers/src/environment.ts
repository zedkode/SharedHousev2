import { readFileSync } from 'node:fs';
import { isAbsolute } from 'node:path';

export interface WorkerEnvironment {
  readonly databaseUrl: string;
  readonly databasePassword: string | null;
  readonly pollIntervalMs: number;
  readonly batchSize: number;
}

export function readWorkerEnvironment(environment: NodeJS.ProcessEnv): WorkerEnvironment {
  const databaseUrl = readRequired(environment.DATABASE_URL, 'DATABASE_URL');
  const directPassword = readOptional(environment.DATABASE_PASSWORD);
  const passwordFile = readOptional(environment.DATABASE_PASSWORD_FILE);
  if (directPassword !== null && passwordFile !== null) {
    throw new Error('DATABASE_PASSWORD and DATABASE_PASSWORD_FILE cannot both be configured.');
  }
  if (passwordFile !== null && !isAbsolute(passwordFile)) {
    throw new Error('DATABASE_PASSWORD_FILE must be an absolute path.');
  }
  const databasePassword =
    passwordFile !== null
      ? readRequired(readFileSync(passwordFile, 'utf8'), 'DATABASE_PASSWORD_FILE')
      : directPassword;

  return {
    databaseUrl,
    databasePassword,
    pollIntervalMs: readInteger(environment.WORKER_POLL_INTERVAL_MS, 60_000, 5_000, 86_400_000),
    batchSize: readInteger(environment.WORKER_BATCH_SIZE, 100, 1, 500),
  };
}

function readRequired(value: string | undefined, name: string): string {
  if (value === undefined || value.trim() === '') throw new Error(`${name} is required.`);
  return value.trim();
}

function readInteger(
  value: string | undefined,
  fallback: number,
  minimum: number,
  maximum: number,
): number {
  if (value === undefined || value.trim() === '') return fallback;
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < minimum || parsed > maximum) {
    throw new Error(
      `Worker integer setting must be between ${minimum.toString()} and ${maximum.toString()}.`,
    );
  }
  return parsed;
}

function readOptional(value: string | undefined): string | null {
  if (value === undefined || value.trim() === '') return null;
  return value.trim();
}
