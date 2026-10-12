CREATE TABLE orders (
    id               UUID          PRIMARY KEY,
    version          BIGINT        NOT NULL DEFAULT 0,     -- optimistic locking
    user_id          UUID          NOT NULL,
    user_email       VARCHAR(255)  NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    total_amount     NUMERIC(12,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    ship_full_name   VARCHAR(150)  NOT NULL,
    ship_line1       VARCHAR(200)  NOT NULL,
    ship_line2       VARCHAR(200),
    ship_city        VARCHAR(100)  NOT NULL,
    ship_postal_code VARCHAR(20)   NOT NULL,
    ship_country     VARCHAR(2)    NOT NULL,
    failure_reason   VARCHAR(500),
    idempotency_key  VARCHAR(64),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
-- A retried checkout with the same Idempotency-Key returns the existing order instead of creating another one.
CREATE UNIQUE INDEX uq_orders_user_idempotency ON orders (user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE order_items (
    id           UUID          PRIMARY KEY,
    order_id     UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   UUID          NOT NULL,
    product_name VARCHAR(200)  NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    quantity     INT           NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- Drives the "order tracking" timeline in the UI.
CREATE TABLE order_status_history (
    id         UUID         PRIMARY KEY,
    order_id   UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status     VARCHAR(20)  NOT NULL,
    note       VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_order_history_order ON order_status_history (order_id, created_at);
