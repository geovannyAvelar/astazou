CREATE TABLE brapi_stock (
    id         BIGSERIAL    PRIMARY KEY,
    ticker     TEXT         NOT NULL UNIQUE,
    name       TEXT,
    sector     TEXT,
    synced_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_brapi_stock_ticker ON brapi_stock(ticker);

