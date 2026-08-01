CREATE TABLE users (
    id uuid PRIMARY KEY,
    email_normalized varchar(254) NOT NULL UNIQUE,
    email_verified_at timestamptz,
    status varchar(24) NOT NULL CHECK (status IN ('pending_verification', 'active', 'suspended', 'deleted')),
    preferred_locale varchar(5) NOT NULL CHECK (preferred_locale IN ('en', 'ro')),
    adult_confirmed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE user_profiles (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name varchar(80) NOT NULL CHECK (char_length(trim(display_name)) > 0),
    appearance_preference varchar(16) NOT NULL DEFAULT 'system'
        CHECK (appearance_preference IN ('system', 'light', 'dark'))
);

CREATE TABLE password_credentials (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    algorithm varchar(24) NOT NULL CHECK (algorithm IN ('argon2id-v1', 'scrypt-v1')),
    salt_base64 varchar(128) NOT NULL,
    hash_base64 varchar(256) NOT NULL,
    changed_at timestamptz NOT NULL
);

CREATE TABLE email_verification_challenges (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 5),
    consumed_at timestamptz,
    created_at timestamptz NOT NULL
);

CREATE INDEX email_verification_user_active_idx
    ON email_verification_challenges (user_id, expires_at DESC)
    WHERE consumed_at IS NULL;

CREATE TABLE consent_records (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose varchar(48) NOT NULL,
    policy_version varchar(32) NOT NULL,
    granted boolean NOT NULL,
    recorded_at timestamptz NOT NULL
);

CREATE TABLE user_sessions (
    id uuid PRIMARY KEY,
    family_id uuid NOT NULL,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name varchar(80) NOT NULL,
    access_token_hash char(64) NOT NULL UNIQUE,
    refresh_token_hash char(64) NOT NULL UNIQUE,
    access_expires_at timestamptz NOT NULL,
    refresh_expires_at timestamptz NOT NULL,
    authenticated_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL
);

CREATE INDEX user_sessions_user_active_idx
    ON user_sessions (user_id, refresh_expires_at DESC)
    WHERE revoked_at IS NULL;

CREATE TABLE consumed_refresh_tokens (
    token_hash char(64) PRIMARY KEY,
    session_id uuid NOT NULL REFERENCES user_sessions(id) ON DELETE CASCADE,
    family_id uuid NOT NULL,
    consumed_at timestamptz NOT NULL
);

CREATE TABLE households (
    id uuid PRIMARY KEY,
    name varchar(100) NOT NULL CHECK (char_length(trim(name)) > 0),
    country_code char(2) NOT NULL CHECK (country_code ~ '^[A-Z]{2}$'),
    timezone varchar(64) NOT NULL,
    default_currency char(3) NOT NULL CHECK (default_currency ~ '^[A-Z]{3}$'),
    first_day_of_week smallint NOT NULL CHECK (first_day_of_week IN (1, 6, 7)),
    cycle_type varchar(24) NOT NULL CHECK (cycle_type IN ('weekly', 'fourteen_day', 'calendar_month')),
    cycle_anchor date NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('active', 'closed')),
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE TABLE household_memberships (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role varchar(24) NOT NULL CHECK (role IN ('owner', 'admin', 'member', 'read_only')),
    status varchar(16) NOT NULL CHECK (status IN ('active', 'suspended', 'left', 'removed')),
    joined_at timestamptz NOT NULL,
    left_at timestamptz
);

CREATE UNIQUE INDEX household_memberships_active_unique
    ON household_memberships (household_id, user_id)
    WHERE status = 'active';

CREATE INDEX household_memberships_user_idx
    ON household_memberships (user_id, status, joined_at DESC);

CREATE TABLE idempotency_records (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation varchar(80) NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    request_hash char(64) NOT NULL,
    response_status integer NOT NULL,
    response_body jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, operation, idempotency_key)
);

CREATE TABLE audit_events (
    id uuid PRIMARY KEY,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    household_id uuid REFERENCES households(id) ON DELETE SET NULL,
    action varchar(100) NOT NULL,
    target_type varchar(60) NOT NULL,
    target_id uuid,
    outcome varchar(16) NOT NULL CHECK (outcome IN ('success', 'denied', 'failed')),
    correlation_id uuid,
    safe_details jsonb NOT NULL DEFAULT '{}'::jsonb,
    occurred_at timestamptz NOT NULL
);

CREATE INDEX audit_events_household_time_idx
    ON audit_events (household_id, occurred_at DESC);

CREATE TABLE outbox_events (
    id uuid PRIMARY KEY,
    event_type varchar(100) NOT NULL,
    aggregate_type varchar(60) NOT NULL,
    aggregate_id uuid NOT NULL,
    household_id uuid,
    actor_user_id uuid,
    payload jsonb NOT NULL,
    occurred_at timestamptz NOT NULL,
    published_at timestamptz
);

CREATE INDEX outbox_events_unpublished_idx
    ON outbox_events (occurred_at)
    WHERE published_at IS NULL;
