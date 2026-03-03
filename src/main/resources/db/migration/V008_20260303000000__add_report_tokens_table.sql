CREATE TABLE report_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       TEXT        NOT NULL UNIQUE,
    username    TEXT        NOT NULL REFERENCES users(username),
    bank_account_id BIGINT  NOT NULL REFERENCES bank_account(id),
    account_name TEXT       NOT NULL,
    report_month INTEGER    NOT NULL,
    report_year  INTEGER    NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_report_tokens_token ON report_tokens(token);

