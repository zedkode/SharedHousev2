CREATE TABLE calendar_events (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    created_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    event_type varchar(24) NOT NULL
        CHECK (event_type IN ('household', 'maintenance', 'appointment', 'shopping', 'other')),
    title varchar(120) NOT NULL CHECK (char_length(trim(title)) > 0),
    description varchar(1000),
    event_date date NOT NULL,
    start_time time,
    end_time time,
    reminder_minutes_before integer
        CHECK (reminder_minutes_before BETWEEN 0 AND 10080),
    status varchar(16) NOT NULL CHECK (status IN ('active', 'deleted')),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    deleted_at timestamptz,
    CHECK (end_time IS NULL OR start_time IS NOT NULL),
    CHECK (end_time IS NULL OR end_time > start_time),
    CHECK (
        (status = 'active' AND deleted_at IS NULL)
        OR (status = 'deleted' AND deleted_at IS NOT NULL)
    )
);

CREATE INDEX calendar_events_household_date_idx
    ON calendar_events (household_id, event_date, start_time, id)
    WHERE status = 'active';

CREATE INDEX calendar_events_creator_idx
    ON calendar_events (created_by_user_id, created_at DESC);
