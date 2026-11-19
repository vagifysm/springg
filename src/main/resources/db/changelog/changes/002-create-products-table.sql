--liquibase formatted sql

--changeset starter:002-create-products-table
CREATE TABLE products (
    id              UUID                     NOT NULL,
    name            VARCHAR(150)             NOT NULL,
    description     VARCHAR(2000),
    sku             VARCHAR(64)              NOT NULL,
    price           NUMERIC(19, 2)           NOT NULL,
    stock_quantity  INTEGER                  NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_products_stock_non_negative CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_products_name ON products (name);
--rollback DROP TABLE products;
