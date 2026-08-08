CREATE TABLE expenses (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    created_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    title varchar(120) NOT NULL CHECK (char_length(trim(title)) > 0),
    category varchar(32) NOT NULL CHECK (category IN (
        'rent', 'electricity', 'gas', 'water', 'internet', 'council_tax',
        'groceries', 'household_supplies', 'maintenance', 'other'
    )),
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    currency char(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    due_date date NOT NULL,
    notes varchar(1000),
    split_method varchar(16) NOT NULL CHECK (split_method = 'equal'),
    status varchar(16) NOT NULL CHECK (status IN ('proposed', 'approved', 'reversed')),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX expenses_household_due_idx
    ON expenses (household_id, status, due_date, created_at DESC);

CREATE TABLE expense_allocations (
    id uuid PRIMARY KEY,
    expense_id uuid NOT NULL REFERENCES expenses(id) ON DELETE RESTRICT,
    membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    amount_minor bigint NOT NULL CHECK (amount_minor >= 0),
    rounding_adjustment_minor smallint NOT NULL CHECK (rounding_adjustment_minor IN (0, 1)),
    status varchar(16) NOT NULL CHECK (status = 'outstanding'),
    created_at timestamptz NOT NULL,
    UNIQUE (expense_id, membership_id)
);

CREATE INDEX expense_allocations_membership_idx
    ON expense_allocations (membership_id, status, expense_id);

CREATE TABLE expense_status_events (
    id uuid PRIMARY KEY,
    expense_id uuid NOT NULL REFERENCES expenses(id) ON DELETE RESTRICT,
    actor_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    previous_status varchar(16) CHECK (previous_status IN ('proposed', 'approved', 'reversed')),
    next_status varchar(16) NOT NULL CHECK (next_status IN ('proposed', 'approved', 'reversed')),
    reason varchar(500),
    occurred_at timestamptz NOT NULL
);

CREATE INDEX expense_status_events_expense_time_idx
    ON expense_status_events (expense_id, occurred_at, id);

CREATE FUNCTION enforce_expense_allocation_total() RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    target_expense_id uuid;
    expected_total bigint;
    allocated_total bigint;
BEGIN
    target_expense_id := COALESCE(NEW.expense_id, OLD.expense_id);
    SELECT amount_minor INTO expected_total FROM expenses WHERE id = target_expense_id;
    IF expected_total IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT COALESCE(SUM(amount_minor), 0) INTO allocated_total
    FROM expense_allocations WHERE expense_id = target_expense_id;
    IF allocated_total <> expected_total THEN
        RAISE EXCEPTION 'Expense allocations must reconcile exactly to the expense total.';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER expense_allocations_reconcile
AFTER INSERT OR UPDATE OR DELETE ON expense_allocations
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_expense_allocation_total();
