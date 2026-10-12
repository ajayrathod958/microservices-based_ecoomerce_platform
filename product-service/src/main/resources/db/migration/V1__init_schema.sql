CREATE TABLE categories (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(5000),
    sku         VARCHAR(64)   NOT NULL,
    price       NUMERIC(12,2) NOT NULL CHECK (price > 0),
    image_url   VARCHAR(500),
    category_id UUID          NOT NULL REFERENCES categories (id),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_sku UNIQUE (sku)
);

CREATE INDEX idx_products_category       ON products (category_id);
CREATE INDEX idx_products_active_created ON products (active, created_at DESC);
