ALTER TABLE expenses
    ADD COLUMN supplier_name varchar(120),
    ADD COLUMN revision_of_expense_id uuid REFERENCES expenses(id) ON DELETE RESTRICT,
    ADD COLUMN superseded_by_expense_id uuid REFERENCES expenses(id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX expenses_one_successor_idx
    ON expenses (revision_of_expense_id)
    WHERE revision_of_expense_id IS NOT NULL;

CREATE TABLE expense_revision_events (
    id uuid PRIMARY KEY,
    original_expense_id uuid NOT NULL REFERENCES expenses(id) ON DELETE RESTRICT,
    revised_expense_id uuid NOT NULL REFERENCES expenses(id) ON DELETE RESTRICT,
    actor_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    original_snapshot jsonb NOT NULL,
    revised_snapshot jsonb NOT NULL,
    reason varchar(500) NOT NULL CHECK (char_length(trim(reason)) >= 3),
    occurred_at timestamptz NOT NULL,
    UNIQUE (original_expense_id, revised_expense_id)
);

CREATE INDEX expense_revision_events_history_idx
    ON expense_revision_events (original_expense_id, occurred_at, id);
