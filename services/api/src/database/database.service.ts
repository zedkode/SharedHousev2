import { Injectable, type OnModuleDestroy, type OnModuleInit } from '@nestjs/common';
import { PGlite, type Transaction as PGliteTransaction } from '@electric-sql/pglite';
import { createHash } from 'node:crypto';
import { readdir, readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Pool, type PoolClient } from 'pg';

import { readApiEnvironment } from '../config/environment.js';

export interface SqlExecutor {
  query<T extends object>(sql: string, parameters?: readonly unknown[]): Promise<readonly T[]>;
  exec(sql: string): Promise<void>;
}

@Injectable()
export class DatabaseService implements OnModuleInit, OnModuleDestroy, SqlExecutor {
  private database: PGlite | Pool | null = null;
  private executor: SqlExecutor | null = null;

  async onModuleInit(): Promise<void> {
    const environment = readApiEnvironment(process.env);

    if (environment.databaseUrl === null) {
      const database = await PGlite.create(environment.pgliteDataDirectory);
      this.database = database;
      this.executor = new PGliteExecutor(database);
    } else {
      const database = new Pool({
        connectionString: environment.databaseUrl,
        ...(environment.databasePassword === null
          ? {}
          : { password: environment.databasePassword }),
      });
      await database.query('SELECT 1');
      this.database = database;
      this.executor = new PgExecutor(database);
    }

    await runMigrations(this);
  }

  async onModuleDestroy(): Promise<void> {
    if (this.database instanceof PGlite) {
      await this.database.close();
    } else if (this.database instanceof Pool) {
      await this.database.end();
    }

    this.database = null;
    this.executor = null;
  }

  async query<T extends object>(
    sql: string,
    parameters: readonly unknown[] = [],
  ): Promise<readonly T[]> {
    return this.requireExecutor().query<T>(sql, parameters);
  }

  async exec(sql: string): Promise<void> {
    await this.requireExecutor().exec(sql);
  }

  async transaction<T>(work: (executor: SqlExecutor) => Promise<T>): Promise<T> {
    if (this.database instanceof PGlite) {
      return this.database.transaction(async (transaction) =>
        work(new PGliteExecutor(transaction)),
      );
    }

    if (this.database instanceof Pool) {
      const client = await this.database.connect();
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

    throw new Error('Database is not initialised.');
  }

  private requireExecutor(): SqlExecutor {
    if (this.executor === null) {
      throw new Error('Database is not initialised.');
    }

    return this.executor;
  }
}

class PGliteExecutor implements SqlExecutor {
  constructor(private readonly database: PGlite | PGliteTransaction) {}

  async query<T extends object>(
    sql: string,
    parameters: readonly unknown[] = [],
  ): Promise<readonly T[]> {
    const result = await this.database.query<T>(sql, [...parameters]);
    return result.rows;
  }

  async exec(sql: string): Promise<void> {
    await this.database.exec(sql);
  }
}

class PgExecutor implements SqlExecutor {
  constructor(private readonly database: Pool | PoolClient) {}

  async query<T extends object>(
    sql: string,
    parameters: readonly unknown[] = [],
  ): Promise<readonly T[]> {
    const result = await this.database.query(sql, [...parameters]);
    return result.rows as readonly T[];
  }

  async exec(sql: string): Promise<void> {
    await this.database.query(sql);
  }
}

interface MigrationRow {
  readonly name: string;
  readonly checksum: string;
}

async function runMigrations(database: DatabaseService): Promise<void> {
  await database.exec(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      name text PRIMARY KEY,
      checksum char(64) NOT NULL,
      applied_at timestamptz NOT NULL
    );
  `);

  const migrationDirectory = fileURLToPath(new URL('../../migrations/', import.meta.url));
  const migrationNames = (await readdir(migrationDirectory))
    .filter((name) => name.endsWith('.sql'))
    .sort((left, right) => left.localeCompare(right));
  const applied = await database.query<MigrationRow>(
    'SELECT name, checksum FROM schema_migrations ORDER BY name',
  );
  const appliedByName = new Map(applied.map((migration) => [migration.name, migration.checksum]));

  for (const name of migrationNames) {
    const sql = await readFile(resolve(migrationDirectory, name), 'utf8');
    const checksum = createHash('sha256').update(sql).digest('hex');
    const previousChecksum = appliedByName.get(name);

    if (previousChecksum !== undefined && previousChecksum !== checksum) {
      throw new Error(`Migration checksum mismatch: ${name}`);
    }

    if (previousChecksum !== undefined) {
      continue;
    }

    await database.transaction(async (transaction) => {
      await transaction.exec(sql);
      await transaction.query(
        'INSERT INTO schema_migrations (name, checksum, applied_at) VALUES ($1, $2, $3)',
        [name, checksum, new Date().toISOString()],
      );
    });
  }
}
