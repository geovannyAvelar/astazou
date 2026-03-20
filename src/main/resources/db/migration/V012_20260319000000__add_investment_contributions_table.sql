CREATE TABLE investment_contribution (
    id             BIGSERIAL PRIMARY KEY,
    symbol         TEXT           NOT NULL,
    purchase_date  DATE           NOT NULL,
    quantity       NUMERIC(18,8)  NOT NULL,
    purchase_price NUMERIC(14,4)  NOT NULL,
    username       TEXT           NOT NULL REFERENCES users(username),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_investment_contribution_username ON investment_contribution(username);
CREATE INDEX idx_investment_contribution_symbol   ON investment_contribution(symbol);

