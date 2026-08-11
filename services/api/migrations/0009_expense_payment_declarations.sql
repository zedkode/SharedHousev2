CREATE TABLE expense_payment_declarations (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    expense_id uuid NOT NULL REFERENCES expenses(id) ON DELETE RESTRICT,
    allocation_id uuid NOT NULL REFERENCES expense_allocations(id) ON DELETE RESTRICT,
    declared_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    currency char(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    method varchar(24) NOT NULL CHECK (method IN (
        'bank_transfer', 'cash', 'card', 'direct_debit', 'other'
    )),
    payment_reference varchar(120),
    note varchar(500),
    paid_at timestamptz NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('declared', 'confirmed', 'disputed', 'reversed')),
    confirmed_by_membership_id uuid REFERENCES household_memberships(id) ON DELETE RESTRICT,
    confirmed_at timestamptz,
    disputed_by_membership_id uuid REFERENCES household_memberships(id) ON DELETE RESTRICT,
    disputed_at timestamptz,
    dispute_reason varchar(500),
    reversed_by_membership_id uuid REFERENCES household_memberships(id) ON DELETE RESTRICT,
    reversed_at timestamptz,
    reversal_reason varchar(500),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CHECK ((confirmed_by_membership_id IS NULL) = (confirmed_at IS NULL)),
    CHECK ((disputed_by_membership_id IS NULL) = (disputed_at IS NULL)),
    CHECK ((disputed_at IS NULL) = (dispute_reason IS NULL)),
    CHECK ((reversed_by_membership_id IS NULL) = (reversed_at IS NULL)),
    CHECK ((reversed_at IS NULL) = (reversal_reason IS NULL))
);

CREATE UNIQUE INDEX expense_payment_one_active_per_allocation_idx
    ON expense_payment_declarations (allocation_id)
    WHERE status <> 'reversed';

CREATE INDEX expense_payment_expense_history_idx
    ON expense_payment_declarations (expense_id, created_at, id);

CREATE INDEX expense_payment_household_status_idx
    ON expense_payment_declarations (household_id, status, paid_at DESC);

CREATE TABLE expense_payment_status_events (
    id uuid PRIMARY KEY,
    payment_id uuid NOT NULL REFERENCES expense_payment_declarations(id) ON DELETE RESTRICT,
    actor_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    previous_status varchar(16) CHECK (previous_status IN ('declared', 'confirmed', 'disputed', 'reversed')),
    next_status varchar(16) NOT NULL CHECK (next_status IN ('declared', 'confirmed', 'disputed', 'reversed')),
    reason varchar(500),
    occurred_at timestamptz NOT NULL
);

CREATE INDEX expense_payment_status_events_history_idx
    ON expense_payment_status_events (payment_id, occurred_at, id);
