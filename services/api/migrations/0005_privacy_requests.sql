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
