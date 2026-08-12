ALTER TABLE household_tasks
    ADD COLUMN recurrence_cadence varchar(16),
    ADD COLUMN recurrence_ends_on date,
    ADD COLUMN recurrence_anchor_day smallint,
    ADD COLUMN series_id uuid,
    ADD COLUMN occurrence_date date,
    ADD COLUMN recurrence_completed boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT household_tasks_recurrence_cadence_check CHECK (
        recurrence_cadence IS NULL OR recurrence_cadence IN ('weekly', 'fortnightly', 'monthly')
    ),
    ADD CONSTRAINT household_tasks_recurrence_anchor_check CHECK (
        recurrence_anchor_day IS NULL OR recurrence_anchor_day BETWEEN 1 AND 31
    ),
    ADD CONSTRAINT household_tasks_recurrence_fields_check CHECK (
        (recurrence_cadence IS NULL AND recurrence_ends_on IS NULL AND recurrence_anchor_day IS NULL
            AND series_id IS NULL AND occurrence_date IS NULL)
        OR (recurrence_cadence IS NOT NULL AND recurrence_anchor_day IS NOT NULL
            AND series_id IS NOT NULL AND occurrence_date IS NOT NULL
            AND (recurrence_ends_on IS NULL OR recurrence_ends_on >= occurrence_date))
    ),
    ADD CONSTRAINT household_tasks_recurrence_completed_check CHECK (
        recurrence_cadence IS NOT NULL OR recurrence_completed = false
    );

CREATE UNIQUE INDEX household_tasks_series_occurrence_unique
    ON household_tasks (series_id, occurrence_date)
    WHERE series_id IS NOT NULL;

CREATE INDEX household_tasks_recurring_generation_idx
    ON household_tasks (due_date, series_id)
    WHERE recurrence_cadence IS NOT NULL AND recurrence_completed = false;

ALTER TABLE household_task_history
    ALTER COLUMN actor_user_id DROP NOT NULL;
