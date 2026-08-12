ALTER TABLE expense_templates
    DROP CONSTRAINT expense_templates_cadence_check,
    ADD COLUMN schedule_ends_on date,
    ADD CONSTRAINT expense_templates_cadence_check CHECK (
        cadence IN ('weekly', 'fortnightly', 'monthly', 'quarterly', 'yearly')
    ),
    ADD CONSTRAINT expense_templates_schedule_window_check CHECK (
        status = 'archived' OR schedule_ends_on IS NULL OR schedule_ends_on >= next_due_date
    );

ALTER TABLE expense_template_status_events
    ALTER COLUMN actor_membership_id DROP NOT NULL;

CREATE INDEX expense_templates_active_schedule_window_idx
    ON expense_templates (next_due_date, schedule_ends_on, id)
    WHERE status = 'active';
