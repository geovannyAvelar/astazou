-- Remove the preferred_currency column from users; currency is now defined per account
ALTER TABLE users DROP COLUMN IF EXISTS preferred_currency;

