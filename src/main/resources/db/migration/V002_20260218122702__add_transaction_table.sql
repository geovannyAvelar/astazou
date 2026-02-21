CREATE TABLE bank_account (
    id BIGSERIAL PRIMARY KEY,
    name text not null,
    balance NUMERIC(14,2) NOT NULL,
    username text NOT NULL REFERENCES users(username)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_date DATE NOT NULL,
    description TEXT NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    type VARCHAR(10) NOT NULL,
    page INTEGER,
    sequence INTEGER NOT NULL,
    bank_account_id BIGINT NOT NULL REFERENCES bank_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);