CREATE TABLE inbox_messages
(
    event_id     UUID PRIMARY KEY,
    payload      JSONB        NOT NULL,
    status       VARCHAR(100) NOT NULL
        CHECK (status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    lease_until  TIMESTAMPTZ,
    started_at   TIMESTAMPTZ,
    received_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ
);