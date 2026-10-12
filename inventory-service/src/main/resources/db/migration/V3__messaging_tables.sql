CREATE TABLE outbox_events (
    id           UUID         PRIMARY KEY,
    seq          BIGINT       GENERATED ALWAYS AS IDENTITY,
    topic        VARCHAR(100) NOT NULL,
    message_key  VARCHAR(100) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (seq) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    id           VARCHAR(120) PRIMARY KEY,
    processed_at TIMESTAMPTZ  NOT NULL
);
