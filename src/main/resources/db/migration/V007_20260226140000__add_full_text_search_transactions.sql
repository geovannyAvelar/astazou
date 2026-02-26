-- Add tsvector column for full-text search on transaction descriptions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS description_search tsvector;

-- Update existing rows with tsvector
UPDATE transactions SET description_search = to_tsvector('english', COALESCE(description, ''));

-- Create index for faster full-text search
CREATE INDEX IF NOT EXISTS idx_transactions_description_search ON transactions USING GIN(description_search);

-- Create trigger to automatically update tsvector on insert/update
CREATE OR REPLACE FUNCTION transactions_description_search_trigger() RETURNS trigger AS $$
BEGIN
  NEW.description_search := to_tsvector('english', COALESCE(NEW.description, ''));
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS transactions_description_search_trigger ON transactions;

CREATE TRIGGER transactions_description_search_trigger
BEFORE INSERT OR UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION transactions_description_search_trigger();

