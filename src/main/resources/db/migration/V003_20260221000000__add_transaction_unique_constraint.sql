-- Add unique constraint on transaction_date, bank_account_id, sequence
-- This ensures no duplicate transactions for the same date, account, and sequence
ALTER TABLE transactions
ADD CONSTRAINT uk_transaction_date_account_sequence
UNIQUE (transaction_date, bank_account_id, sequence);

