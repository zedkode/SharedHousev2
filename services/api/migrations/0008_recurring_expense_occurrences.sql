ALTER TABLE expense_templates
    ADD COLUMN schedule_anchor_day smallint,
    ADD COLUMN schedule_anchor_month smallint;

UPDATE expense_templates
SET schedule_anchor_day = EXTRACT(DAY FROM next_due_date),
    schedule_anchor_month = EXTRACT(MONTH FROM next_due_date);

ALTER TABLE expense_templates
    ALTER COLUMN schedule_anchor_day SET NOT NULL,
    ALTER COLUMN schedule_anchor_month SET NOT NULL,
    ADD CONSTRAINT expense_templates_anchor_day_check
        CHECK (schedule_anchor_day BETWEEN 1 AND 31),
    ADD CONSTRAINT expense_templates_anchor_month_check
        CHECK (schedule_anchor_month BETWEEN 1 AND 12);

ALTER TABLE expenses
    ADD COLUMN source_template_id uuid REFERENCES expense_templates(id) ON DELETE RESTRICT,
    ADD COLUMN occurrence_date date,
    ADD CONSTRAINT expenses_template_occurrence_pair_check CHECK (
        (source_template_id IS NULL AND occurrence_date IS NULL)
        OR (source_template_id IS NOT NULL AND occurrence_date IS NOT NULL)
    );

CREATE UNIQUE INDEX expenses_template_occurrence_unique
    ON expenses (source_template_id, occurrence_date)
    WHERE source_template_id IS NOT NULL;

CREATE INDEX expenses_source_template_idx
    ON expenses (source_template_id, occurrence_date DESC)
    WHERE source_template_id IS NOT NULL;

-- Automated occurrences are system actions. Keeping the actor nullable avoids
-- attributing a background decision to the member who originally made the template.
ALTER TABLE expense_status_events
    ALTER COLUMN actor_membership_id DROP NOT NULL;
