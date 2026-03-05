-- Add tags column as a PostgreSQL text array to transactions table
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS tags TEXT[] NOT NULL DEFAULT '{}';

-- Create a GIN index to allow efficient filtering by tags
CREATE INDEX IF NOT EXISTS idx_transactions_tags ON transactions USING GIN(tags);

