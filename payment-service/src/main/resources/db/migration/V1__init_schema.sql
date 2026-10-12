CREATE TABLE payments (
    id              UUID          PRIMARY KEY,
    order_id        UUID          NOT NULL,
    user_id         UUID          NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    transaction_ref VARCHAR(64),
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL,
    -- exactly one payment attempt per order: the last line of defence against double charging
    CONSTRAINT uq_payments_order UNIQUE (order_id)
);

CREATE INDEX idx_payments_user ON payments (user_id, created_at DESC);
