CREATE TABLE privacy_requests (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    request_type varchar(16) NOT NULL CHECK (request_type IN ('export', 'deletion')),
    status varchar(16) NOT NULL CHECK (status IN ('completed', 'blocked')),
    result_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    requested_at timestamptz NOT NULL,
    completed_at timestamptz
);

CREATE INDEX privacy_requests_user_time_idx
    ON privacy_requests (user_id, requested_at DESC);

CREATE TABLE household_membership_role_changes (
    id uuid PRIMARY KEY,
    membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    previous_role varchar(24) NOT NULL CHECK (previous_role IN ('owner', 'admin', 'member', 'read_only')),
    next_role varchar(24) NOT NULL CHECK (next_role IN ('owner', 'admin', 'member', 'read_only')),
    changed_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    occurred_at timestamptz NOT NULL
);

CREATE INDEX household_membership_role_changes_household_time_idx
    ON household_membership_role_changes (household_id, occurred_at DESC);
