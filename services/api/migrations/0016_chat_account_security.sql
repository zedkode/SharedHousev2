-- Extended household chat and account-security state. This migration is additive except for
-- widening the existing chat body from varchar(2000) to PostgreSQL text.
ALTER TABLE household_chat_messages
    DROP CONSTRAINT IF EXISTS household_chat_messages_body_check;

ALTER TABLE household_chat_messages
    ALTER COLUMN body TYPE text;

ALTER TABLE household_chat_messages
    ADD COLUMN message_kind varchar(16) NOT NULL DEFAULT 'member'
        CHECK (message_kind IN ('member', 'system')),
    ADD COLUMN latitude double precision,
    ADD COLUMN longitude double precision,
    ADD COLUMN source_chat_message_id uuid REFERENCES household_chat_messages(id) ON DELETE RESTRICT,
    ADD COLUMN source_calendar_event_id uuid REFERENCES calendar_events(id) ON DELETE RESTRICT,
    ADD CONSTRAINT household_chat_location_check CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    );

ALTER TABLE calendar_events
    ADD COLUMN source_chat_message_id uuid REFERENCES household_chat_messages(id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX calendar_events_source_chat_message_unique
    ON calendar_events (source_chat_message_id)
    WHERE source_chat_message_id IS NOT NULL AND status = 'active';

CREATE TABLE household_chat_attachments (
    id uuid PRIMARY KEY,
    message_id uuid REFERENCES household_chat_messages(id) ON DELETE RESTRICT,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    uploaded_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    media_type varchar(32) NOT NULL CHECK (media_type IN ('image/jpeg', 'image/png', 'image/webp')),
    byte_size integer NOT NULL CHECK (byte_size BETWEEN 1 AND 2621440),
    width integer NOT NULL CHECK (width BETWEEN 1 AND 8192),
    height integer NOT NULL CHECK (height BETWEEN 1 AND 8192),
    content bytea NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX household_chat_attachments_message_idx
    ON household_chat_attachments (message_id, id);

CREATE INDEX household_chat_attachments_unclaimed_idx
    ON household_chat_attachments (created_at)
    WHERE message_id IS NULL;

CREATE TABLE household_chat_mentions (
    message_id uuid NOT NULL REFERENCES household_chat_messages(id) ON DELETE RESTRICT,
    mentioned_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    PRIMARY KEY (message_id, mentioned_user_id)
);

CREATE INDEX household_chat_mentions_user_time_idx
    ON household_chat_mentions (mentioned_user_id, created_at DESC);

CREATE TABLE household_chat_message_pins (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    message_id uuid NOT NULL REFERENCES household_chat_messages(id) ON DELETE RESTRICT,
    pinned_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    pinned_at timestamptz NOT NULL,
    unpinned_by_membership_id uuid REFERENCES household_memberships(id) ON DELETE RESTRICT,
    unpinned_at timestamptz,
    CHECK (
        (unpinned_at IS NULL AND unpinned_by_membership_id IS NULL)
        OR (unpinned_at IS NOT NULL AND unpinned_by_membership_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX household_chat_message_active_pin_unique
    ON household_chat_message_pins (message_id)
    WHERE unpinned_at IS NULL;

CREATE INDEX household_chat_household_active_pins_idx
    ON household_chat_message_pins (household_id, pinned_at DESC)
    WHERE unpinned_at IS NULL;

CREATE TABLE account_email_changes (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    old_email_normalized varchar(254) NOT NULL,
    new_email_normalized varchar(254) NOT NULL,
    code_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 5),
    confirmed_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz NOT NULL,
    CHECK (old_email_normalized <> new_email_normalized)
);

CREATE UNIQUE INDEX account_email_changes_pending_user_unique
    ON account_email_changes (user_id)
    WHERE confirmed_at IS NULL AND cancelled_at IS NULL;

CREATE UNIQUE INDEX account_email_changes_pending_email_unique
    ON account_email_changes (new_email_normalized)
    WHERE confirmed_at IS NULL AND cancelled_at IS NULL;

CREATE TABLE user_avatars (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    media_type varchar(32) NOT NULL CHECK (media_type IN ('image/jpeg', 'image/png', 'image/webp')),
    byte_size integer NOT NULL CHECK (byte_size BETWEEN 1 AND 1048576),
    width integer NOT NULL CHECK (width BETWEEN 1 AND 2048),
    height integer NOT NULL CHECK (height BETWEEN 1 AND 2048),
    content bytea NOT NULL,
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    updated_at timestamptz NOT NULL
);

ALTER TABLE verification_email_outbox
    DROP CONSTRAINT IF EXISTS verification_email_outbox_challenge_id_fkey,
    ADD COLUMN message_kind varchar(32) NOT NULL DEFAULT 'email_verification'
        CHECK (message_kind IN ('email_verification', 'email_change_verification', 'email_change_warning', 'password_changed')),
    DROP CONSTRAINT IF EXISTS verification_email_outbox_payload_state_check,
    ADD CONSTRAINT verification_email_outbox_payload_state_check CHECK (
        status IN ('sent', 'dead')
        OR message_kind IN ('email_change_warning', 'password_changed')
        OR (
            code_ciphertext_base64 IS NOT NULL
            AND code_iv_base64 IS NOT NULL
            AND code_auth_tag_base64 IS NOT NULL
        )
    );
