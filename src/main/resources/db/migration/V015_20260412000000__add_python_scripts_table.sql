CREATE TABLE python_scripts (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(255) NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    code        TEXT         NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_python_scripts_username ON python_scripts(username);

