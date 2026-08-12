import { randomBytes } from 'node:crypto';

import type { SqlExecutor, WorkerDatabase } from './database.js';
import { nextOccurrenceDate, type ExpenseCadence } from './recurrence.js';

type TaskCadence = Extract<ExpenseCadence, 'weekly' | 'fortnightly' | 'monthly'>;

interface RecurringTaskRow {
  readonly id: string;
  readonly household_id: string;
  readonly created_by_user_id: string;
  readonly assignee_membership_id: string;
  readonly title: string;
  readonly instructions: string | null;
  readonly zone: string | null;
  readonly priority: 'low' | 'normal' | 'high';
  readonly due_date: Date | string;
  readonly due_time: string | null;
  readonly estimated_minutes: number | null;
  readonly recurrence_cadence: TaskCadence;
  readonly recurrence_ends_on: Date | string | null;
  readonly recurrence_anchor_day: number;
  readonly series_id: string;
}

export interface TaskOccurrenceRunSummary {
  readonly processed: number;
  readonly generated: number;
  readonly alreadyPresent: number;
}

export async function generateUpcomingTaskOccurrences(
  database: WorkerDatabase,
  checkedAt: Date,
  batchSize: number,
  horizonDays = 90,
): Promise<TaskOccurrenceRunSummary> {
  if (!Number.isSafeInteger(batchSize) || batchSize < 1 || batchSize > 500) {
    throw new Error('batchSize must be between 1 and 500.');
  }
  if (!Number.isSafeInteger(horizonDays) || horizonDays < 1 || horizonDays > 366) {
    throw new Error('horizonDays must be between 1 and 366.');
  }
  const horizon = new Date(checkedAt.getTime() + horizonDays * 86_400_000)
    .toISOString()
    .slice(0, 10);
  let generated = 0;
  let alreadyPresent = 0;
  let processed = 0;
  for (let index = 0; index < batchSize; index += 1) {
    const result = await database.transaction((transaction) =>
      generateOne(transaction, checkedAt, horizon),
    );
    if (result === 'none_due') break;
    if (result === 'series_complete') continue;
    processed += 1;
    if (result === 'generated') generated += 1;
    else alreadyPresent += 1;
  }
  return { processed, generated, alreadyPresent };
}

async function generateOne(
  transaction: SqlExecutor,
  checkedAt: Date,
  horizon: string,
): Promise<'generated' | 'already_present' | 'series_complete' | 'none_due'> {
  const task = (
    await transaction.query<RecurringTaskRow>(
      `SELECT t.id, t.household_id, t.created_by_user_id, t.assignee_membership_id,
         t.title, t.instructions, t.zone, t.priority, t.due_date, t.due_time,
         t.estimated_minutes, t.recurrence_cadence, t.recurrence_ends_on,
         t.recurrence_anchor_day, t.series_id
       FROM household_tasks t
       JOIN households h ON h.id = t.household_id AND h.status = 'active'
       JOIN household_memberships assignee
         ON assignee.id = t.assignee_membership_id AND assignee.status = 'active'
       WHERE t.recurrence_cadence IS NOT NULL
         AND t.recurrence_completed = false
         AND NOT EXISTS (
           SELECT 1 FROM household_tasks newer
           WHERE newer.series_id = t.series_id
             AND newer.occurrence_date > t.occurrence_date
         )
         AND (
           (t.recurrence_cadence = 'weekly' AND t.due_date <= $1::date - 7)
           OR (t.recurrence_cadence = 'fortnightly' AND t.due_date <= $1::date - 14)
           OR (t.recurrence_cadence = 'monthly' AND t.due_date <= $1::date - 31)
         )
         AND (t.recurrence_ends_on IS NULL OR t.due_date < t.recurrence_ends_on)
       ORDER BY t.due_date, t.series_id
       FOR UPDATE OF t SKIP LOCKED
       LIMIT 1`,
      [horizon],
    )
  )[0];
  if (task === undefined) return 'none_due';

  const currentDate = toDate(task.due_date);
  const nextDate = nextOccurrenceDate(
    currentDate,
    task.recurrence_cadence,
    task.recurrence_anchor_day,
    1,
  );
  const endsOn = task.recurrence_ends_on === null ? null : toDate(task.recurrence_ends_on);
  if (endsOn !== null && nextDate > endsOn) {
    await transaction.query(
      `UPDATE household_tasks SET recurrence_completed = true, updated_at = $2 WHERE id = $1`,
      [task.id, checkedAt.toISOString()],
    );
    return 'series_complete';
  }
  if (nextDate > horizon) return 'none_due';

  const occurredAt = checkedAt.toISOString();
  const taskId = newUuidV7(checkedAt.getTime());
  const inserted = await transaction.query<{ readonly id: string }>(
    `INSERT INTO household_tasks (
       id, household_id, created_by_user_id, assignee_membership_id, title, instructions,
       zone, priority, due_date, due_time, estimated_minutes, recurrence_cadence,
       recurrence_ends_on, recurrence_anchor_day, series_id, occurrence_date,
       status, version, created_at, updated_at
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$9,
       'open',1,$16,$16)
     ON CONFLICT (series_id, occurrence_date) WHERE series_id IS NOT NULL DO NOTHING
     RETURNING id`,
    [
      taskId,
      task.household_id,
      task.created_by_user_id,
      task.assignee_membership_id,
      task.title,
      task.instructions,
      task.zone,
      task.priority,
      nextDate,
      task.due_time,
      task.estimated_minutes,
      task.recurrence_cadence,
      endsOn,
      task.recurrence_anchor_day,
      task.series_id,
      occurredAt,
    ],
  );
  if (inserted.length === 0) return 'already_present';

  await transaction.query(
    `INSERT INTO household_task_history (
       id, task_id, actor_user_id, event_type, from_status, to_status, details, occurred_at
     ) VALUES ($1,$2,NULL,'generated',NULL,'open',$3::jsonb,$4)`,
    [
      newUuidV7(checkedAt.getTime()),
      taskId,
      JSON.stringify({ seriesId: task.series_id, occurrenceDate: nextDate }),
      occurredAt,
    ],
  );
  await transaction.query(
    `INSERT INTO audit_events (
       id, actor_user_id, household_id, action, target_type, target_id, outcome, safe_details, occurred_at
     ) VALUES ($1,NULL,$2,'tasks.recurring_occurrence_generated','household_task',$3,
       'success',$4::jsonb,$5)`,
    [
      newUuidV7(checkedAt.getTime()),
      task.household_id,
      taskId,
      JSON.stringify({ seriesId: task.series_id, occurrenceDate: nextDate }),
      occurredAt,
    ],
  );
  await transaction.query(
    `INSERT INTO outbox_events (
       id,event_type,aggregate_type,aggregate_id,household_id,actor_user_id,payload,occurred_at
     ) VALUES ($1,'tasks.recurring_occurrence_generated.v1','household_task',$2,$3,NULL,$4::jsonb,$5)`,
    [
      newUuidV7(checkedAt.getTime()),
      taskId,
      task.household_id,
      JSON.stringify({ taskId, seriesId: task.series_id, occurrenceDate: nextDate }),
      occurredAt,
    ],
  );
  return 'generated';
}

function toDate(value: Date | string): string {
  return value instanceof Date ? value.toISOString().slice(0, 10) : value.slice(0, 10);
}

function newUuidV7(timestamp: number): string {
  const bytes = randomBytes(16);
  let remaining = BigInt(timestamp);
  for (let index = 5; index >= 0; index -= 1) {
    bytes[index] = Number(remaining & 0xffn);
    remaining >>= 8n;
  }
  bytes[6] = ((bytes[6] ?? 0) & 0x0f) | 0x70;
  bytes[8] = ((bytes[8] ?? 0) & 0x3f) | 0x80;
  const hex = bytes.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
