import { describe, expect, it } from 'vitest';

import type { SqlExecutor, WorkerDatabase } from './database.js';
import { generateDueExpenseOccurrences } from './occurrence-generator.js';

class FakeDatabase implements WorkerDatabase, SqlExecutor {
  readonly calls: { readonly sql: string; readonly parameters: readonly unknown[] }[] = [];
  dueReads = 0;
  insertSucceeds = true;

  async transaction<T>(work: (executor: SqlExecutor) => Promise<T>): Promise<T> {
    return work(this);
  }

  query<T extends object>(sql: string, parameters: readonly unknown[] = []): Promise<readonly T[]> {
    this.calls.push({ sql, parameters });
    if (sql.includes('FROM expense_templates t')) {
      this.dueReads += 1;
      if (this.dueReads > 1) return Promise.resolve([]);
      return Promise.resolve([
        {
          id: '10000000-0000-7000-8000-000000000001',
          household_id: '10000000-0000-7000-8000-000000000002',
          created_by_membership_id: '10000000-0000-7000-8000-000000000003',
          title: 'Rent',
          category: 'rent',
          custom_category_name: null,
          amount_minor: '1001',
          currency: 'GBP',
          cadence: 'monthly',
          next_due_date: '2027-01-31',
          schedule_anchor_day: 31,
          schedule_anchor_month: 1,
          notes: null,
        },
      ] as unknown as readonly T[]);
    }
    if (sql.includes('SELECT id FROM household_memberships')) {
      return Promise.resolve([
        { id: '20000000-0000-7000-8000-000000000001' },
        { id: '20000000-0000-7000-8000-000000000002' },
        { id: '20000000-0000-7000-8000-000000000003' },
      ] as unknown as readonly T[]);
    }
    if (sql.includes('INSERT INTO expenses')) {
      return Promise.resolve(
        (this.insertSucceeds ? [{ id: 'generated' }] : []) as unknown as readonly T[],
      );
    }
    return Promise.resolve([]);
  }

  close(): Promise<void> {
    return Promise.resolve();
  }
}

describe('recurring expense generation', () => {
  it('creates one approved exact-split occurrence and advances the series', async () => {
    const database = new FakeDatabase();
    const result = await generateDueExpenseOccurrences(
      database,
      new Date('2027-01-31T12:00:00.000Z'),
      10,
    );

    expect(result).toEqual({ processed: 1, generated: 1, alreadyPresent: 0 });
    const allocations = database.calls.filter((call) =>
      call.sql.includes('INSERT INTO expense_allocations'),
    );
    expect(allocations.map((call) => call.parameters[3])).toEqual(['334', '334', '333']);
    const advance = database.calls.find((call) => call.sql.includes('UPDATE expense_templates'));
    expect(advance?.parameters[1]).toBe('2027-02-28');
    expect(
      database.calls.some((call) => call.sql.includes('actor_membership_id, previous_status')),
    ).toBe(true);
  });

  it('treats an existing occurrence as an idempotent replay and still repairs the cursor', async () => {
    const database = new FakeDatabase();
    database.insertSucceeds = false;
    const result = await generateDueExpenseOccurrences(
      database,
      new Date('2027-01-31T12:00:00.000Z'),
      10,
    );

    expect(result).toEqual({ processed: 1, generated: 0, alreadyPresent: 1 });
    expect(
      database.calls.some((call) => call.sql.includes('INSERT INTO expense_allocations')),
    ).toBe(false);
    expect(database.calls.some((call) => call.sql.includes('UPDATE expense_templates'))).toBe(true);
  });
});
