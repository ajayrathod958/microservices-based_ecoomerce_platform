CREATE TABLE inventory_items (
    product_id        UUID        PRIMARY KEY,
    quantity_on_hand  INT         NOT NULL CHECK (quantity_on_hand >= 0),
    quantity_reserved INT         NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reserved_le_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

-- One row per (order, product): the UNIQUE constraint is the last line of defence for idempotent consumers.
CREATE TABLE stock_reservations (
    id         UUID        PRIMARY KEY,
    order_id   UUID        NOT NULL,
    product_id UUID        NOT NULL REFERENCES inventory_items (product_id),
    quantity   INT         NOT NULL CHECK (quantity > 0),
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reservation_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_reservations_order ON stock_reservations (order_id);
