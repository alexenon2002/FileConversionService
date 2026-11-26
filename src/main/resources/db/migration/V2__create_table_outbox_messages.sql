CREATE TABLE outbox_messages
(
    event_id              UUID PRIMARY KEY,
    request_event_id      UUID         NOT NULL UNIQUE,
    saga_id               UUID         NOT NULL,
    payload               JSONB        NOT NULL,
    status                VARCHAR(100) NOT NULL
        CHECK (status IN ('PENDING', 'PUBLISHING', 'PUBLISHED'))
         DEFAULT 'PENDING',
    lease_until           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    publishing_started_at TIMESTAMPTZ,
    published_at          TIMESTAMPTZ,
    FOREIGN KEY (request_event_id) REFERENCES inbox_messages(event_id)
);