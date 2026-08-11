ALTER TABLE household_memberships
    ADD COLUMN version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    ADD COLUMN updated_at timestamptz;

UPDATE household_memberships
SET updated_at = COALESCE(left_at, joined_at)
WHERE updated_at IS NULL;

ALTER TABLE household_memberships
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT now();

CREATE UNIQUE INDEX household_memberships_active_owner_unique
    ON household_memberships (household_id)
    WHERE role = 'owner' AND status = 'active';

CREATE TABLE household_membership_history (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action varchar(32) NOT NULL CHECK (action IN (
        'role_changed', 'suspended', 'reactivated', 'removed',
        'ownership_transferred_from', 'ownership_transferred_to'
    )),
    previous_role varchar(24) NOT NULL CHECK (previous_role IN ('owner', 'admin', 'member', 'read_only')),
    new_role varchar(24) NOT NULL CHECK (new_role IN ('owner', 'admin', 'member', 'read_only')),
    previous_status varchar(16) NOT NULL CHECK (previous_status IN ('active', 'suspended', 'left', 'removed')),
    new_status varchar(16) NOT NULL CHECK (new_status IN ('active', 'suspended', 'left', 'removed')),
    reason varchar(240),
    occurred_at timestamptz NOT NULL
);

CREATE INDEX household_membership_history_household_time_idx
    ON household_membership_history (household_id, occurred_at DESC);

