CREATE TABLE household_tasks (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    created_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assignee_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    title varchar(120) NOT NULL CHECK (char_length(trim(title)) > 0),
    instructions varchar(2000),
    zone varchar(80),
    priority varchar(12) NOT NULL CHECK (priority IN ('low', 'normal', 'high')),
    due_date date NOT NULL,
    due_time time,
    estimated_minutes integer CHECK (estimated_minutes BETWEEN 5 AND 1440),
    status varchar(16) NOT NULL CHECK (status IN ('open', 'in_progress', 'completed', 'cancelled')),
    completion_note varchar(1000),
    completed_by_user_id uuid REFERENCES users(id) ON DELETE RESTRICT,
    completed_at timestamptz,
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CHECK (
        (status = 'completed' AND completed_by_user_id IS NOT NULL AND completed_at IS NOT NULL)
        OR (status <> 'completed' AND completed_by_user_id IS NULL AND completed_at IS NULL)
    )
);

CREATE INDEX household_tasks_household_due_idx
    ON household_tasks (household_id, due_date, due_time, id)
    WHERE status <> 'cancelled';

CREATE INDEX household_tasks_assignee_idx
    ON household_tasks (assignee_membership_id, status, due_date);

CREATE TABLE household_task_requests (
    id uuid PRIMARY KEY,
    task_id uuid NOT NULL REFERENCES household_tasks(id) ON DELETE RESTRICT,
    created_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    request_type varchar(16) NOT NULL CHECK (request_type IN ('help', 'swap', 'postpone', 'issue')),
    status varchar(16) NOT NULL CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled')),
    reason varchar(1000) NOT NULL CHECK (char_length(trim(reason)) >= 3),
    requested_assignee_membership_id uuid REFERENCES household_memberships(id) ON DELETE RESTRICT,
    requested_due_date date,
    requested_due_time time,
    resolved_by_user_id uuid REFERENCES users(id) ON DELETE RESTRICT,
    resolution_note varchar(1000),
    resolved_at timestamptz,
    created_at timestamptz NOT NULL,
    CHECK (
        (request_type = 'swap' AND requested_assignee_membership_id IS NOT NULL)
        OR (request_type <> 'swap' AND requested_assignee_membership_id IS NULL)
    ),
    CHECK (
        (request_type = 'postpone' AND requested_due_date IS NOT NULL)
        OR (request_type <> 'postpone' AND requested_due_date IS NULL AND requested_due_time IS NULL)
    ),
    CHECK (
        (status = 'pending' AND resolved_by_user_id IS NULL AND resolved_at IS NULL)
        OR (status <> 'pending' AND resolved_by_user_id IS NOT NULL AND resolved_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX household_task_requests_one_pending_type_idx
    ON household_task_requests (task_id, request_type)
    WHERE status = 'pending';

CREATE INDEX household_task_requests_task_idx
    ON household_task_requests (task_id, created_at DESC);

CREATE TABLE household_task_history (
    id uuid PRIMARY KEY,
    task_id uuid NOT NULL REFERENCES household_tasks(id) ON DELETE RESTRICT,
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    event_type varchar(40) NOT NULL,
    from_status varchar(16),
    to_status varchar(16),
    details jsonb NOT NULL DEFAULT '{}'::jsonb,
    occurred_at timestamptz NOT NULL
);

CREATE INDEX household_task_history_task_idx
    ON household_task_history (task_id, occurred_at, id);
