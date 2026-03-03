CREATE TABLE stock_quote (
    id          BIGSERIAL PRIMARY KEY,
    symbol      TEXT           NOT NULL UNIQUE,
    short_name  TEXT,
    long_name   TEXT,
    currency    TEXT,
    price       NUMERIC(14,4)  NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE stock_quote_history (
    id          BIGSERIAL PRIMARY KEY,
    symbol      TEXT           NOT NULL,
    short_name  TEXT,
    long_name   TEXT,
    currency    TEXT,
    price       NUMERIC(14,4)  NOT NULL,
    recorded_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_quote_history_symbol     ON stock_quote_history(symbol);
CREATE INDEX idx_stock_quote_history_recorded_at ON stock_quote_history(recorded_at);

