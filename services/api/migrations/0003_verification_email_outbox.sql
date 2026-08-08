CREATE TABLE verification_email_outbox (
    id uuid PRIMARY KEY,
    challenge_id uuid NOT NULL UNIQUE
        REFERENCES email_verification_challenges(id) ON DELETE CASCADE,
    recipient_email varchar(254) NOT NULL,
    locale varchar(5) NOT NULL CHECK (locale IN ('en', 'ro')),
    code_ciphertext_base64 text,
    code_iv_base64 varchar(32),
    code_auth_tag_base64 varchar(32),
    expires_at timestamptz NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'sending', 'sent', 'dead')),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 8),
    available_at timestamptz NOT NULL,
    locked_at timestamptz,
    sent_at timestamptz,
    provider_message_id varchar(128),
    last_error_code varchar(80),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT verification_email_outbox_payload_state_check CHECK (
        status IN ('sent', 'dead')
        OR (
            code_ciphertext_base64 IS NOT NULL
            AND code_iv_base64 IS NOT NULL
            AND code_auth_tag_base64 IS NOT NULL
        )
    )
);

CREATE INDEX verification_email_outbox_pending_idx
    ON verification_email_outbox (available_at, created_at)
    WHERE status IN ('pending', 'sending');
