CREATE TABLE carts (
    user_id    UUID        PRIMARY KEY,
    version    BIGINT      NOT NULL DEFAULT 0,      -- optimistic locking
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id           UUID          PRIMARY KEY,
    cart_user_id UUID          NOT NULL REFERENCES carts (user_id) ON DELETE CASCADE,
    product_id   UUID          NOT NULL,
    product_name VARCHAR(200)  NOT NULL,
    image_url    VARCHAR(500),
    unit_price   NUMERIC(12,2) NOT NULL,
    quantity     INT           NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    added_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_item UNIQUE (cart_user_id, product_id)
);
