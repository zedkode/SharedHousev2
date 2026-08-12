import { describe, expect, it } from 'vitest';

import type { SqlExecutor, WorkerDatabase } from './database.js';
import { generateUpcomingTaskOccurrences } from './task-occurrence-generator.js';

class FakeDatabase implements WorkerDatabase, SqlExecutor {
  readonly calls: { readonly sql: string; readonly parameters: readonly unknown[] }[] = [];
  reads = 0;
  endsOn: string | null = null;

  async transaction<T>(work: (executor: SqlExecutor) => Promise<T>): Promise<T> {
    return work(this);
  }

  query<T extends object>(sql: string, parameters: readonly unknown[] = []): Promise<readonly T[]> {
    this.calls.push({ sql, parameters });
    if (sql.includes('FROM household_tasks t')) {
      this.reads += 1;
      if (this.reads > 1) return Promise.resolve([]);
      return Promise.resolve([
        {
          id: '10000000-0000-7000-8000-000000000001',
          household_id: '10000000-0000-7000-8000-000000000002',
          created_by_user_id: '10000000-0000-7000-8000-000000000003',
          assignee_membership_id: '10000000-0000-7000-8000-000000000004',
          title: 'Clean the kitchen',
          instructions: 'Mop the floor.',
          zone: 'Kitchen',
          priority: 'normal',
          due_date: '2026-08-15',
          due_time: '18:30',
          estimated_minutes: 30,
          recurrence_cadence: 'fortnightly',
          recurrence_ends_on: this.endsOn,
          recurrence_anchor_day: 15,
          series_id: '10000000-0000-7000-8000-000000000005',
        },
      ] as unknown as readonly T[]);
    }
    if (sql.includes('INSERT INTO household_tasks')) {
      return Promise.resolve([{ id: 'generated' }] as unknown as readonly T[]);
    }
    return Promise.resolve([]);
  }

  close(): Promise<void> {
    return Promise.resolve();
  }
}

describe('recurring task generation', () => {
  it('creates the next fixed-assignee task occurrence inside the rolling horizon', async () => {
    const database = new FakeDatabase();

    const result = await generateUpcomingTaskOccurrences(
      database,
      new Date('2026-08-11T12:00:00.000Z'),
      10,
      30,
    );

    expect(result).toEqual({ processed: 1, generated: 1, alreadyPresent: 0 });
    const insert = database.calls.find((call) => call.sql.includes('INSERT INTO household_tasks'));
    expect(insert?.parameters).toEqual(expect.arrayContaining(['2026-08-29', 'fortnightly']));
    expect(
      database.calls.some((call) => call.sql.includes("'tasks.recurring_occurrence_generated.v1'")),
    ).toBe(true);
  });

  it('marks a finite series complete instead of revisiting it after its last occurrence', async () => {
    const database = new FakeDatabase();
    database.endsOn = '2026-08-15';

    const result = await generateUpcomingTaskOccurrences(
      database,
      new Date('2026-08-11T12:00:00.000Z'),
      10,
      30,
    );

    expect(result).toEqual({ processed: 0, generated: 0, alreadyPresent: 0 });
    expect(
      database.calls.some(
        (call) =>
          call.sql.includes('recurrence_completed = true') &&
          call.parameters[0] === '10000000-0000-7000-8000-000000000001',
      ),
    ).toBe(true);
    expect(database.calls.some((call) => call.sql.includes('INSERT INTO household_tasks'))).toBe(
      false,
    );
  });
});
