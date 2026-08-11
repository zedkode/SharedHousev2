import { Pool, type PoolClient } from 'pg';

import type { WorkerEnvironment } from './environment.js';

export interface SqlExecutor {
  query<T extends object>(sql: string, parameters?: readonly unknown[]): Promise<readonly T[]>;
}

export interface WorkerDatabase {
  transaction<T>(work: (executor: SqlExecutor) => Promise<T>): Promise<T>;
  close(): Promise<void>;
}

export class PostgresWorkerDatabase implements WorkerDatabase {
  private readonly pool: Pool;

  constructor(environment: WorkerEnvironment) {
    this.pool = new Pool({
      connectionString: buildConnectionString(
        environment.databaseUrl,
        environment.databasePassword,
      ),
      max: 4,
      application_name: 'sharedhouse-workers',
    });
  }

  async transaction<T>(work: (executor: SqlExecutor) => Promise<T>): Promise<T> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      const result = await work(new PgExecutor(client));
      await client.query('COMMIT');
      return result;
    } catch (error: unknown) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  async close(): Promise<void> {
    await this.pool.end();
  }
}

export function buildConnectionString(databaseUrl: string, password: string | null): string {
  if (password === null) return databaseUrl;
  const parsed = new URL(databaseUrl);
  if (parsed.protocol !== 'postgres:' && parsed.protocol !== 'postgresql:') {
    throw new Error('DATABASE_URL must use the postgres or postgresql protocol.');
  }
  parsed.password = password;
  return parsed.toString();
}

class PgExecutor implements SqlExecutor {
  constructor(private readonly client: PoolClient) {}

  async query<T extends object>(
    sql: string,
    parameters: readonly unknown[] = [],
  ): Promise<readonly T[]> {
    const result = await this.client.query(sql, [...parameters]);
    return result.rows as readonly T[];
  }
}
