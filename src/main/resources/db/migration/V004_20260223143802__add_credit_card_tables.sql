CREATE TABLE IF NOT EXISTS credit_cards (
    id bigserial primary key,
    "name" text not null,
    username text references users(username)
);

CREATE TABLE IF NOT EXISTS credit_card_transactions (
    id text primary key,
    amount NUMERIC(14,2) NOT NULL,
    description text not null,
    credit_card bigint references credit_cards(id),
    statement_date DATE NOT NULL,
    transaction_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);