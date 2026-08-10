ALTER TABLE expenses
    DROP CONSTRAINT expenses_category_check,
    ADD COLUMN custom_category_name varchar(60),
    ADD CONSTRAINT expenses_category_check CHECK (category IN (
        'rent', 'electricity', 'gas', 'water', 'internet', 'council_tax',
        'groceries', 'household_supplies', 'maintenance', 'other', 'custom'
    )),
    ADD CONSTRAINT expenses_custom_category_check CHECK (
        (category = 'custom' AND custom_category_name IS NOT NULL
            AND char_length(trim(custom_category_name)) BETWEEN 1 AND 60)
        OR (category <> 'custom' AND custom_category_name IS NULL)
    );

CREATE TABLE expense_templates (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    created_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    title varchar(120) NOT NULL CHECK (char_length(trim(title)) > 0),
    category varchar(32) NOT NULL CHECK (category IN (
        'rent', 'electricity', 'gas', 'water', 'internet', 'council_tax',
        'groceries', 'household_supplies', 'maintenance', 'other', 'custom'
    )),
    custom_category_name varchar(60),
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    currency char(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    cadence varchar(16) NOT NULL CHECK (cadence IN ('weekly', 'monthly', 'quarterly', 'yearly')),
    next_due_date date NOT NULL,
    notes varchar(1000),
    status varchar(16) NOT NULL CHECK (status IN ('active', 'archived')),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT expense_templates_custom_category_check CHECK (
        (category = 'custom' AND custom_category_name IS NOT NULL
            AND char_length(trim(custom_category_name)) BETWEEN 1 AND 60)
        OR (category <> 'custom' AND custom_category_name IS NULL)
    )
);

CREATE INDEX expense_templates_household_status_due_idx
    ON expense_templates (household_id, status, next_due_date, created_at);

CREATE TABLE expense_template_status_events (
    id uuid PRIMARY KEY,
    template_id uuid NOT NULL REFERENCES expense_templates(id) ON DELETE RESTRICT,
    actor_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    previous_status varchar(16) CHECK (previous_status IN ('active', 'archived')),
    next_status varchar(16) NOT NULL CHECK (next_status IN ('active', 'archived')),
    reason varchar(500),
    occurred_at timestamptz NOT NULL
);

CREATE INDEX expense_template_status_events_template_time_idx
    ON expense_template_status_events (template_id, occurred_at, id);
