CREATE TABLE household_chat_messages (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    sender_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    body varchar(2000) NOT NULL CHECK (char_length(trim(body)) BETWEEN 1 AND 2000),
    created_at timestamptz NOT NULL
);

CREATE INDEX household_chat_messages_household_cursor_idx
    ON household_chat_messages (household_id, id);

CREATE INDEX household_chat_messages_sender_time_idx
    ON household_chat_messages (sender_membership_id, created_at DESC);
