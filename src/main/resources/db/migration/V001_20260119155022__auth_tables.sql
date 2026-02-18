CREATE TABLE IF NOT EXISTS users
(
    username text primary key,
    "name" text not null,
    email text unique not null,
    password text not null,
    roles text[] not null default ARRAY[]::text[]
);

CREATE TABLE IF NOT EXISTS sessions (
    token text primary key,
    username text not null references users(username),
    expires_at timestamptz
);