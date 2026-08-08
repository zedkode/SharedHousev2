CREATE TABLE household_invitations (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    token_hash char(64) NOT NULL UNIQUE,
    email_normalized varchar(254),
    role varchar(24) NOT NULL CHECK (role IN ('admin', 'member', 'read_only')),
    status varchar(16) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'revoked', 'expired')),
    invited_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    accepted_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    expires_at timestamptz NOT NULL,
    accepted_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT household_invitations_terminal_state_check CHECK (
        (
            status = 'pending'
            AND accepted_by IS NULL AND accepted_at IS NULL AND revoked_at IS NULL
        ) OR (
            status = 'accepted'
            AND accepted_by IS NOT NULL AND accepted_at IS NOT NULL AND revoked_at IS NULL
        ) OR (
            status = 'revoked'
            AND accepted_by IS NULL AND accepted_at IS NULL AND revoked_at IS NOT NULL
        ) OR (
            status = 'expired'
            AND accepted_by IS NULL AND accepted_at IS NULL AND revoked_at IS NULL
        )
    )
);

CREATE INDEX household_invitations_household_idx
    ON household_invitations (household_id, created_at DESC);

CREATE INDEX household_invitations_pending_idx
    ON household_invitations (expires_at)
    WHERE status = 'pending';
